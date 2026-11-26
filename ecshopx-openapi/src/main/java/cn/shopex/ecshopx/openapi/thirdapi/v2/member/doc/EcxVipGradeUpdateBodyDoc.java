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
 * 仅 OpenAPI 文档；PATCH 请求可选 JSON body，11 键与 query 同名双通道：
 * vip_grade_id 走 {@code originalString}；其余 10 键走 {@code presentOptionalString}
 * （按键存在性 PATCH，body 含键即 present、值原样字符串化）。
 * 校验与局部更新见 {@code OpenapiThirdApiV2MemberCardVipGradeUpdateService}。
 * 注意：discount 与 external_id 被接收并校验（discount），但服务不消费——不落库。
 */
@Schema(
		name = "EcxVipGradeUpdateBody",
		description =
				"更新付费会员卡等级参数（11 键，body 含键优先否则 query 同名键）。"
						+ " vip_grade_id 必填定位；字符串键空串跳过不更新（无法清空）；"
						+ " 三档 fee 按档合并进 price_list（空串跳过）；is_default/is_disabled 键存在即弱布尔覆盖。"
						+ " 全部 PATCH 键缺席时仍会 UPDATE updated 时间戳（非无操作）。"
						+ " discount/external_id 为遗留键：不落库。")
public class EcxVipGradeUpdateBodyDoc {

	@Schema(
			description =
					"付费等级 ID；必填非空（null/空串报 E5001「请求参数错误」）；trim 后 parseLong，"
							+ "失败再按整值 double 截断（如 \"2.0\"→2），仍失败按 -1 查询 → E5541「会员付费等级找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "2")
	public String vip_grade_id;

	@Schema(
			description = "新付费等级名称；键存在且非空才更新（原样字符串；空串跳过保留原值，无法清空）",
			example = "Gold")
	public String grade_name;

	@Schema(
			description =
					"月卡金额（元）；键存在且非空时须可 parseDouble ≥0（否则 E5001「请求参数错误」）；"
							+ "空串跳过不更新；生效时合并进 price_list monthly 档（已有档改 price，无档新建 30 天档），"
							+ "原样字符串落库",
			example = "0")
	public String monthly_fee;

	@Schema(
			description = "季卡金额（元，90 天档）；校验与合并语义同 monthly_fee",
			example = "0")
	public String quarter_fee;

	@Schema(
			description = "年卡金额（元，365 天档）；校验与合并语义同 monthly_fee",
			example = "0")
	public String year_fee;

	@Schema(
			description =
					"遗留参数：被接收并校验（键存在且非空须 1≤值≤10，否则 E5001「请求参数错误」），"
							+ "但服务不消费——privileges 不落库、响应 discount 回显原值",
			deprecated = true,
			example = "10")
	public String discount;

	@Schema(description = "新导购标题；键存在且非空才更新（空串跳过保留原值，无法清空）")
	public String guide_title;

	@Schema(description = "新等级描述；键存在且非空才更新（空串跳过保留原值，无法清空）")
	public String description;

	@Schema(
			description =
					"是否默认等级；键存在即覆盖：弱布尔（空串/\"0\"/\"false\"（忽略大小写）→false，其余 true）；"
							+ "不与其他等级互斥",
			example = "0")
	public String is_default;

	@Schema(
			description = "是否停用；键存在即覆盖，弱布尔语义同 is_default",
			example = "0")
	public String is_disabled;

	@Schema(
			description = "遗留参数：被接收但忽略（服务不写 external_id，响应回显原值）",
			deprecated = true)
	public String external_id;
}