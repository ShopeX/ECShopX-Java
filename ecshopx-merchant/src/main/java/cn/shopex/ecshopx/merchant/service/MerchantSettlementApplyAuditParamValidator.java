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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MerchantSettlementApplyAuditParamValidator {

	public Map<String, Object> validate(Map<String, Object> merged) {
		Map<String, Object> out = new LinkedHashMap<>();

		Object rawId = merged.get("id");
		if (rawId == null || isBlankScalar(rawId)) {
			throw new ResourceException("申请ID必填");
		}
		long id = toPositiveLong(rawId, "申请ID无效");
		out.put("id", id);

		Object rawStatus = merged.get("audit_status");
		if (rawStatus == null || isBlankScalar(rawStatus)) {
			throw new ResourceException("审核状态必填");
		}
		String auditStatus = normalizeAuditStatus(rawStatus);
		if (auditStatus == null) {
			throw new ResourceException("审核状态无效");
		}
		out.put("audit_status", auditStatus);

		Object rawGoods = merged.get("audit_goods");
		if (rawGoods == null || isBlankScalar(rawGoods)) {
			throw new ResourceException("审核商品必填");
		}
		out.put("audit_goods", normalizeAuditGoods(rawGoods));

		if (merged.containsKey("audit_memo")) {
			Object memo = merged.get("audit_memo");
			if (memo instanceof String s && !s.isEmpty()) {
				if (s.codePointCount(0, s.length()) > 300) {
					throw new ResourceException("审核备注不能超过300个字符");
				}
			}
			out.put("audit_memo", memo);
		}

		return out;
	}

	private static String normalizeAuditStatus(Object raw) {
		String s = String.valueOf(raw).trim();
		if ("2".equals(s) || "3".equals(s)) {
			return s;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			if (v == 2) {
				return "2";
			}
			if (v == 3) {
				return "3";
			}
		}
		return null;
	}

	private static boolean normalizeAuditGoods(Object raw) {
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			if (v == 0) {
				return false;
			}
			if (v == 1) {
				return true;
			}
			throw new ResourceException("审核商品无效");
		}
		String s = String.valueOf(raw).trim();
		if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
			return true;
		}
		if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
			return false;
		}
		throw new ResourceException("审核商品无效");
	}

	private static boolean isBlankScalar(Object o) {
		if (o instanceof String s) {
			return s.isBlank();
		}
		return false;
	}

	private static long toPositiveLong(Object raw, String badMessage) {
		try {
			if (raw instanceof Number n) {
				long v = n.longValue();
				if (v > 0) {
					return v;
				}
			} else {
				long v = Long.parseLong(String.valueOf(raw).trim());
				if (v > 0) {
					return v;
				}
			}
		} catch (NumberFormatException ignored) {
		}
		throw new ResourceException(badMessage);
	}
}
