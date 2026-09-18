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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class DistributorUpdateParamValidator {

	private static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");
	private static final Pattern TEL = Pattern.compile("^\\d{3,4}-?\\d{7,8}$");

	private DistributorUpdateParamValidator() {
	}

	public static void validate(Map<String, Object> merged, long pathDistributorId, Set<String> datapassBlockCols) {
		if (pathDistributorId < 1L) {
			throw new BadRequestException("店铺ID无效");
		}
		if (merged.containsKey("is_ziti") || merged.containsKey("is_delivery") || merged.containsKey("is_self_delivery")) {
			boolean ziti = truthy(merged.get("is_ziti"));
			boolean delivery = truthy(merged.get("is_delivery"));
			boolean selfDel = truthy(merged.get("is_self_delivery"));
			if (!ziti && !delivery && !selfDel) {
				throw new ResourceException("至少选择一种配送方式");
			}
		}
		String shopCode = str(merged.get("shop_code"));
		if (!StringUtils.hasText(shopCode)) {
			return;
		}
		if (!shopCode.matches("^[A-Za-z0-9-]+$")) {
			throw new BadRequestException("店铺编号格式不正确");
		}
		String contract = str(merged.get("contract_phone"));
		if (StringUtils.hasText(contract) && !"0".equals(contract) && !contract.matches("^\\d{3,4}-?\\d{7,8}$")) {
			throw new BadRequestException("固定电话格式不正确");
		}
		Map<String, Object> v = new LinkedHashMap<>(merged);
		Set<String> block = datapassBlockCols != null ? datapassBlockCols : Set.of();
		for (String col : block) {
			v.remove(col);
		}
		requireText(v, "name", "店铺名称必填");
		maxLen(v, "name", 255, "店铺名称过长");
		if (!block.contains("contact")) {
			requireText(v, "contact", "联系人必填");
			maxLen(v, "contact", 255, "联系人过长");
		}
		if (!block.contains("mobile")) {
			requireText(v, "mobile", "手机号必填");
			if (!validMobileOrTel(v.get("mobile"))) {
				throw new ResourceException("手机号格式不正确");
			}
		}
		requireText(v, "hour", "营业时间必填");
		maxLen(v, "hour", 150, "营业时间过长");
		if (v.get("is_ziti") == null) {
			throw new ResourceException("是否支持自提必填");
		}
		requireText(v, "distribution_type", "店铺类型必填");
		String dist = v.get("distribution_type").toString().trim();
		if ("1".equals(dist) && (v.get("merchant_id") == null || toLong(v.get("merchant_id")) <= 0)) {
			throw new ResourceException("所属商户必填");
		}
		String intro = v.get("introduce") == null ? "" : v.get("introduce").toString();
		if (intro.length() > 1000) {
			throw new ResourceException("店铺介绍过长");
		}
		boolean distributorSelf = truthyOne(merged.get("distributor_self"));
		if (!distributorSelf) {
			requireText(v, "lng", "经度必填");
			requireText(v, "lat", "纬度必填");
			if (v.get("regions_id") == null) {
				throw new ResourceException("区域编码必填");
			}
			if (v.get("regions") == null) {
				throw new ResourceException("区域名称必填");
			}
			maxLen(v, "address", 255, "详细地址过长");
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
	}

	public static Set<String> datapassBlockColsForShopCodeBranch(Map<String, Object> merged) {
		Set<String> out = new HashSet<>();
		String shopCode = str(merged.get("shop_code"));
		if (!StringUtils.hasText(shopCode)) {
			return out;
		}
		String mobile = str(merged.get("mobile"));
		if (mobile.contains("*")) {
			out.add("mobile");
		}
		String contact = str(merged.get("contact"));
		if (contact.contains("*")) {
			out.add("contact");
		}
		return out;
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
