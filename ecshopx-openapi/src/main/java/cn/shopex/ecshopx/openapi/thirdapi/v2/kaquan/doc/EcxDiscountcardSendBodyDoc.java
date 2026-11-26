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

package cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源
 * {@code OpenapiThirdApiV2DiscountCardSendParams#validate} 经 {@code OpenapiRequestParams#mergeString}
 * 读取的全部 key。
 */
@Schema(
		name = "EcxDiscountcardSendBody",
		description =
				"单张优惠券发放参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无键读 query 并 trim）")
public class EcxDiscountcardSendBodyDoc {

	@Schema(
			description =
					"商派会员 ID（user_id 整数字符串，>0）；必填非空白否则「商派会员Id参数错误」；"
							+ "非整数/≤0/按租户查无手机号报「会员ID错误，未查询到会员信息」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "5")
	public String plat_account;

	@Schema(
			description =
					"优惠券模板 ID（card_id 整数字符串，>0）；必填非空白否则「优惠券ID参数错误」；"
							+ "非整数/≤0/租户内查无报「领取的优惠券不存在」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "3")
	public String card_id;

	@Schema(
			description =
					"卡券来源中文文案；必填非空白否则「卡券来源参数错误」（无枚举校验，任意文案落库 source_type）。"
							+ "特殊语义：「商城本地领取」校验等级/付费等级可领性并应用每人限领；「大转盘中奖领取」应用每人限领；"
							+ "「达摩CRM批量投放事件」「达摩CRM领取事件」跳过达摩同步发券；"
							+ "其他来源在租户开启达摩 CRM 且券 source_type=dmcrm 时同步向达摩发券",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "商城本地领取")
	public String source_type;

	@Schema(description = "活动名称；可选，缺省空串，落库 activity_name", example = "")
	public String activity_name;
}