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

package cn.shopex.ecshopx.merchant.service.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.port.MerchantWxappLoginCompanyIdResolverPort;
import cn.shopex.ecshopx.merchant.port.MerchantWxappLoginSmsVcodePort;
import cn.shopex.ecshopx.merchant.service.MerchantBaseSettingSaveService;
import cn.shopex.ecshopx.merchant.service.MerchantCreateParamValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MerchantWxappLoginService {

	private final MerchantWxappLoginCompanyIdResolverPort companyResolver;
	private final MerchantWxappLoginSmsVcodePort smsVcodePort;
	private final MerchantBaseSettingSaveService merchantBaseSettingSaveService;
	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final MerchantWxappJwtIssuer merchantWxappJwtIssuer;

	public MerchantWxappLoginService(
			MerchantWxappLoginCompanyIdResolverPort companyResolver,
			MerchantWxappLoginSmsVcodePort smsVcodePort,
			MerchantBaseSettingSaveService merchantBaseSettingSaveService,
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			MerchantWxappJwtIssuer merchantWxappJwtIssuer) {
		this.companyResolver = companyResolver;
		this.smsVcodePort = smsVcodePort;
		this.merchantBaseSettingSaveService = merchantBaseSettingSaveService;
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.merchantWxappJwtIssuer = merchantWxappJwtIssuer;
	}

	/**
	 * Whether {@code raw} is treated as an absent {@code company_id} credential.
	 * <ul>
	 * <li>{@code null} — empty</li>
	 * <li>{@link Boolean} — empty if {@code false}</li>
	 * <li>{@link Number} — empty if {@linkplain Number#longValue() long value} is {@code 0}</li>
	 * <li>{@link String} — empty if {@linkplain String#isEmpty() empty} or equal to {@code "0"}</li>
	 * <li>any other type — not empty</li>
	 * </ul>
	 */
	public static boolean isEmptyCompanyId(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (raw instanceof String s) {
			return s.isEmpty() || "0".equals(s);
		}
		return false;
	}

	public Map<String, Object> loginWxapp(Map<String, Object> bodyOrNull, HttpServletRequest request, String origin) {
		Map<String, Object> credentials = new LinkedHashMap<>();
		putCredential(credentials, "mobile", bodyOrNull, request);
		putCredential(credentials, "vcode", bodyOrNull, request);
		putCredential(credentials, "company_id", bodyOrNull, request);
		if (StringUtils.hasText(origin)) {
			credentials.put("origin", origin.trim());
		}

		companyResolver.resolveCompanyId(credentials);
		checkParams(credentials);
		long companyId = parsePositiveCompanyId(credentials.get("company_id"));
		String mobile = scalarStringForParams(credentials.get("mobile")).trim();
		String vcode = scalarStringForParams(credentials.get("vcode")).trim();

		if (!smsVcodePort.verifyAndConsumeMerchantLogin(mobile, companyId, vcode)) {
			throw new ResourceException("短信验证码错误");
		}

		MerchantSettlementApply existing = merchantSettlementApplyMapper.selectOne(
				new LambdaQueryWrapper<MerchantSettlementApply>()
						.eq(MerchantSettlementApply::getCompanyId, companyId)
						.eq(MerchantSettlementApply::getMobile, mobile)
						.last("LIMIT 1"));

		long accountId;
		if (existing != null) {
			accountId = existing.getId();
		} else {
			Map<String, Object> base = merchantBaseSettingSaveService.loadCompanyBaseSetting(companyId);
			Object st = base.get("status");
			if (Boolean.FALSE.equals(st)) {
				throw new ResourceException("平台不支持商户入驻");
			}
			MerchantSettlementApply row = new MerchantSettlementApply();
			row.setCompanyId(companyId);
			row.setMobile(mobile);
			row.setAgreeAgreement(true);
			row.setMerchantTypeId(0L);
			row.setAuditStatus("1");
			row.setSource("h5");
			row.setDisabled(true);
			row.setCreated((int) (System.currentTimeMillis() / 1000L));
			row.setUpdated(null);
			int inserted = merchantSettlementApplyMapper.insert(row);
			if (inserted < 1 || row.getId() == null) {
				throw new ResourceException("提交失败，请重试");
			}
			accountId = row.getId();
		}

		String jwt;
		try {
			jwt = merchantWxappJwtIssuer.issueToken(accountId, companyId, mobile);
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}

		return Map.of("data", Map.of("token", jwt));
	}

	private static void putCredential(
			Map<String, Object> credentials, String key, Map<String, Object> bodyOrNull, HttpServletRequest request) {
		Object v = credentialRaw(key, bodyOrNull, request);
		if (v != null) {
			credentials.put(key, v);
		}
	}

	private static Object credentialRaw(String key, Map<String, Object> bodyOrNull, HttpServletRequest request) {
		if (bodyOrNull != null && bodyOrNull.containsKey(key)) {
			return bodyOrNull.get(key);
		}
		return request.getParameter(key);
	}

	private static void checkParams(Map<String, Object> credentials) {
		if (isEmptyCompanyId(credentials.get("company_id"))) {
			throw new ResourceException("企业 ID 不能为空");
		}
		String mobile = scalarStringForParams(credentials.get("mobile")).trim();
		if (!StringUtils.hasText(mobile)) {
			throw new ResourceException("手机号不能为空");
		}
		if (!MerchantCreateParamValidator.isCnMobile(mobile)) {
			throw new ResourceException("请输入正确的手机号");
		}
		credentials.put("mobile", mobile);
		String vcode = scalarStringForParams(credentials.get("vcode")).trim();
		if (!StringUtils.hasText(vcode)) {
			throw new ResourceException("短信验证码不能为空");
		}
		credentials.put("vcode", vcode);
	}

	private static long parsePositiveCompanyId(Object raw) {
		if (isEmptyCompanyId(raw)) {
			throw new ResourceException("企业 ID 不能为空");
		}
		long id;
		try {
			if (raw instanceof Number n) {
				id = n.longValue();
			} else {
				id = Long.parseLong(scalarStringForParams(raw).trim());
			}
		} catch (Exception e) {
			throw new ResourceException("企业 ID 不能为空");
		}
		if (id <= 0L) {
			throw new ResourceException("企业 ID 不能为空");
		}
		return id;
	}

	private static String scalarStringForParams(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof String s) {
			return s;
		}
		if (v instanceof Number || v instanceof Boolean) {
			return String.valueOf(v);
		}
		if (v instanceof List<?> list) {
			if (list.isEmpty()) {
				return "";
			}
			return scalarStringForParams(list.get(0));
		}
		return String.valueOf(v);
	}
}
