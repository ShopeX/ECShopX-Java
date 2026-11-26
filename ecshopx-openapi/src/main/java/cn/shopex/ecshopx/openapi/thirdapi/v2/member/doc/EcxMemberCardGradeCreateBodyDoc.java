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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，5 键与 query 同名双通道：
 * grade_name/discount/total_consumption 走 {@code OpenapiRequestParams#originalString}（不 trim），
 * background_pic_url/external_id 走 {@code mergeString}（trim）；body 含键优先、值 null 视为未传。
 * 校验与落库见 {@code OpenapiThirdApiV2MemberCardGradeCreateService}。
 */
@Schema(
		name = "EcxMemberCardGradeCreateBody",
		description =
				"创建会员卡等级参数（5 键，body 含键优先否则 query 同名键）。"
						+ " 新建等级恒非默认（is_default=0）；租户等级总数达 6 拒绝创建"
						+ "（E5530「最多只能创建5个会员等级」，文案如此、实际含默认等级共 6 上限）。")
public class EcxMemberCardGradeCreateBodyDoc {

	@Schema(
			description = "等级名称；必填非空（原样落库不 trim；null/空串报 E5001「未填写等级名称」）；zh-CN 多语言分表同步",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "普通会员")
	public String grade_name;

	@Schema(
			description =
					"会员折扣（折）；必填非空（E5001「未填写会员折扣」）；须可 parseDouble 且 1≤值≤10"
							+ "（否则 E5001「请求参数错误」）；原样字符串存 privileges.discount_desc，"
							+ "另存 privileges.discount=100-(int)(值*10)（小数截断，如 9.5→5、10→0 无折扣）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "10")
	public String discount;

	@Schema(
			description =
					"升级条件（累计消费门槛）；必填非空（E5001「未填写升级条件」）；须可 parseDouble 且 ≥0"
							+ "（否则 E5001「请求参数错误」）；原样字符串存 promotion_condition.total_consumption",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "0")
	public String total_consumption;

	@Schema(
			description = "会员卡背景图 URL；可选（mergeString trim；null 按空串落库）；zh-CN 多语言分表同步；示例为格式演示值",
			example = "https://example.test/key.jpg")
	public String background_pic_url;

	@Schema(
			description = "外部等级唯一标识（对接方 ID）；可选（mergeString trim；null 按空串落库）；不校验唯一性",
			example = "1")
	public String external_id;
}