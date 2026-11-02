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

package cn.shopex.ecshopx.espier.security;

/**
 * G-L8：POS JWT 可访问 path（auth 精确 + §4.3 前缀）。Operator JWT 不受此表约束。
 */
public final class PosJwtPathWhitelist {

	private PosJwtPathWhitelist() {}

	public static boolean isAllowed(String method, String path) {
		if (path == null || path.isBlank()) {
			return false;
		}
		String p = path;
		if ("POST".equalsIgnoreCase(method) && "/api/v1/pos/auth/login".equals(p)) {
			return true;
		}
		if ("POST".equalsIgnoreCase(method) && "/api/v1/pos/auth/logout".equals(p)) {
			return true;
		}
		if ("GET".equalsIgnoreCase(method) && "/api/v1/pos/auth/me".equals(p)) {
			return true;
		}
		if (p.startsWith("/api/v1/order")) {
			return true;
		}
		if (p.startsWith("/api/v1/aftersales")) {
			return true;
		}
		if ("/api/v1/checkout".equals(p) || p.startsWith("/api/v1/checkout/")) {
			return true;
		}
		if (p.startsWith("/api/v1/operator/cart")
				|| p.startsWith("/api/v1/operator/pending")
				|| p.startsWith("/api/v1/operator/cartdata")
				|| "/api/v1/operator/scancodeAddcart".equals(p)) {
			return true;
		}
		// 商品读：精确 list + onsale；不放开整棵 /goods/**
		if ("GET".equalsIgnoreCase(method)
				&& ("/api/v1/goods/items".equals(p) || "/api/v1/goods/items/onsale".equals(p))) {
			return true;
		}
		// 门店列表（精确 GET；不放开 /distributors/** 写/配置）
		if ("GET".equalsIgnoreCase(method) && "/api/v1/distributors".equals(p)) {
			return true;
		}
		// 收银会员：精确 /member；GET /members 搜索；验码；标签族 tag*/reltag*
		if ("/api/v1/member".equals(p)
				|| ("GET".equalsIgnoreCase(method) && "/api/v1/members".equals(p))
				|| ("GET".equalsIgnoreCase(method)
						&& ("/api/v1/member/image/code".equals(p) || "/api/v1/member/sms/code".equals(p)))
				|| p.startsWith("/api/v1/member/tag")
				|| "/api/v1/member/reltag".equals(p)
				|| "/api/v1/member/reltagdel".equals(p)) {
			return true;
		}
		// 物流：下单可用列表 + 公司物流配置
		if (("GET".equalsIgnoreCase(method) && "/api/v1/trade/logistics/list".equals(p))
				|| p.startsWith("/api/v1/company/logistics")) {
			return true;
		}
		// 员工只读列表/详情（收银选人；不放开 POST/PATCH/DELETE 写员工）
		if ("GET".equalsIgnoreCase(method)
				&& ("/api/v1/account/management".equals(p) || p.startsWith("/api/v1/account/management/"))) {
			return true;
		}
		// 优惠券可用列表、省市区地址树（精确 GET）
		if ("GET".equalsIgnoreCase(method)
				&& ("/api/v1/getUserCardList".equals(p) || "/api/v1/espier/address".equals(p))) {
			return true;
		}
		// 订单发货（精确 POST；不放开 /delivery/** 列表等）
		if ("POST".equalsIgnoreCase(method) && "/api/v1/delivery".equals(p)) {
			return true;
		}
		// 自提核销：扫码 + 按单号（路径不在 /api/v1/order* 下）
		if ("POST".equalsIgnoreCase(method) && "/api/v1/qr_writeoff".equals(p)) {
			return true;
		}
		if (p.startsWith("/api/v1/writeoff/")) {
			return true;
		}
		return false;
	}

	/** 明确排除的纯 Admin（即便前缀误配也不放行）。 */
	public static boolean isExplicitlyDeniedAdmin(String path) {
		if (path == null) {
			return false;
		}
		return path.startsWith("/api/v1/store/pos-accounts");
	}
}
