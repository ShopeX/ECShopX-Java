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
 * 仅 OpenAPI 文档；会员优惠券行（7 键精简形态，
 * {@code OpenapiDiscountCardV2UserDiscountListPresentationService#toSlimRow}；
 * 数据源 kaquan_user_discount，{@code UserDiscountMapper#selectOpenapiUserDiscountListPage}）。
 */
@Schema(name = "EcxUserdiscountListItem")
public class EcxUserdiscountListItemDoc {

	@Schema(description = "优惠券模板 ID（card_id 整型化，解析失败/缺失为 0）", example = "3")
	public Integer card_id;

	@Schema(
			description = "券码序列号（12 位随机数字，发放时生成；字符串化，null 回退 \"0\"）",
			example = "000000000000")
	public String code;

	@Schema(description = "商派会员 ID（user_id 字符串化，null 回退 \"0\"）", example = "5")
	public String plat_account;

	@Schema(
			description =
					"优惠券类型：discount 折扣券 / cash 代金券 / new_gift 兑换券"
							+ "（kaquan_user_discount.card_type；null 回退 \"0\"）",
			example = "cash")
	public String card_type;

	@Schema(description = "有效期开始 Unix 秒字符串（begin_date；null 回退空串）", example = "1700000000")
	public String begin_date;

	@Schema(description = "有效期结束 Unix 秒字符串（end_date；null 回退 \"0\"）", example = "1800000000")
	public String end_date;

	@Schema(
			description =
					"券状态：unused 未使用（DB status 1/4/10 且未过期）/ redeemed 已核销（DB 2）/"
							+ " expired 已过期（DB 6，或 1/4/10 且 end_date>0 且小于当前秒）/"
							+ " 空串（其他状态，如 3 已转赠、5）",
			example = "unused")
	public String status;
}