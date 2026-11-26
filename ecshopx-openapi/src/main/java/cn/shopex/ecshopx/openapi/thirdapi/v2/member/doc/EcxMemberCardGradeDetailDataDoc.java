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
 * 仅 OpenAPI 文档；data 会员关联等级行（8 固定键，无 is_default——formatOpenApiGradeRow 后显式 remove），
 * 来源 {@code OpenapiThirdApiV2MemberCardGradeDetailService#executeOpenapiDetail}。
 * 会员 grade_id 为 null 按 0 查询；等级行查无（含 grade_id=0）不报错，
 * 返回占位行：grade_id=0、discount/total_consumption="0"、其余字符串键空串。
 */
@Schema(name = "EcxMemberCardGradeDetailData")
public class EcxMemberCardGradeDetailDataDoc {

	@Schema(description = "等级 ID（整型；会员无等级或等级查无为 0）", example = "1")
	public Integer grade_id;

	@Schema(description = "等级名称（等级查无为空串）", example = "普通会员")
	public String grade_name;

	@Schema(description = "会员折扣（privileges.discount_desc；缺失/解析失败默认 \"0\"）", example = "10")
	public String discount;

	@Schema(
			description = "升级条件（promotion_condition.total_consumption；缺失/解析失败默认 \"0\"）",
			example = "0")
	public String total_consumption;

	@Schema(description = "会员卡背景图 URL（查无/null 为空串；示例为格式演示值）", example = "https://example.test/key.jpg")
	public String background_pic_url;

	@Schema(description = "外部等级唯一标识（查无/null 为空串）", example = "1")
	public String external_id;

	@Schema(
			description = "创建时间（yyyy-MM-dd HH:mm:ss，系统时区；等级查无为空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String created;

	@Schema(
			description = "更新时间（yyyy-MM-dd HH:mm:ss，系统时区；等级查无为空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String updated;
}