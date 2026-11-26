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
 * 仅 OpenAPI 文档；PATCH 请求可选 JSON body，字段来源
 * {@code OpenapiMemberV2UpdateCardCodeGradeHandlerParams#mergeAll}（3 键；mobile 走 mergeString，
 * user_card_code/grade_id 按键存在性合并、body 原值优先不字符串化），
 * 校验与更新见 {@code OpenapiThirdApiV2MemberUpdateCardCodeGradeService}。
 * 注意：query 中的 card_code 参数被 Controller 接收但忽略（未并入 mergeAll）。
 */
@Schema(
		name = "EcxMemberCardCodeGradeUpdateBody",
		description =
				"按手机号定位会员，局部更新会员卡号/等级（3 键，body 含键优先否则 query 同名键）。"
						+ " user_card_code/grade_id 均按键存在性 PATCH：两键都不存在时无操作成功（仍先校验 mobile）；"
						+ " 键存在即参与更新（含 null/空串语义，见各字段）。query card_code 被忽略。")
public class EcxMemberCardCodeGradeUpdateBodyDoc {

	@Schema(
			description =
					"会员手机号（明文）；必填，trim 后须匹配 ^1[3456789][0-9]{9}$"
							+ "（缺失/空白 E5001「手机号必填」，格式错 E5001「手机号填写错误」）；"
							+ "按固定加密后等值查 members.mobile，查无 E5501「会员找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800138000")
	public String mobile;

	@Schema(
			description =
					"新会员卡号；键存在即更新：body 值须字符串或 null（数字/布尔/对象报 E5001「会员卡号参数错误」）；"
							+ "trim 后非空直接用；null/空白自动生成 12 位大写随机码（SHA-1 切片乱序 + Redis zset usercardcode 去重，"
							+ "3 次冲突失败 → E5000「系统错误」）；卡号被其他手机号会员占用报 E5522「会员卡号已存在」"
							+ "（占用者即本手机号则放行）；示例值取自 MemberAccountServiceCreatMemberForH5PostDispatchTest",
			example = "card")
	public String user_card_code;

	@Schema(
			description =
					"新会员等级 ID；键存在即更新：须数值或可 parseDouble 的有限数字字符串（否则 E5001「等级ID参数错误」，"
							+ "null 也报错），小数截断取整；membercard_grade 按 company_id+grade_id 查无报 E5531「会员等级找不到」",
			example = "1")
	public String grade_id;
}