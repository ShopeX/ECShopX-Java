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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

public final class OpenapiDiscountCardV2SendPhpMessages {

	private OpenapiDiscountCardV2SendPhpMessages() {}

	public static final String MSG_PLAT_ACCOUNT_PARAM = "商派会员Id参数错误";

	public static final String MSG_CARD_ID_PARAM = "优惠券ID参数错误";

	public static final String MSG_SOURCE_TYPE_PARAM = "卡券来源参数错误";

	public static final String MSG_MEMBER_NOT_FOUND = "会员ID错误，未查询到会员信息";

	public static final String MSG_COUPON_RECORD_ADD_FAILED = "领取卡券记录添加失败, company_id 为空";

	public static final String MSG_COUPON_NOT_EXIST = "领取的优惠券不存在";

	public static final String MSG_COUPON_EXPIRED = "领取优惠券失败，优惠券已过期";

	public static final String MSG_COUPON_OUT_OF_STOCK = "领取的优惠券失败，库存不足了";

	public static final String MSG_STATUS_ABNORMAL = "不可领取！卡券状态非正常";

	public static final String MSG_NOT_RECEIVABLE = "不可领取！";

	public static final String MSG_ALREADY_RECEIVED_LIMIT = "您已经领过该券，请到个人中心中查看哦~";

	public static final String MSG_ALREADY_RECEIVED_SAME_USER = "您已经领取该卡券";

	public static final String MSG_FAILED_TO_RECEIVE = "领取该卡券失败";
}
