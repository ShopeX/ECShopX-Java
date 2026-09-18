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

package cn.shopex.ecshopx.companys.service.activation;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.domain.Resources;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.mapper.ResourcesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanyLicenseCreateService {

	/**
	 * 降级文案：网关 HTTP/非 JSON/结构异常且响应体无 {@code message_zh} 时与 SaaS 非 0 分支一致（用户可见）。
	 */
	private static final String GATEWAY_FAIL_FALLBACK_ZH = "许可证校验失败";

	private final CompanysMapper companysMapper;
	private final ResourcesMapper resourcesMapper;
	private final LicenseGatewayClient licenseGatewayClient;
	private final ShopexAuthCodeClient shopexAuthCodeClient;
	private final CompanysLicensePayloadEncryptor licensePayloadEncryptor;
	private final IndependentLicenseCertReadService independentLicenseCertReadService;
	private final LicenseActivationProperties licenseActivationProperties;
	private final CompanyActivatePersistenceService companyActivatePersistenceService;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${common.system-is-saas:false}")
	private boolean systemIsSaas;

	public CompanyLicenseCreateService(
			CompanysMapper companysMapper,
			ResourcesMapper resourcesMapper,
			LicenseGatewayClient licenseGatewayClient,
			ShopexAuthCodeClient shopexAuthCodeClient,
			CompanysLicensePayloadEncryptor licensePayloadEncryptor,
			IndependentLicenseCertReadService independentLicenseCertReadService,
			LicenseActivationProperties licenseActivationProperties,
			CompanyActivatePersistenceService companyActivatePersistenceService,
			ApplicationEventPublisher applicationEventPublisher,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysMapper = companysMapper;
		this.resourcesMapper = resourcesMapper;
		this.licenseGatewayClient = licenseGatewayClient;
		this.shopexAuthCodeClient = shopexAuthCodeClient;
		this.licensePayloadEncryptor = licensePayloadEncryptor;
		this.independentLicenseCertReadService = independentLicenseCertReadService;
		this.licenseActivationProperties = licenseActivationProperties;
		this.companyActivatePersistenceService = companyActivatePersistenceService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> createCompanyLicense(
			Map<String, Object> whitelistParams, Map<String, Object> jwtUser) {
		if (jwtUser == null || jwtUser.isEmpty()) {
			throw new ResourceException("授权信息有误");
		}
		long companyId = requirePositiveCompanyId(jwtUser);
		String plainActiveCode = requireNonBlankActiveCode(whitelistParams);

		Companys company = companysMapper.selectById(companyId);
		if (company == null) {
			throw new ResourceException("无相关企业信息");
		}
		String passportUid = company.getPassportUid();
		if (!StringUtils.hasText(passportUid)) {
			throw new ResourceException("登录信息有误");
		}

		ActivationDims dims = companysActivation(passportUid, plainActiveCode, companyId);
		long activeAt = System.currentTimeMillis() / 1000L;
		// 门店数、天数仅在许可证结果未给出对应字段时用 1、5；failure_time 存在时仍只重算天数
		int shopNum = dims.store != null ? dims.store : 1;
		int availableDays = dims.days != null ? dims.days : 5;
		if (dims.failureTimeSeconds != null) {
			availableDays = (int) Math.floor((dims.failureTimeSeconds - activeAt) / 86400.0);
		}
		if (shopNum == 0) {
			throw new ResourceException("激活门店数有误");
		}
		if (availableDays == 0) {
			throw new ResourceException("激活天数有误！");
		}

		LinkedHashMap<String, Object> fullParams = new LinkedHashMap<>();
		fullParams.put("source", "purchased");
		fullParams.put("resource_name", shopNum + "店版");
		fullParams.put("left_shop_num", shopNum);
		fullParams.put("shop_num", shopNum);
		fullParams.put("active_status", "active");
		fullParams.put("active_at", activeAt);
		fullParams.put("code", plainActiveCode);
		fullParams.put("available_days", availableDays);
		fullParams.put("expired_at", activeAt + availableDays * 86400L);
		fullParams.put("active_code", licensePayloadEncryptor.encryptActiveCodePlain(plainActiveCode));
		fullParams.put("company_id", companyId);
		fullParams.put("eid", company.getEid() != null ? company.getEid() : "");
		fullParams.put("passport_uid", passportUid);

		Map<String, Object> result = companyActivatePersistenceService.activateAfterLicense(fullParams);

		writePostCommitSideEffects(companyId, fullParams, result);
		applicationEventPublisher.publishEvent(new CompanyActivatedEvent(this, companyId));

		return result;
	}

	private void writePostCommitSideEffects(long companyId, LinkedHashMap<String, Object> fullParams, Map<String, Object> result) {
		Resources latest = selectLatestValidResource(companyId);
		if (latest != null) {
			LinkedHashMap<String, Object> res = new LinkedHashMap<>();
			res.put("company_id", companyId);
			res.put("expired_at", latest.getExpiredAt());
			res.put("resouce_id", latest.getResourceId());
			res.put("source", latest.getSource() != null ? latest.getSource() : "");
			try {
				String json = objectMapper.writeValueAsString(res);
				companysRedisTemplate.opsForValue().set(companyActivateRedisKey(companyId), json);
			} catch (JsonProcessingException e) {
				throw new IllegalStateException(e);
			}
		}
		Object rid = result.get("resource_id");
		if (rid != null && !rid.toString().isEmpty() && !"0".equals(rid.toString())) {
			saveLicenseRedis(fullParams, companyId);
		}
	}

	private void saveLicenseRedis(LinkedHashMap<String, Object> params, long companyId) {
		String redisKey = "AuthorizeActivation:" + sha1Hex(String.valueOf(companyId));
		String field = str(params.get("active_code"));
		String value = licensePayloadEncryptor.encryptParamsMap(params);
		companysRedisTemplate.opsForHash().put(redisKey, field, value);
	}

	private Resources selectLatestValidResource(long companyId) {
		long nowSec = System.currentTimeMillis() / 1000L;
		LambdaQueryWrapper<Resources> q = new LambdaQueryWrapper<>();
		q.eq(Resources::getCompanyId, companyId)
				.gt(Resources::getExpiredAt, nowSec)
				.orderByDesc(Resources::getExpiredAt)
				.last("LIMIT 1");
		return resourcesMapper.selectOne(q);
	}

	private static String companyActivateRedisKey(long companyId) {
		return "companyActivateInfo:" + sha1Hex(String.valueOf(companyId));
	}

	private ActivationDims companysActivation(String shopexId, String activeCode, long companyId) {
		if (systemIsSaas) {
			return checkActiveCodeSaaS(shopexId, activeCode);
		}
		return checkActiveCodeIndependent(shopexId, activeCode, companyId);
	}

	private ActivationDims checkActiveCodeSaaS(String shopexId, String activeCode) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("shopex_id", shopexId);
		payload.put("active_code", activeCode);
		String encoded = shopexAuthCodeClient.encode(payload);
		String product = licenseActivationProperties.getProductName();
		String url = licenseActivationProperties.getLicenseUrl();
		JsonNode root =
				licenseGatewayClient.postFormUrlEncoded(
						url, Map.of("product", product != null ? product : "", "code", encoded));
		if (root == null) {
			throw new ResourceException(GATEWAY_FAIL_FALLBACK_ZH);
		}
		JsonNode codeNode = root.get("code");
		if (isZeroCode(codeNode) && root.hasNonNull("data")) {
			JsonNode data = root.get("data");
			Integer days = jsonOptionalInt(data, "days");
			Integer store = jsonOptionalInt(data, "store");
			if (days == null || store == null) {
				throw new ResourceException(GATEWAY_FAIL_FALLBACK_ZH);
			}
			return new ActivationDims(days, store, null);
		}
		if (codeNode != null && !codeNode.isNull() && "E111".equals(codeNode.asText())) {
			String msg = messageZhFromGateway(root);
			throw new ResourceException(msg.isEmpty() ? GATEWAY_FAIL_FALLBACK_ZH : msg);
		}
		if (!isZeroCode(codeNode)) {
			String msg = messageZhFromGateway(root);
			throw new ResourceException(msg.isEmpty() ? GATEWAY_FAIL_FALLBACK_ZH : msg);
		}
		throw new ResourceException(GATEWAY_FAIL_FALLBACK_ZH);
	}

	private ActivationDims checkActiveCodeIndependent(String shopexId, String activeCode, long companyId) {
		Map<String, String> cert = independentLicenseCertReadService.getCertSetting(shopexId, companyId);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("shopex_id", shopexId);
		data.put("active_code", activeCode);
		String encoded = shopexAuthCodeClient.encode(data);

		String shopUrl = licenseActivationProperties.getIndependentShopUrl();
		LinkedHashMap<String, String> form = new LinkedHashMap<>();
		form.put("node_id", cert.get("node_id"));
		form.put("certificate", cert.get("cert_id"));
		form.put("shop_url", shopUrl != null ? shopUrl : "http://localhost");
		form.put("version", licenseActivationProperties.getVersion() != null ? licenseActivationProperties.getVersion() : "");
		form.put(
				"product_name",
				licenseActivationProperties.getIndependentProductType() != null
						? licenseActivationProperties.getIndependentProductType()
						: "");
		form.put(
				"product",
				licenseActivationProperties.getProductName() != null
						? licenseActivationProperties.getProductName()
						: "");
		form.put("code", encoded);

		String url = licenseActivationProperties.getIndependentLicenseUrl();
		JsonNode root = licenseGatewayClient.postFormUrlEncoded(url, form);
		if (root == null) {
			throw new ResourceException(GATEWAY_FAIL_FALLBACK_ZH);
		}
		JsonNode codeNode = root.get("code");
		if (isZeroCode(codeNode) && root.hasNonNull("data")) {
			String dataCipher = root.get("data").asText("");
			Map<String, Object> inner = shopexAuthCodeClient.decode(dataCipher);
			if (inner == null || inner.isEmpty()) {
				String msg = messageZhFromGateway(root);
				throw new ResourceException(msg.isEmpty() ? GATEWAY_FAIL_FALLBACK_ZH : msg);
			}
			Integer days = parseOptionalIntFromDecoded(inner, "days");
			Integer store = parseOptionalIntFromDecoded(inner, "store");
			Long failureSec = null;
			if (inner.containsKey("failure_time") && inner.get("failure_time") != null) {
				failureSec = parseFailureTimeToEpochSeconds(inner.get("failure_time"));
			}
			boolean shapeOk = (days != null && store != null) || failureSec != null;
			if (!shapeOk) {
				String msg = messageZhFromGateway(root);
				throw new ResourceException(msg.isEmpty() ? GATEWAY_FAIL_FALLBACK_ZH : msg);
			}
			return new ActivationDims(days, store, failureSec);
		}
		String msg = messageZhFromGateway(root);
		throw new ResourceException(msg.isEmpty() ? GATEWAY_FAIL_FALLBACK_ZH : msg);
	}

	private static Integer parseOptionalIntFromDecoded(Map<String, Object> inner, String key) {
		if (inner == null || !inner.containsKey(key)) {
			return null;
		}
		Object v = inner.get(key);
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long parseFailureTimeToEpochSeconds(Object ft) {
		if (ft == null) {
			return null;
		}
		if (ft instanceof Number n) {
			return n.longValue();
		}
		String s = ft.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		if (s.matches("\\d{10,}")) {
			return Long.parseLong(s);
		}
		try {
			return Instant.parse(s).getEpochSecond();
		} catch (DateTimeParseException ignored) {
			// continue
		}
		for (DateTimeFormatter f :
				new DateTimeFormatter[] {
					DateTimeFormatter.ISO_LOCAL_DATE_TIME,
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
				}) {
			try {
				LocalDateTime ldt = LocalDateTime.parse(s, f);
				return ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
			} catch (DateTimeParseException ignored) {
				// next
			}
		}
		throw new ResourceException("许可证返回时间格式无效");
	}

	private static String messageZhFromGateway(JsonNode root) {
		if (root == null || !root.has("message_zh") || root.get("message_zh").isNull()) {
			return "";
		}
		return root.get("message_zh").asText("");
	}

	private static boolean isZeroCode(JsonNode codeNode) {
		if (codeNode == null || codeNode.isNull()) {
			return false;
		}
		if (codeNode.isNumber()) {
			return codeNode.intValue() == 0;
		}
		return "0".equals(codeNode.asText().trim());
	}

	private static Integer jsonOptionalInt(JsonNode parent, String field) {
		if (parent == null || !parent.has(field) || parent.get(field).isNull()) {
			return null;
		}
		JsonNode n = parent.get(field);
		if (n.isNumber()) {
			return n.intValue();
		}
		if (n.isTextual()) {
			String s = n.asText().trim();
			if (s.isEmpty()) {
				return null;
			}
			try {
				return Integer.parseInt(s);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static long requirePositiveCompanyId(Map<String, Object> jwtUser) {
		Object v = jwtUser.get("company_id");
		if (v == null) {
			throw new BadRequestException("企业信息有误");
		}
		long id;
		if (v instanceof Number n) {
			id = n.longValue();
		} else {
			try {
				id = Long.parseLong(v.toString().trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("企业信息有误");
			}
		}
		if (id <= 0L) {
			throw new BadRequestException("企业信息有误");
		}
		return id;
	}

	private static String requireNonBlankActiveCode(Map<String, Object> whitelistParams) {
		Object v = whitelistParams != null ? whitelistParams.get("active_code") : null;
		if (v == null) {
			throw new BadRequestException("激活信息有误");
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			throw new BadRequestException("激活信息有误");
		}
		return s;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static final class ActivationDims {
		private final Integer days;
		private final Integer store;
		private final Long failureTimeSeconds;

		private ActivationDims(Integer days, Integer store, Long failureTimeSeconds) {
			this.days = days;
			this.store = store;
			this.failureTimeSeconds = failureTimeSeconds;
		}
	}
}
