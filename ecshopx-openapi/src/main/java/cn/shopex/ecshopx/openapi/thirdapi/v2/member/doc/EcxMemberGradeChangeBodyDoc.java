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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，3 键与 query 同名双通道，来源
 * {@code OpenapiMemberV2UpdateGradeHandlerParams#mergeAll}（body 含键取原值，
 * 否则 query mergeString trim）。校验与更新见
 * {@code OpenapiThirdApiV2MemberUpdateGradeService}。
 * 注意：本接口 grade_id 语义为**外部等级标识 external_id**（数云对接），非平台内部等级 ID；
 * 区别于 ecx.member_card_grade.update（#PATCH 修改会员卡等级设置）。
 */
@Schema(
		name = "EcxMemberGradeChangeBody",
		description =
				"修改会员等级参数（3 键，body 含键优先否则 query 同名键，均必填）。"
						+ " plat_account 为平台会员 user_id；grade_id 为外部等级标识（按 membercard_grade.external_id"
						+ " 匹配换取内部等级）；grade_level 仅做非空字符串校验、不参与业务逻辑。")
public class EcxMemberGradeChangeBodyDoc {

	@Schema(
			description =
					"商派会员 Id（members.user_id）；必填，数值或可 parseDouble 的有限数字字符串"
							+ "（小数截断取整；null/空白/非数字/布尔报 E5001「商派会员Id必填」）；"
							+ "按 company_id+user_id 查无报 E5501「会员找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "50")
	public String plat_account;

	@Schema(
			description =
					"外部等级标识（membercard_grade.external_id，数云 gradeId）；必填，数值或可 parseDouble"
							+ " 的有限数字字符串（否则 E5001「等级ID参数必填」）；trim 后字符串化按"
							+ " company_id+external_id 查等级，查无报 E5531「会员等级找不到」；"
							+ "**注意小数会被字符串化保留（\"1.0\" 匹配 external_id=\"1.0\" 而非 \"1\"）**",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String grade_id;

	@Schema(
			description =
					"等级门槛（累计消费）；必填且须**非空白字符串**（数值/布尔/null 报 E5001「等级参数必填」——"
							+ "body 直传数字会因类型校验失败，须传字符串）；仅校验不落库不参与升级判定"
							+ "（升级活动按库内新旧等级 promotion_condition.total_consumption 比较）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "0")
	public String grade_level;
}