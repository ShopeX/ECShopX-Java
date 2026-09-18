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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.companys.config.CommonApiTokenProperties;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorBasicUserService {

	private final CommonApiTokenProperties commonApiTokenProperties;
	private final OperatorsQueryService operatorsQueryService;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final OperatorDistributorSelectionService operatorDistributorSelectionService;
	private final ObjectMapper objectMapper;

	public OperatorBasicUserService(
			CommonApiTokenProperties commonApiTokenProperties,
			OperatorsQueryService operatorsQueryService,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			WechatAuthQueryService wechatAuthQueryService,
			OperatorDistributorSelectionService operatorDistributorSelectionService,
			ObjectMapper objectMapper) {
		this.commonApiTokenProperties = commonApiTokenProperties;
		this.operatorsQueryService = operatorsQueryService;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.operatorDistributorSelectionService = operatorDistributorSelectionService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getBasicUserById(String idRaw, String tokenParam) {
		String configured = commonApiTokenProperties.getApiToken();
		if (!StringUtils.hasText(configured)
				|| !StringUtils.hasText(tokenParam)
				|| !Objects.equals(tokenParam, configured)) {
			throw new ForbiddenException("无权访问该API,签名错误");
		}

		if (idRaw == null || idRaw.isBlank()) {
			throw new ResourceException("帐号不存在");
		}

		final long operatorId;
		try {
			operatorId = Long.parseLong(idRaw.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("帐号不存在");
		}

		Map<String, Object> op = operatorsQueryService.getInfo(Map.of("operator_id", operatorId));
		if (op == null || op.isEmpty()) {
			throw new ResourceException("帐号不存在");
		}

		Object companyObj = op.get("company_id");
		if (companyObj == null) {
			throw new ResourceException("帐号不存在");
		}
		long companyId = companyObj instanceof Number n ? n.longValue() : Long.parseLong(companyObj.toString().trim());

		List<Long> distributorIds = parseFlexibleLongList(op.get("distributor_ids"), "分销商列表格式错误");

		String opType = String.valueOf(op.getOrDefault("operator_type", ""));
		long regionauthVal = parseRegionauthId(op.get("regionauth_id"));
		if ("staff".equals(opType) && regionauthVal > 0L) {
			distributorIds =
					distributionDistributorSelfReadMapper.listDistributorIdsByCompanyAndRegionauthId(
							companyId, regionauthVal);
			if (distributorIds == null) {
				distributorIds = new ArrayList<>();
			}
		}

		List<Long> shopIds = parseFlexibleLongList(op.get("shop_ids"), "店铺列表格式错误");

		String authorizerAppid = wechatAuthQueryService.getAuthorizerAppid(companyId);

		long distributorId = 0L;
		if ("distributor".equals(opType)) {
			distributorId = operatorDistributorSelectionService.readSelectedDistributorId(operatorId, companyId).orElse(0L);
		}

		String mobile = op.get("mobile") == null ? "" : op.get("mobile").toString();
		String usernameRaw = op.get("username") == null ? null : op.get("username").toString();
		String username = StringUtils.hasText(usernameRaw) ? usernameRaw : "超级管理员";
		Object headRaw = op.get("head_portrait");
		String headPortrait = headRaw == null ? "" : headRaw.toString();

		Object isDisableRaw = op.get("is_disable");
		int isDisable = 0;
		if (isDisableRaw instanceof Number n) {
			isDisable = n.intValue();
		} else if (isDisableRaw != null && StringUtils.hasText(isDisableRaw.toString())) {
			try {
				isDisable = Integer.parseInt(isDisableRaw.toString().trim());
			} catch (NumberFormatException ignored) {
				isDisable = 0;
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", operatorId);
		result.put("operator_id", operatorId);
		result.put("distributor_ids", distributorIds);
		result.put("distributor_id", distributorId);
		result.put("shop_ids", shopIds);
		result.put("mobile", mobile);
		result.put("company_id", companyId);
		result.put("authorizer_appid", authorizerAppid);
		result.put("operator_type", op.get("operator_type"));
		result.put("username", username);
		result.put("head_portrait", headPortrait);
		result.put("regionauth_id", op.get("regionauth_id"));
		result.put("updated", toEpochSecondsOrNull(op.get("updated")));
		result.put("merchant_id", op.get("merchant_id"));
		result.put("is_merchant_main", op.get("is_merchant_main"));
		result.put("is_distributor_main", op.get("is_distributor_main"));
		result.put("is_disable", isDisable);
		return result;
	}

	private static long parseRegionauthId(Object rObj) {
		if (rObj == null) {
			return 0L;
		}
		if (rObj instanceof Number n) {
			return n.longValue();
		}
		String s = rObj.toString();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private List<Long> parseFlexibleLongList(Object raw, String badMessage) {
		if (raw == null) {
			return new ArrayList<>();
		}
		if (raw instanceof Collection<?> c) {
			return parseLongIdsFromCollection(c, badMessage);
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return new ArrayList<>();
			}
			try {
				List<Long> parsed = objectMapper.readValue(s, new TypeReference<List<Long>>() {});
				return parsed != null ? parsed : new ArrayList<>();
			} catch (JsonProcessingException e) {
				throw new BadRequestException(badMessage);
			}
		}
		return new ArrayList<>();
	}

	private static List<Long> parseLongIdsFromCollection(Collection<?> c, String badMessage) {
		List<Long> out = new ArrayList<>();
		for (Object e : c) {
			if (e == null) {
				continue;
			}
			try {
				if (e instanceof Number n) {
					out.add(n.longValue());
				} else {
					out.add(Long.parseLong(String.valueOf(e).trim()));
				}
			} catch (NumberFormatException ex) {
				throw new BadRequestException(badMessage);
			}
		}
		return out;
	}

	/** Epoch seconds from the stored {@code updated} value; {@code null} when that value is absent. */
	private static Long toEpochSecondsOrNull(Object updated) {
		if (updated == null) {
			return null;
		}
		if (updated instanceof Number n) {
			long v = n.longValue();
			if (v > 1_000_000_000_000L) {
				return v / 1000L;
			}
			return v;
		}
		if (updated instanceof LocalDateTime ldt) {
			return ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
		}
		if (updated instanceof java.util.Date d) {
			return d.getTime() / 1000L;
		}
		if (updated instanceof java.sql.Timestamp ts) {
			return ts.toInstant().getEpochSecond();
		}
		return 0L;
	}
}
