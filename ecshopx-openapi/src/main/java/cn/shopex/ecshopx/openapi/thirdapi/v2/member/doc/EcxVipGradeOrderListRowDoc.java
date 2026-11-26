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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；购买记录行（11 固定键），来源
 * {@code OpenapiMemberCardVipGradeOrderOpenApiFormatSupport#formatOpenApiVipGradeOrderRow}
 * （kaquan_vip_grade_order，order_id 降序）。
 * price 分→元 2 位小数字符串；discount 由库内折扣额度反推「折」1 位小数字符串；
 * card_type 取库内 JSON 的 day 键整型（monthly=30/quarter=90/year=365）。
 */
@Schema(name = "EcxVipGradeOrderListRow")
public class EcxVipGradeOrderListRowDoc {

	@Schema(description = "订单号（字符串化的 bigint；null 为空串）", example = "100")
	public String order_id;

	@Schema(description = "付款金额（元，字符串 2 位小数；库内分 HALF_UP 除 100；null 按 0 → \"0.00\"）", example = "0.00")
	public String price;

	@Schema(description = "会员用户 ID（整型；null/非法按 0）", example = "50")
	public Integer user_id;

	@Schema(description = "会员手机号（明文；密文解密，解密失败回退原存储值；null 为空串）", example = "13800138000")
	public String mobile;

	@Schema(description = "付费会员卡等级 ID（整型；null 按 0）", example = "2")
	public Integer vip_grade_id;

	@Schema(description = "等级类型（vip/svip；null 为空串）", example = "vip")
	public String lv_type;

	@Schema(description = "会员卡标题（null 为空串）", example = "Gold")
	public String title;

	@Schema(
			description =
					"卡片时长（天，整型）：取库内 card_type JSON 的 day 键（monthly=30/quarter=90/year=365）；"
							+ "缺失/解析失败按 0",
			example = "30")
	public Integer card_type;

	@Schema(
			description =
					"会员折扣（折，字符串 1 位小数）：(100-库内折扣额度)/10 HALF_UP；"
							+ "额度 0（无折扣）→ \"10.0\"",
			example = "10.0")
	public String discount;

	@Schema(
			description = "创建时间（yyyy-MM-dd HH:mm:ss，系统时区；null 为空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String created;

	@Schema(
			description = "更新时间（yyyy-MM-dd HH:mm:ss，系统时区；null 为空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String updated;
}