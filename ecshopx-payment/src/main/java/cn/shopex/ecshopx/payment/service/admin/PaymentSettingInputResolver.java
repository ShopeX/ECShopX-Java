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

package cn.shopex.ecshopx.payment.service.admin;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.payment.service.dto.PaymentSettingCommand;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Component
public class PaymentSettingInputResolver {

	private static final String[] FILE_KEYS = {
		"cert", "cert_key", "pfx_file", "ca_pfx_file", "oca31_pfx_file", "rsa_private", "rsa_public"
	};

	public PaymentSettingCommand resolve(HttpServletRequest request, Map<String, Object> body) {
		Map<String, Object> fromParams = FlexibleHttpServletParameterMap.toObjectMap(request);
		LinkedHashMap<String, Object> scalar = new LinkedHashMap<>(fromParams);
		if (body != null) {
			for (Map.Entry<String, Object> e : body.entrySet()) {
				if (e.getValue() != null) {
					scalar.put(e.getKey(), e.getValue());
				}
			}
		}

		long companyId = this.requireCompanyId(request);
		long distributorId = parseLongParam(scalar.remove("distributor_id"), 0L);

		Map<String, MultipartFile> files = new LinkedHashMap<>();
		if (request instanceof MultipartHttpServletRequest mreq) {
			for (String key : FILE_KEYS) {
				MultipartFile f = mreq.getFile(key);
				if (f != null && !f.isEmpty()) {
					files.put(key, f);
				}
			}
		}

		String operatorType = "";
		long operatorId = 0L;
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (rawUd instanceof Map<?, ?> ud) {
			Object ot = ud.get("operator_type");
			if (ot != null) {
				operatorType = String.valueOf(ot).trim();
			}
			Object oid = ud.get("operator_id");
			if (oid != null) {
				try {
					operatorId = parseLongLoose(oid);
				} catch (NumberFormatException ignored) {
					operatorId = 0L;
				}
			}
		}

		Object rawPayType = scalar.remove("pay_type");
		String payType = rawPayType != null ? String.valueOf(rawPayType) : null;

		return new PaymentSettingCommand(
				companyId, distributorId, operatorType, operatorId, payType, scalar, Map.copyOf(files));
	}

	public long requireCompanyId(HttpServletRequest request) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return parseLongLoose(cid);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	/**
	 * 与 {@link cn.shopex.ecshopx.payment.service.settings.ChinaumsPaymentSettingWriter#write} 中 subKey 规则一致（dealer 优先，否则 distributor）。
	 */
	public String resolveChinaumsSubKey(HttpServletRequest request, long distributorId) {
		String operatorType = "";
		long operatorId = 0L;
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (rawUd instanceof Map<?, ?> ud) {
			Object ot = ud.get("operator_type");
			if (ot != null) {
				operatorType = String.valueOf(ot).trim();
			}
			Object oid = ud.get("operator_id");
			if (oid != null) {
				try {
					operatorId = parseLongLoose(oid);
				} catch (NumberFormatException ignored) {
					operatorId = 0L;
				}
			}
		}
		if ("dealer".equals(operatorType)) {
			return "dealer_" + operatorId;
		}
		if (distributorId > 0) {
			return "distributor_" + distributorId;
		}
		return "";
	}

	public long parseDistributorIdForPaymentList(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		return LeadingNumberParser.parseAsLong(s);
	}

	private static long parseLongLoose(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static long parseLongParam(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return defaultVal;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
