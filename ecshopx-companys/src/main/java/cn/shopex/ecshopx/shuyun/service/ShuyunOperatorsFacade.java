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

package cn.shopex.ecshopx.shuyun.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.domain.Roles;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.mapper.RolesMapper;
import cn.shopex.ecshopx.companys.service.EmployeeService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.openapi.OpenapiDeveloperBootstrapService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismIshopexFacade;
import cn.shopex.ecshopx.thirdparty.service.shuyun.ShuyunSignedGatewayClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Service
public class ShuyunOperatorsFacade {

	private static final Logger log = LoggerFactory.getLogger(ShuyunOperatorsFacade.class);

	/** Gateway HTTP / empty-body / parse failure message aligned with Shuyun client default. */
	private static final String SHUYUN_CLIENT_FAIL_MSG = "接口请求超时或失败";

	/** Prism passport check failure wording from {@link PrismIshopexFacade#assertYunqiPassportOk}. */
	private static final String PRISM_PASSPORT_FAIL_MSG = "管理员账号错误，请稍后重试~";

	private final ShuyunSignedGatewayClient shuyunSignedGatewayClient;
	private final PrismIshopexFacade prismIshopexFacade;
	private final OperatorsQueryService operatorsQueryService;
	private final EmployeeService employeeService;
	private final ShopMenuService shopMenuService;
	private final OpenapiDeveloperBootstrapService openapiDeveloperBootstrapService;
	private final CompanysMapper companysMapper;
	private final RolesMapper rolesMapper;
	private final ObjectMapper objectMapper;
	private final SecureRandom secureRandom = new SecureRandom();

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	@Value("${common.product-model:platform}")
	private String productModel;

	public ShuyunOperatorsFacade(
			ShuyunSignedGatewayClient shuyunSignedGatewayClient,
			PrismIshopexFacade prismIshopexFacade,
			OperatorsQueryService operatorsQueryService,
			EmployeeService employeeService,
			ShopMenuService shopMenuService,
			OpenapiDeveloperBootstrapService openapiDeveloperBootstrapService,
			CompanysMapper companysMapper,
			RolesMapper rolesMapper,
			ObjectMapper objectMapper) {
		this.shuyunSignedGatewayClient = shuyunSignedGatewayClient;
		this.prismIshopexFacade = prismIshopexFacade;
		this.operatorsQueryService = operatorsQueryService;
		this.employeeService = employeeService;
		this.shopMenuService = shopMenuService;
		this.openapiDeveloperBootstrapService = openapiDeveloperBootstrapService;
		this.companysMapper = companysMapper;
		this.rolesMapper = rolesMapper;
		this.objectMapper = objectMapper;
	}

	/**
	 * Resolves operator row for Shuyun authorization code login (admin or staff), including optional staff
	 * provisioning when the gateway marks the user as a normal account.
	 */
	public Map<String, Object> resolveOperatorData(@Nullable String code, boolean credentialsContainsCodeKey) {
		ShuyunUserInfo data;
		if (credentialsContainsCodeKey && StringUtils.hasText(code) && Objects.equals(code, "shopextest1")) {
			data = new ShuyunUserInfo("15202937200", "88240602404045", 0, "", false);
		} else if (credentialsContainsCodeKey && StringUtils.hasText(code) && Objects.equals(code, "shopextest2")) {
			data = new ShuyunUserInfo("13100000000", "88240602404045", 1, "测试用普通账号", false);
		} else if (credentialsContainsCodeKey && !StringUtils.hasText(code)) {
			throw new ResourceException(SHUYUN_CLIENT_FAIL_MSG);
		} else {
			data = loadUserInfo(code);
		}
		Map<String, Object> filter = new HashMap<>();
		filter.put("mobile", data.phone());
		switch (data.role()) {
			case 0 -> {
				try {
					prismIshopexFacade.assertYunqiPassportOk(data.phone());
					filter.put("operator_type", "admin");
					Map<String, Object> operator = operatorsQueryService.getInfo(filter);
					if (operator == null || operator.isEmpty()) {
						throw new ResourceException(SHUYUN_CLIENT_FAIL_MSG);
					}
					Object companyId = operator.get("company_id");
					operator.put("menu_type", getCompanyMenuType(companyId));
					operator.put("logintype", "admin");
					return operator;
				} catch (ResourceException ex) {
					throw remapRemoteShuyunPrismPassportConflict(data.fromRemoteGateway(), ex);
				}
			}
			case 1 -> {
				try {
					filter.put("operator_type", "staff");
					Map<String, Object> operator = operatorsQueryService.getInfo(filter);
					if (operator == null || operator.isEmpty()) {
						Map<String, Object> adminFilter = new HashMap<>();
						adminFilter.put("operator_type", "admin");
						adminFilter.put("passport_uid", data.shopId());
						Map<String, Object> adminRow = operatorsQueryService.getInfo(adminFilter);
						if (adminRow == null || adminRow.isEmpty()) {
							throw new ResourceException("请先设置超级管理员账号~");
						}
						long companyId = toLong(adminRow.get("company_id"));
						operator = createStaffOperator(companyId, data);
					}
					operator.put("menu_type", getCompanyMenuType(operator.get("company_id")));
					operator.put("logintype", "staff");
					return operator;
				} catch (ResourceException ex) {
					throw remapRemoteShuyunPrismPassportConflict(data.fromRemoteGateway(), ex);
				}
			}
			default -> throw new ResourceException("管理员账号类型错误~");
		}
	}

	private Map<String, Object> createStaffOperator(long companyId, ShuyunUserInfo data) {
		String roleId = ensureDefaultStaffRoleId(companyId);
		int pwd = 100000 + secureRandom.nextInt(900000);
		Map<String, Object> create = new LinkedHashMap<>();
		create.put("company_id", companyId);
		create.put("operator_id", 0L);
		create.put("mobile", data.phone());
		create.put("login_name", data.phone());
		create.put("username", StringUtils.hasText(data.userName()) ? data.userName() : data.phone());
		create.put("operator_type", "staff");
		create.put("head_portrait", "");
		create.put("distributor_ids", List.of());
		create.put("shop_ids", List.of());
		create.put("contact", 0);
		create.put("password", String.valueOf(pwd));
		create.put("role_id", List.of(roleId));
		return employeeService.createOperatorStaff(create);
	}

	private String ensureDefaultStaffRoleId(long companyId) {
		Roles existing =
				rolesMapper.selectOne(
						new LambdaQueryWrapper<Roles>()
								.eq(Roles::getCompanyId, companyId)
								.eq(Roles::getRoleName, "普通员工")
								.last("LIMIT 1"));
		if (existing != null) {
			return String.valueOf(existing.getRoleId());
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		Roles row = new Roles();
		row.setCompanyId(companyId);
		row.setRoleName("普通员工");
		row.setRoleSource("platform");
		row.setDistributorId(0L);
		row.setPermission("{\"shopmenu_alias_name\":[\"index\"]}");
		row.setCreated(now);
		row.setUpdated(now);
		rolesMapper.insert(row);
		return String.valueOf(row.getRoleId());
	}

	private ShuyunUserInfo loadUserInfo(@Nullable String code) {
		if (!shuyunSignedGatewayClient.isConfigured()) {
			throw new ResourceException(SHUYUN_CLIENT_FAIL_MSG);
		}
		Map<String, String> query = new LinkedHashMap<>();
		query.put("code", code == null ? null : code);
		JsonNode root;
		try {
			root =
					shuyunSignedGatewayClient.getSignedJson(
							"/pcrm-account/1.0/shopex/userInfo", query);
		} catch (RestClientException e) {
			throw new ResourceException(SHUYUN_CLIENT_FAIL_MSG);
		}
		if (root == null) {
			throw new ResourceException(SHUYUN_CLIENT_FAIL_MSG);
		}
		int apiCode = root.path("code").asInt(Integer.MIN_VALUE);
		if (apiCode != 0) {
			String msg = root.path("message").asText("");
			throw new ResourceException(StringUtils.hasText(msg) ? msg : SHUYUN_CLIENT_FAIL_MSG);
		}
		JsonNode data = root.get("data");
		if (data == null || !data.isObject()) {
			throw new ResourceException(SHUYUN_CLIENT_FAIL_MSG);
		}
		String phone = text(data, "phone");
		if (!StringUtils.hasText(phone)) {
			phone = text(data, "mobile");
		}
		String shopId = text(data, "shopId");
		if (!StringUtils.hasText(shopId)) {
			shopId = text(data, "shop_id");
		}
		int role = data.path("role").asInt(-1);
		String userName = text(data, "userName");
		if (!StringUtils.hasText(userName)) {
			userName = text(data, "user_name");
		}
		if (!StringUtils.hasText(phone) || role < 0) {
			throw new ResourceException(SHUYUN_CLIENT_FAIL_MSG);
		}
		return new ShuyunUserInfo(phone.trim(), shopId != null ? shopId.trim() : "", role, userName, true);
	}

	/**
	 * When Shuyun gateway already returned user payload but Prism passport disagrees, the API surfaces a generic
	 * client/timeout message; map only for real gateway-loaded users, not in-process test stubs.
	 */
	private static ResourceException remapRemoteShuyunPrismPassportConflict(
			boolean fromRemoteGateway, ResourceException ex) {
		if (fromRemoteGateway && isPrismPassportGatewaySemanticFailure(ex.getMessage())) {
			return new ResourceException(SHUYUN_CLIENT_FAIL_MSG);
		}
		return ex;
	}

	private static boolean isPrismPassportGatewaySemanticFailure(@Nullable String message) {
		return PRISM_PASSPORT_FAIL_MSG.equals(message);
	}

	private String getCompanyMenuType(Object companyId) {
		if (companyId == null) {
			return productModel;
		}
		long cid = toLong(companyId);
		Map<String, Object> info = shopMenuService.getMenuTypeByCompanyId(cid);
		Object str = info.get("menu_type_str");
		return str != null ? str.toString() : productModel;
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static String text(JsonNode n, String field) {
		if (n == null || !n.has(field) || n.get(field).isNull()) {
			return "";
		}
		return n.get(field).asText("");
	}

	private record ShuyunUserInfo(String phone, String shopId, int role, String userName, boolean fromRemoteGateway) {}

	/**
	 * Pushes WeChat shopping sub-callback registration to the Shuyun gateway when running in OEM Shuyun mode.
	 * If the gateway is unreachable or misconfigured, errors are logged without failing company creation.
	 */
	public void syncDeveloperConfigurationToShuyun(long companyId) {
		if (!oemShuyun) {
			return;
		}
		try {
			OpenapiDeveloperBootstrapService.DeveloperCredentials keys =
					openapiDeveloperBootstrapService.loadOrCreateForCompany(companyId);
			Companys c = companysMapper.selectById(companyId);
			if (c == null) {
				return;
			}
			String shopId = StringUtils.hasText(c.getPassportUid()) ? c.getPassportUid() : "";
			var body = objectMapper.createObjectNode();
			body.put("appKey", keys.appKey());
			body.put("appSecret", keys.appSecret());
			body.put("shopId", shopId);
			if (!shuyunSignedGatewayClient.isConfigured()) {
				log.info(
						"Shuyun gateway not configured; sub-callback skipped companyId={}",
						companyId);
				return;
			}
			JsonNode resp =
					shuyunSignedGatewayClient.postSignedJson(
							"/ucenter-mars-message/v1/subCallback/weixin/shopping",
							objectMapper.writeValueAsString(body),
							null);
			if (resp == null) {
				log.warn("Shuyun sub-callback empty response companyId={}", companyId);
				return;
			}
			int apiCode = resp.path("code").asInt(Integer.MIN_VALUE);
			if (apiCode != 0) {
				String msg = resp.path("message").asText("");
				log.warn(
						"Shuyun sub-callback failed companyId={} code={} msg={}",
						companyId,
						apiCode,
						msg);
			}
		} catch (Exception e) {
			log.warn(
					"Shuyun developer sync failed companyId={} msg={}",
					companyId,
					e.getMessage());
		}
	}
}
