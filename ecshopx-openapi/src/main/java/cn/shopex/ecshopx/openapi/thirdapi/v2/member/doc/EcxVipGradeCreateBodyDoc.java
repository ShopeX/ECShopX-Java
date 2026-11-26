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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，10 键与 query 同名双通道：
 * grade_name/monthly_fee/quarter_fee/year_fee/discount 走 {@code originalString}（不 trim），
 * guide_title/description/is_default/is_disabled/external_id 走 {@code mergeString}（trim）；
 * body 含键优先、值 null 视为未传。校验与落库见
 * {@code OpenapiThirdApiV2MemberCardVipGradeCreateService}。
 */
@Schema(
		name = "EcxVipGradeCreateBody",
		description =
				"创建付费会员卡等级参数（10 键，body 含键优先否则 query 同名键）。"
						+ " lv_type 自动分配（无→vip；已有 vip→svip）；每类型限 1 个（重复 E5542）、"
						+ " 共 2 个上限（E5540）。注意：external_id 被接收但忽略（恒写空串）。")
public class EcxVipGradeCreateBodyDoc {

	@Schema(
			description = "付费等级名称；必填非空（原样落库不 trim；null/空串报 E5001「未填写付费等级名称」）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "Gold")
	public String grade_name;

	@Schema(
			description =
					"月卡购买金额（元，原样字符串存 price_list.price）；三档金额至少一项非 null"
							+ "（全缺 E5001「购买金额必填一项」）；键存在时须非空且可 parseDouble ≥0"
							+ "（空串/负数/非数字报 E5001「请求参数错误」）；null/键缺省该档 price=null",
			example = "0")
	public String monthly_fee;

	@Schema(
			description = "季卡购买金额（元）；校验同 monthly_fee（90 天档）；null/键缺省该档 price=null",
			example = "0")
	public String quarter_fee;

	@Schema(
			description = "年卡购买金额（元）；校验同 monthly_fee（365 天档）；null/键缺省该档 price=null",
			example = "0")
	public String year_fee;

	@Schema(
			description =
					"会员折扣（折）；必填非空（E5001「未填写会员折扣」）；须可 parseDouble 且 1≤值≤10"
							+ "（否则 E5001「请求参数错误」）；原样存 privileges.discount_desc，"
							+ "另存 privileges.discount=100-(int)(值*10)（小数截断，10→0 无折扣）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "10")
	public String discount;

	@Schema(description = "导购标题；可选（mergeString trim；null/空串落库 null，响应回显空串）")
	public String guide_title;

	@Schema(description = "等级描述；可选（mergeString trim；null/空串落库 null，响应回显空串）")
	public String description;

	@Schema(
			description =
					"是否默认等级；弱布尔：null/空串/\"0\"/\"false\"（忽略大小写）→ false，其余任何值 → true；"
							+ "不与其他等级互斥（可同时多个默认）",
			example = "0")
	public String is_default;

	@Schema(
			description = "是否停用；弱布尔语义同 is_default（默认 false）",
			example = "0")
	public String is_disabled;

	@Schema(
			description = "遗留参数：被接收但忽略（服务恒写 external_id=\"\"，入参不落库）",
			deprecated = true)
	public String external_id;
}