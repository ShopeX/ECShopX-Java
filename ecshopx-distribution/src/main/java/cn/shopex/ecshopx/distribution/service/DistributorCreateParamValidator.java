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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class DistributorCreateParamValidator {

	private static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");
	private static final Pattern TEL = Pattern.compile("^\\d{3,4}-?\\d{7,8}$");

	private DistributorCreateParamValidator() {
	}

	public static void validate(Map<String, Object> merged) {
		requireText(merged, "name", "店铺名称必填");
		maxLen(merged, "name", 255, "店铺名称过长");
		requireText(merged, "contact", "联系人必填");
		maxLen(merged, "contact", 255, "联系人过长");
		requireText(merged, "mobile", "手机号必填");
		if (!validMobileOrTel(merged.get("mobile"))) {
			throw new ResourceException("手机号格式不正确");
		}
		requireText(merged, "hour", "营业时间必填");
		maxLen(merged, "hour", 150, "营业时间过长");
		if (merged.get("is_ziti") == null) {
			throw new ResourceException("是否支持自提必填");
		}
		requireText(merged, "distribution_type", "店铺类型必填");
		String dist = merged.get("distribution_type").toString().trim();
		if ("1".equals(dist) && (merged.get("merchant_id") == null || toLong(merged.get("merchant_id")) <= 0)) {
			throw new ResourceException("所属商户必填");
		}
		String intro = merged.get("introduce") == null ? "" : merged.get("introduce").toString();
		if (intro.length() > 1000) {
			throw new ResourceException("店铺介绍过长");
		}
		boolean distributorSelf = truthyOne(merged.get("distributor_self"));
		if (!distributorSelf) {
			requireText(merged, "lng", "经度必填");
			requireText(merged, "lat", "纬度必填");
			if (merged.get("regions_id") == null) {
				throw new ResourceException("区域编码必填");
			}
			if (merged.get("regions") == null) {
				throw new ResourceException("区域名称必填");
			}
			maxLen(merged, "address", 255, "详细地址过长");
		}
		boolean dadaOn = truthy(merged.get("is_dada"));
		if (!dadaOn) {
			requireText(merged, "shop_code", "店铺编号必填");
		}
		boolean offSelf = merged.get("offline_aftersales_self") instanceof Boolean b && b;
		boolean offOther = merged.get("offline_aftersales_other") instanceof Boolean b && b;
		if (offSelf || offOther) {
			@SuppressWarnings("unchecked")
			Map<String, Object> addr = (Map<String, Object>) merged.get("offline_aftersales_address");
			if (addr == null) {
				throw new ResourceException("退货地址必填");
			}
			requireInMap(addr, "name", "退货点名称必填");
			if (addr.get("regions") == null) {
				throw new ResourceException("退货点区域必填");
			}
			if (addr.get("regions_id") == null) {
				throw new ResourceException("退货点区域必填");
			}
			requireInMap(addr, "address", "退货点地址必填");
			requireInMap(addr, "mobile", "退货点电话必填");
			requireInMap(addr, "hours", "退货点营业时间必填");
		}
		forbidNonEmptyDistributorIds(merged);
		boolean ziti = truthy(merged.get("is_ziti"));
		boolean delivery = truthy(merged.get("is_delivery"));
		boolean selfDel = truthy(merged.get("is_self_delivery"));
		if (!ziti && !delivery && !selfDel) {
			throw new ResourceException("至少选择一种配送方式");
		}
		String shopCode = str(merged.get("shop_code"));
		if (StringUtils.hasText(shopCode) && !shopCode.matches("^[A-Za-z0-9-]+$")) {
			throw new ResourceException("店铺编号格式不正确");
		}
		String contract = str(merged.get("contract_phone"));
		if (StringUtils.hasText(contract) && !"0".equals(contract) && !contract.matches("^\\d{3,4}-?\\d{7,8}$")) {
			throw new ResourceException("固定电话格式不正确");
		}
	}

	private static void forbidNonEmptyDistributorIds(Map<String, Object> merged) {
		Object v = merged.get("distributorIds");
		if (v == null) {
			return;
		}
		if (v instanceof List<?> list && !list.isEmpty()) {
			throw new ForbiddenException("无权限通过该方式创建店铺");
		}
		if (v instanceof String s && StringUtils.hasText(s) && !"[]".equals(s.trim())) {
			throw new ForbiddenException("无权限通过该方式创建店铺");
		}
	}

	private static void requireText(Map<String, Object> m, String k, String msg) {
		if (!StringUtils.hasText(str(m.get(k)))) {
			throw new ResourceException(msg);
		}
	}

	private static void maxLen(Map<String, Object> m, String k, int max, String msg) {
		String s = str(m.get(k));
		if (s.length() > max) {
			throw new ResourceException(msg);
		}
	}

	private static void requireInMap(Map<String, Object> m, String k, String msg) {
		if (!StringUtils.hasText(str(m.get(k)))) {
			throw new ResourceException(msg);
		}
	}

	private static boolean validMobileOrTel(Object o) {
		String s = str(o);
		return MOBILE.matcher(s).matches() || TEL.matcher(s).matches();
	}

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"false".equalsIgnoreCase(s) && !"0".equals(s);
	}

	private static boolean truthyOne(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
