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
 * 仅 OpenAPI 文档；PATCH 请求可选 JSON body，6 键与 query 同名双通道：
 * grade_id 走 {@code OpenapiRequestParams#originalString}；其余 5 键走
 * {@code presentOptionalString}（按键存在性 PATCH，body 含键即 present、值原样字符串化）。
 * 校验与局部更新见 {@code OpenapiThirdApiV2MemberCardGradeUpdateService}。
 */
@Schema(
		name = "EcxMemberCardGradeUpdateBody",
		description =
				"修改会员卡等级参数（6 键，body 含键优先否则 query 同名键）。"
						+ " grade_id 必填定位；其余 5 键按键存在性 PATCH——全部缺席或均无可生效值时"
						+ " 无操作成功（data=null，等级存在性仍先校验）。"
						+ " 注意：PATCH 键值为 JSON null/非字符串时经接口 Optional 通道存在运行时风险（见各字段说明）。")
public class EcxMemberCardGradeUpdateBodyDoc {

	@Schema(
			description =
					"等级 ID；必填非空（null/空串报 E5001「请求参数错误」）；原样字符串（不 trim）等值匹配"
							+ " membercard_grade.grade_id，查无报 E5531「会员等级找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String grade_id;

	@Schema(
			description = "新等级名称；键存在即更新（原样字符串不 trim，空串可写入清空名称）；zh-CN 多语言分表同步",
			example = "普通会员")
	public String grade_name;

	@Schema(
			description =
					"新会员折扣（折）；键存在且非空时须可 parseDouble 且 1≤值≤10（否则 E5001「请求参数错误」）；"
							+ "空串跳过校验且不更新（保留原值）；生效时整体重建 privileges JSON"
							+ "（discount_desc=原样字符串、discount=100-(int)(值*10) 小数截断）",
			example = "10")
	public String discount;

	@Schema(
			description =
					"新升级条件（累计消费门槛）；键存在且非空时须可 parseDouble 且 ≥0（否则 E5001「请求参数错误」）；"
							+ "空串跳过校验且不更新；生效时整体重建 promotion_condition JSON（total_consumption=原样字符串）",
			example = "0")
	public String total_consumption;

	@Schema(
			description = "新会员卡背景图 URL；键存在即更新（原样字符串，空串可写入清空）；zh-CN 多语言分表同步；示例为格式演示值",
			example = "https://example.test/key.jpg")
	public String background_pic_url;

	@Schema(
			description = "新外部等级唯一标识；键存在即更新（原样字符串，空串可写入清空）；不校验唯一性",
			example = "1")
	public String external_id;
}