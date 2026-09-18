/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.employeepurchase.service.export;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterprises;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityPassphraseEnterpriseMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseActivityQrcodeExportRowBuilder {

	private static final String TEMPLATE_NAME = "yykweishop";
	private static final String PAGE = "pages/share-land";
	private static final String FROM_SCENE = "poster_purchase_auth";

	private final ActivitiesMapper activitiesMapper;
	private final ActivityEnterprisesMapper activityEnterprisesMapper;
	private final ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper;
	private final EnterprisesMapper enterprisesMapper;
	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final String appBaseUrl;

	public EmployeePurchaseActivityQrcodeExportRowBuilder(
			ActivitiesMapper activitiesMapper,
			ActivityEnterprisesMapper activityEnterprisesMapper,
			ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper,
			EnterprisesMapper enterprisesMapper,
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			@Value("${APP_URL:http://localhost}") String appBaseUrl) {
		this.activitiesMapper = activitiesMapper;
		this.activityEnterprisesMapper = activityEnterprisesMapper;
		this.activityPassphraseEnterpriseMapper = activityPassphraseEnterpriseMapper;
		this.enterprisesMapper = enterprisesMapper;
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.appBaseUrl = normalizeBaseUrl(appBaseUrl);
	}

	public List<Map<String, String>> buildRows(long companyId, long activityId, Integer distributorId) {
		Activities activity = requireActivity(companyId, activityId, distributorId);
		boolean passphraseEnabled = Boolean.TRUE.equals(activity.getIsPassphraseEnabled());

		String wxaAppId =
				weappAuthorizerAppidRepository
						.findAuthorizerAppid(companyId, TEMPLATE_NAME)
						.orElseThrow(() -> new ResourceException("没有绑定小程序"));

		List<ActivityEnterprises> participations =
				activityEnterprisesMapper.selectList(
						Wrappers.<ActivityEnterprises>lambdaQuery()
								.eq(ActivityEnterprises::getCompanyId, companyId)
								.eq(ActivityEnterprises::getActivityId, activityId)
								.orderByAsc(ActivityEnterprises::getEnterpriseId));
		if (participations.isEmpty()) {
			return List.of();
		}

		Set<Long> enterpriseIds =
				participations.stream()
						.map(ActivityEnterprises::getEnterpriseId)
						.filter(Objects::nonNull)
						.filter(id -> id > 0L)
						.collect(Collectors.toSet());
		Map<Long, Enterprises> enterpriseById = loadEnterprises(companyId, enterpriseIds);
		Map<Long, ActivityPassphraseEnterprise> passphraseByEnterprise =
				passphraseEnabled ? loadPassphraseByEnterprise(companyId, activityId) : Map.of();

		List<Map<String, String>> rows = new ArrayList<>(participations.size());
		for (ActivityEnterprises participation : participations) {
			Long enterpriseId = participation.getEnterpriseId();
			if (enterpriseId == null || enterpriseId <= 0L) {
				continue;
			}
			Enterprises ent = enterpriseById.get(enterpriseId);
			String name = ent == null ? "" : nullToEmpty(ent.getName());
			String sn = ent == null ? "" : nullToEmpty(ent.getEnterpriseSn());

			String pass = "";
			String participateQuota = "";
			String passphraseLimitfeeYuan = "";
			if (passphraseEnabled) {
				ActivityPassphraseEnterprise cfg = passphraseByEnterprise.get(enterpriseId);
				if (cfg != null) {
					pass = nullToEmpty(cfg.getPassphraseCode());
					participateQuota =
							cfg.getParticipateQuota() == null ? "0" : cfg.getParticipateQuota().toString();
					int limitFen = cfg.getPassphraseLimitfee() == null ? 0 : cfg.getPassphraseLimitfee();
					passphraseLimitfeeYuan = formatYuan(limitFen);
				}
			}

			LinkedHashMap<String, String> query = new LinkedHashMap<>();
			query.put("company_id", Long.toString(companyId));
			query.put("cid", Long.toString(companyId));
			query.put("temp_name", TEMPLATE_NAME);
			query.put("page", PAGE);
			query.put("id", Long.toString(activityId));
			query.put("enterprise_id", Long.toString(enterpriseId));
			query.put("appid", wxaAppId);
			query.put("from_scene", FROM_SCENE);
			if (passphraseEnabled) {
				query.put("ppe", "1");
			}

			LinkedHashMap<String, String> row = new LinkedHashMap<>();
			row.put("enterprise_name", name);
			row.put("enterprise_sn", sn);
			row.put("passphrase_code", pass);
			row.put("participate_quota", participateQuota);
			row.put("passphrase_limitfee", passphraseLimitfeeYuan);
			row.put("qrcode_url", appBaseUrl + "/wechatAuth/wxapp/qrcode.png?" + buildQuery(query));
			rows.add(row);
		}
		return rows;
	}

	private Activities requireActivity(long companyId, long activityId, Integer distributorId) {
		var wrapper =
				Wrappers.<Activities>lambdaQuery()
						.eq(Activities::getCompanyId, companyId)
						.eq(Activities::getId, activityId);
		if (distributorId != null) {
			wrapper.eq(Activities::getDistributorId, distributorId);
		}
		Activities activity = activitiesMapper.selectOne(wrapper.last("LIMIT 1"));
		if (activity == null) {
			throw new ResourceException("活动不存在");
		}
		return activity;
	}

	private Map<Long, ActivityPassphraseEnterprise> loadPassphraseByEnterprise(long companyId, long activityId) {
		List<ActivityPassphraseEnterprise> rows =
				activityPassphraseEnterpriseMapper.selectList(
						Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
								.eq(ActivityPassphraseEnterprise::getCompanyId, companyId)
								.eq(ActivityPassphraseEnterprise::getActivityId, activityId));
		LinkedHashMap<Long, ActivityPassphraseEnterprise> out = new LinkedHashMap<>();
		for (ActivityPassphraseEnterprise row : rows) {
			if (row.getEnterpriseId() != null && row.getEnterpriseId() > 0L) {
				out.put(row.getEnterpriseId(), row);
			}
		}
		return out;
	}

	private Map<Long, Enterprises> loadEnterprises(long companyId, Set<Long> enterpriseIds) {
		if (enterpriseIds.isEmpty()) {
			return Map.of();
		}
		List<Enterprises> list =
				enterprisesMapper.selectList(
						Wrappers.<Enterprises>lambdaQuery()
								.eq(Enterprises::getCompanyId, companyId)
								.in(Enterprises::getId, enterpriseIds));
		LinkedHashMap<Long, Enterprises> out = new LinkedHashMap<>();
		for (Enterprises e : list) {
			if (e.getId() != null) {
				out.put(e.getId(), e);
			}
		}
		return out;
	}

	private static String buildQuery(Map<String, String> query) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : query.entrySet()) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(encode(e.getKey())).append('=').append(encode(e.getValue()));
		}
		return sb.toString();
	}

	private static String encode(String value) {
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
	}

	private static String formatYuan(int limitFen) {
		return String.format("%.2f", limitFen / 100.0);
	}

	private static String normalizeBaseUrl(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "http://localhost";
		}
		String trimmed = raw.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed.isEmpty() ? "http://localhost" : trimmed;
	}

	private static String nullToEmpty(String v) {
		return v == null ? "" : v;
	}
}
