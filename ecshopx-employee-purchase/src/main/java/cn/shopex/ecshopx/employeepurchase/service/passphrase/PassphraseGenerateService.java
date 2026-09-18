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

package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterprises;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PassphraseGenerateService {

	private static final String CODE_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int CODE_LENGTH = 8;
	private static final int MAX_COLLISION_RETRIES = 64;

	private final ActivitiesMapper activitiesMapper;
	private final ActivityEnterprisesMapper activityEnterprisesMapper;
	private final EnterprisesMapper enterprisesMapper;
	private final ActivityPassphraseService activityPassphraseService;
	private final SecureRandom secureRandom = new SecureRandom();

	public PassphraseGenerateService(
			ActivitiesMapper activitiesMapper,
			ActivityEnterprisesMapper activityEnterprisesMapper,
			EnterprisesMapper enterprisesMapper,
			ActivityPassphraseService activityPassphraseService) {
		this.activitiesMapper = activitiesMapper;
		this.activityEnterprisesMapper = activityEnterprisesMapper;
		this.enterprisesMapper = enterprisesMapper;
		this.activityPassphraseService = activityPassphraseService;
	}

	public Map<String, Object> generate(
			long companyId,
			long distributorId,
			List<Long> enterpriseIds,
			int count,
			Long activityId) {
		if (enterpriseIds == null || enterpriseIds.isEmpty()) {
			throw new ResourceException("请传入企业ID");
		}
		if (enterpriseIds.size() > 100) {
			throw new ResourceException("单次最多选择 100 个企业");
		}
		if (count < 1 || count > 50) {
			throw new ResourceException("每企业生成条数须在 1～50 之间");
		}
		long total = (long) enterpriseIds.size() * count;
		if (total > 500) {
			throw new ResourceException("单次生成口令总数不能超过 500，请减少企业数量或每企业条数");
		}

		Set<Long> distinctEnterpriseIds = new LinkedHashSet<>(enterpriseIds);
		Set<Long> activityEnterpriseIds = null;
		if (activityId != null && activityId > 0L) {
			Activities activity = loadActivityOrThrow(companyId, activityId);
			activityEnterpriseIds = loadActivityEnterpriseIds(companyId, activity.getId());
		}

		Set<String> usedCodes;
		if (activityId != null && activityId > 0L) {
			usedCodes = activityPassphraseService.collectUsedCodesForActivity(companyId, activityId);
		} else {
			usedCodes = activityPassphraseService.collectUsedCodesForCompany(companyId, 0L);
		}
		Set<String> batchCodes = new HashSet<>();

		List<Map<String, Object>> list = new ArrayList<>();
		for (Long enterpriseId : distinctEnterpriseIds) {
			validateEnterprise(companyId, distributorId, enterpriseId, activityEnterpriseIds);
			List<String> codes = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				String code = generateUniqueCode(usedCodes, batchCodes);
				codes.add(code);
				batchCodes.add(code);
			}
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("enterprise_id", enterpriseId);
			row.put("passphrase_codes", codes);
			list.add(row);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("list", list);
		return result;
	}

	private void validateEnterprise(
			long companyId, long distributorId, Long enterpriseId, Set<Long> activityEnterpriseIds) {
		if (enterpriseId == null || enterpriseId <= 0L) {
			throw new ResourceException("公司参数无效");
		}
		if (activityEnterpriseIds != null && !activityEnterpriseIds.contains(enterpriseId)) {
			throw new ResourceException("企业须为本活动参与企业");
		}
		Enterprises enterprise =
				enterprisesMapper.selectOne(
						Wrappers.<Enterprises>lambdaQuery()
								.eq(Enterprises::getCompanyId, companyId)
								.eq(Enterprises::getId, enterpriseId)
								.last("LIMIT 1"));
		if (enterprise == null) {
			throw new ResourceException("公司参数无效");
		}
		if (distributorId > 0L) {
			Integer entDist = enterprise.getDistributorId();
			if (entDist != null && entDist.longValue() != distributorId) {
				throw new ResourceException("公司参数无效");
			}
		}
	}

	private Activities loadActivityOrThrow(long companyId, long activityId) {
		Activities activity =
				activitiesMapper.selectOne(
						Wrappers.<Activities>lambdaQuery()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, activityId)
								.last("LIMIT 1"));
		if (activity == null) {
			throw new ResourceException("活动不存在");
		}
		return activity;
	}

	private Set<Long> loadActivityEnterpriseIds(long companyId, long activityId) {
		List<ActivityEnterprises> rows =
				activityEnterprisesMapper.selectList(
						Wrappers.<ActivityEnterprises>lambdaQuery()
								.eq(ActivityEnterprises::getCompanyId, companyId)
								.eq(ActivityEnterprises::getActivityId, activityId));
		Set<Long> ids = new LinkedHashSet<>();
		for (ActivityEnterprises row : rows) {
			if (row.getEnterpriseId() != null) {
				ids.add(row.getEnterpriseId());
			}
		}
		return ids;
	}

	private String generateUniqueCode(Set<String> usedCodes, Set<String> batchCodes) {
		for (int i = 0; i < MAX_COLLISION_RETRIES; i++) {
			String code = randomCode();
			if (!usedCodes.contains(code) && !batchCodes.contains(code)) {
				return code;
			}
		}
		throw new ResourceException("生成唯一口令失败，请重试");
	}

	private String randomCode() {
		StringBuilder sb = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			sb.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
		}
		return sb.toString();
	}

	public static List<Long> parseEnterpriseIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				long id = longVal(o, 0L);
				if (id > 0L) {
					out.add(id);
				}
			}
			return out;
		}
		return List.of();
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return def;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
