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

package cn.shopex.ecshopx.shuyun.gateway;

/** 数云开放网关 Action 字符串目录（对齐 PHP 常量）。 */
public final class ShuyunOpenPlatformGatewayActions {

	public static final String TRADE_SYNC = "shuyun.base.trade.sync";
	public static final String REFUND_SYNC = "shuyun.base.refund.sync";
	public static final String SHOP_BATCH_REGISTER = "shuyun.base.shop.batch.register";
	public static final String PLATFORM_SHOP_BATCH_REGISTER = "shuyun.base.platform.shop.batch.register";
	public static final String MEMBER_REGISTER = "shuyun.loyalty.member.register";
	public static final String MEMBER_BIND_PUSH = "shuyun.private.bind.push";
	public static final String MEMBER_ENHANCE_POST = "shuyun.loyalty.enhance.member.post";
	public static final String MEMBER_POINT_CHANGE = "shuyun.loyalty.member.point.change";
	public static final String MEMBER_POINT_CHANGELOG_SEARCH = "shuyun.loyalty.member.point.changelog.search";
	public static final String MEMBER_ENHANCE_QUERY_DETAIL = "shuyun.loyalty.enhance.member.query.detail";
	public static final String MEMBER_MODIFY = "shuyun.loyalty.member.modify";
	public static final String MEMBER_UNBIND = "shuyun.loyalty.member.unbind";
	public static final String LOYALTY_CARD_GRADE_QUERY = "shuyun.loyalty.card.grade.query";
	public static final String PRODUCT_CATEGORY_SYNC = "shuyun.base.product.category.sync";
	public static final String PRODUCT_SYNC = "shuyun.base.product.sync";
	public static final String OFFLINE_BENEFIT_SEND_REPORT_PUSH_V2 = "shuyun.offline.benefit.send.report.push.v2";
	public static final String OFFLINE_BENEFIT_SEND_RESULT_DETAIL_PUSH_V2 =
			"shuyun.offline.benefit.send.result.detail.push.v2";
	public static final String OFFLINE_BENEFIT_RESULT_PUSH_V2 = "shuyun.offline.benefit.result.push.v2";

	private ShuyunOpenPlatformGatewayActions() {}
}
