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

package cn.shopex.ecshopx.companys.service.wxapp;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.companys.domain.DistributorWechatRel;
import cn.shopex.ecshopx.companys.mapper.DistributorWechatRelMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.operator.OperatorAdminAppDetailEnrichPort;
import cn.shopex.ecshopx.companys.service.setting.DianwuSettingRedisService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DistributorWxappBindCheckoutService {

	private static final ObjectMapper DISTRIBUTOR_IDS_JSON = new ObjectMapper();

	private final DianwuSettingRedisService dianwuSettingRedisService;

	private final ShopMenuService shopMenuService;

	private final OperatorsQueryService operatorsQueryService;

	private final OperatorAdminAppDetailEnrichPort operatorAdminAppDetailEnrichPort;

	private final DistributorWechatRelMapper distributorWechatRelMapper;

	private final DistributorWxappBindCheckoutService self;

	public DistributorWxappBindCheckoutService(
			DianwuSettingRedisService dianwuSettingRedisService,
			ShopMenuService shopMenuService,
			OperatorsQueryService operatorsQueryService,
			OperatorAdminAppDetailEnrichPort operatorAdminAppDetailEnrichPort,
			DistributorWechatRelMapper distributorWechatRelMapper,
			@Lazy DistributorWxappBindCheckoutService self) {
		this.dianwuSettingRedisService = dianwuSettingRedisService;
		this.shopMenuService = shopMenuService;
		this.operatorsQueryService = operatorsQueryService;
		this.operatorAdminAppDetailEnrichPort = operatorAdminAppDetailEnrichPort;
		this.distributorWechatRelMapper = distributorWechatRelMapper;
		this.self = self;
	}

	public Map<String, Object> checkDistributor(HttpServletRequest request) {
		long companyId = readPositiveH5CompanyId(request);
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims =
				rawClaims instanceof Map<?, ?> m ? (Map<String, Object>) m : Collections.emptyMap();

		Object authUserId = claims.get("user_id");
		Object mobileObj = claims.get("mobile");
		String mobile = mobileObj == null ? "" : String.valueOf(mobileObj).trim();

		String wxappAppidRaw = stringClaim(claims, "wxapp_appid");
		String appId = StringUtils.hasText(wxappAppidRaw) ? wxappAppidRaw.trim() : "no_wxapp_app_id";

		String openidRaw = stringClaim(claims, "open_id");
		if (!StringUtils.hasText(openidRaw)) {
			openidRaw = stringClaim(claims, "openid");
		}
		String openid = openidRaw == null ? "" : openidRaw.trim();
		String unionid = stringClaim(claims, "unionid");

		Map<String, Object> returnData = new LinkedHashMap<>();
		returnData.put("status", Boolean.FALSE);
		returnData.put("result", Collections.emptyList());

		Map<String, Object> dianwu = dianwuSettingRedisService.getDianwuSetting(companyId);
		boolean show = Boolean.TRUE.equals(dianwu.get("dianwu_show_status"));
		if (!show) {
			returnData.put("msg", "未开启移动端显示店务端入口");
			return returnData;
		}

		if (!hasUserId(authUserId)) {
			throw new UnauthorizedException("请重新登录");
		}
		if (!StringUtils.hasText(mobile)) {
			return returnData;
		}

		String operatorTypeFilter = "distributor";
		String productModel = shopMenuService.resolveProductModelKeyForCompany(companyId);
		if (!"platform".equalsIgnoreCase(productModel)) {
			operatorTypeFilter = "staff";
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("mobile", mobile.trim());
		filter.put("operator_type", operatorTypeFilter);

		Map<String, Object> operatorBrief = operatorsQueryService.getInfo(filter);
		if (operatorBrief == null || operatorBrief.isEmpty()) {
			return returnData;
		}

		long operatorId = parsePositiveLong(operatorBrief.get("operator_id"));
		if (operatorId <= 0L) {
			return returnData;
		}

		LinkedHashMap<String, Object> operator = new LinkedHashMap<>(operatorBrief);
		operatorAdminAppDetailEnrichPort.enrichAppDetailForOperator(companyId, operator);
		if (operator.containsKey("password") && legacyTruthy(operator.get("password"))) {
			operator.remove("password");
		}
		returnData.put("operator", operator);

		String opType = String.valueOf(operator.getOrDefault("operator_type", "")).trim();
		Object distIds = operator.get("distributor_ids");
		boolean distributorType = "distributor".equals(opType);
		if (distributorType && distributorIdsEffectivelyEmpty(distIds)) {
			returnData.put("msg", "账号未绑定店铺");
			return returnData;
		}

		LambdaQueryWrapper<DistributorWechatRel> wxQ = new LambdaQueryWrapper<>();
		wxQ.eq(DistributorWechatRel::getCompanyId, companyId)
				.eq(DistributorWechatRel::getOperatorId, operatorId)
				.eq(DistributorWechatRel::getAppType, "wxa")
				.last("LIMIT 1");
		DistributorWechatRel existingBound = distributorWechatRelMapper.selectOne(wxQ);

		if (existingBound != null) {
			returnData.put("result", wechatRelToRowMap(existingBound));
		} else {
			Map<String, Object> rowMap = self.bindDistributorUserForCheckout(companyId, operatorId, appId, openid, unionid);
			if (rowMap != null && !rowMap.isEmpty()) {
				returnData.put("result", rowMap);
			}
		}

		Object resultObj = returnData.get("result");
		boolean hasResult =
				resultObj != null
						&& (!(resultObj instanceof Collection<?> c) || !c.isEmpty())
						&& (!(resultObj instanceof Map<?, ?> m) || !m.isEmpty());
		returnData.put("status", hasResult);
		return returnData;
	}

	public Map<String, Object> checkDeliveryStaff(HttpServletRequest request) {
		long companyId = readPositiveH5CompanyId(request);
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims =
				rawClaims instanceof Map<?, ?> m ? (Map<String, Object>) m : Collections.emptyMap();

		Object authUserId = claims.get("user_id");
		Object mobileObj = claims.get("mobile");
		String mobile = mobileObj == null ? "" : String.valueOf(mobileObj).trim();

		String wxappAppidRaw = stringClaim(claims, "wxapp_appid");
		String appId = StringUtils.hasText(wxappAppidRaw) ? wxappAppidRaw.trim() : "no_wxapp_app_id";

		String openidRaw = stringClaim(claims, "open_id");
		if (!StringUtils.hasText(openidRaw)) {
			openidRaw = stringClaim(claims, "openid");
		}
		String openid = openidRaw == null ? "" : openidRaw.trim();
		String unionid = stringClaim(claims, "unionid");

		Map<String, Object> returnData = new LinkedHashMap<>();
		returnData.put("status", Boolean.FALSE);
		returnData.put("result", Collections.emptyList());

		if (!hasUserId(authUserId)) {
			throw new UnauthorizedException("请重新登录");
		}
		if (!StringUtils.hasText(mobile)) {
			return returnData;
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("mobile", mobile.trim());
		filter.put("operator_type", "self_delivery_staff");

		Map<String, Object> operatorBrief = operatorsQueryService.getInfo(filter);
		if (operatorBrief == null || operatorBrief.isEmpty()) {
			return returnData;
		}

		long operatorId = parsePositiveLong(operatorBrief.get("operator_id"));
		if (operatorId <= 0L) {
			return returnData;
		}

		LinkedHashMap<String, Object> operator = new LinkedHashMap<>(operatorBrief);
		operatorAdminAppDetailEnrichPort.enrichAppDetailForOperator(companyId, operator);
		if (operator.containsKey("password") && legacyTruthy(operator.get("password"))) {
			operator.remove("password");
		}
		returnData.put("operator", operator);

		String opType = String.valueOf(operator.getOrDefault("operator_type", "")).trim();
		Object raw = operator.get("distributor_ids");
		boolean staffType = "self_delivery_staff".equals(opType);
		if (staffType && distributorIdsEffectivelyEmpty(raw)) {
			returnData.put("msg", "账号未绑定店铺");
			return returnData;
		}

		LambdaQueryWrapper<DistributorWechatRel> wxQ = new LambdaQueryWrapper<>();
		wxQ.eq(DistributorWechatRel::getCompanyId, companyId)
				.eq(DistributorWechatRel::getOperatorId, operatorId)
				.eq(DistributorWechatRel::getAppType, "wxa")
				.last("LIMIT 1");
		DistributorWechatRel existingBound = distributorWechatRelMapper.selectOne(wxQ);

		if (existingBound != null) {
			returnData.put("result", wechatRelToRowMap(existingBound));
		} else {
			Map<String, Object> rowMap = self.bindDistributorUserForCheckout(companyId, operatorId, appId, openid, unionid);
			if (rowMap != null && !rowMap.isEmpty()) {
				returnData.put("result", rowMap);
			}
		}

		Object resultObj = returnData.get("result");
		boolean hasResult =
				resultObj != null
						&& (!(resultObj instanceof Collection<?> c) || !c.isEmpty())
						&& (!(resultObj instanceof Map<?, ?> m) || !m.isEmpty());
		returnData.put("status", hasResult);
		return returnData;
	}

	private static Map<String, Object> wechatRelToRowMap(DistributorWechatRel rel) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", rel.getId());
		m.put("company_id", rel.getCompanyId());
		m.put("app_id", rel.getAppId());
		m.put("app_type", rel.getAppType());
		m.put("openid", rel.getOpenid());
		m.put("unionid", rel.getUnionid());
		m.put("operator_id", rel.getOperatorId());
		m.put("bound_time", rel.getBoundTime());
		return m;
	}

	@Transactional(rollbackFor = Exception.class)
	Map<String, Object> bindDistributorUserForCheckout(
			long companyId, long operatorId, String appId, String openid, String unionid) {
		LambdaQueryWrapper<DistributorWechatRel> dimQ = new LambdaQueryWrapper<>();
		dimQ.eq(DistributorWechatRel::getCompanyId, companyId)
				.eq(DistributorWechatRel::getAppId, appId)
				.eq(DistributorWechatRel::getAppType, "wxa")
				.eq(DistributorWechatRel::getOpenid, openid == null ? "" : openid)
				.eq(DistributorWechatRel::getUnionid, unionid == null ? "" : unionid)
				.last("LIMIT 1");
		DistributorWechatRel byWx = distributorWechatRelMapper.selectOne(dimQ);
		if (byWx != null) {
			distributorWechatRelMapper.deleteById(byWx.getId());
		}
		long boundTime = Instant.now().getEpochSecond();
		DistributorWechatRel ins = new DistributorWechatRel();
		ins.setCompanyId(companyId);
		ins.setAppId(appId);
		ins.setAppType("wxa");
		ins.setOpenid(openid == null ? "" : openid);
		ins.setUnionid(unionid == null ? "" : unionid);
		ins.setOperatorId(operatorId);
		ins.setBoundTime(boundTime);
		distributorWechatRelMapper.insert(ins);
		Long id = ins.getId();
		if (id == null || id <= 0L) {
			return Collections.emptyMap();
		}
		DistributorWechatRel loaded = distributorWechatRelMapper.selectById(id);
		if (loaded == null) {
			return Collections.emptyMap();
		}
		return wechatRelToRowMap(loaded);
	}

	private static boolean distributorIdsEffectivelyEmpty(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return true;
			}
			try {
				JsonNode node = DISTRIBUTOR_IDS_JSON.readTree(s.trim());
				if (!node.isArray()) {
					return true;
				}
				return node.size() == 0;
			} catch (JacksonException e) {
				return true;
			}
		}
		return false;
	}

	private static long readPositiveH5CompanyId(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("请重新登录");
			}
		} else {
			throw new UnauthorizedException("请重新登录");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("请重新登录");
		}
		return companyId;
	}

	private static String stringClaim(Map<String, Object> claims, String key) {
		if (claims == null || !claims.containsKey(key)) {
			return "";
		}
		Object v = claims.get(key);
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}

	private static boolean hasUserId(Object v) {
		return parseUserIdAsLong(v) > 0L;
	}

	private static long parseUserIdAsLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		String t = String.valueOf(v).trim();
		if (!StringUtils.hasText(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parsePositiveLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean legacyTruthy(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b.booleanValue();
		}
		if (o instanceof Number n) {
			return n.intValue() != 0;
		}
		if (o instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"0".equals(t);
		}
		if (o instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (o instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		String t = String.valueOf(o).trim();
		return !t.isEmpty() && !"0".equals(t);
	}
}
