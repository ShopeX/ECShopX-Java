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
 * 仅 OpenAPI 文档；data 会员关联付费等级行（14 固定键，无 is_default——formatOpenApiVipGradeRow 后显式 remove），
 * 来源 {@code OpenapiThirdApiV2MemberCardVipGradeDetailService#executeOpenapiDetail}。
 * 关联查 vip_grade_rel_user(company_id, user_id)（不过滤 end_date，过期关联仍返回；
 * rel 的 vip_type/end_date 被读取但不透出）。会员无关联/关联无 vip_grade_id/等级行查无均不报错，
 * 返回占位行：vip_grade_id=0、三档 fee=null、discount="0"、is_disabled=0、其余字符串键空串。
 */
@Schema(name = "EcxVipGradeDetailData")
public class EcxVipGradeDetailDataDoc {

	@Schema(description = "付费等级 ID（整型；无关联或等级查无为 0）", example = "2")
	public Integer vip_grade_id;

	@Schema(description = "等级类型（vip/svip；查无为空串）", example = "vip")
	public String type;

	@Schema(description = "付费等级名称（查无为空串）", example = "Gold")
	public String grade_name;

	@Schema(description = "月卡金额（price_list 回显字符串；无关联/未设档为 null）", nullable = true, example = "0")
	public String monthly_fee;

	@Schema(description = "季卡金额（回显同上；无关联/未设档为 null）", nullable = true)
	public String quarter_fee;

	@Schema(description = "年卡金额（回显同上；无关联/未设档为 null）", nullable = true)
	public String year_fee;

	@Schema(description = "会员折扣（privileges.discount_desc；缺失/解析失败默认 \"0\"）", example = "10")
	public String discount;

	@Schema(description = "导购标题（null 回显空串）", example = "")
	public String guide_title;

	@Schema(description = "等级描述（null 回显空串）", example = "")
	public String description;

	@Schema(description = "背景图 URL（null 回显空串）", example = "")
	public String background_pic_url;

	@Schema(description = "是否停用（整型 0/1；查无为 0）", example = "0")
	public Integer is_disabled;

	@Schema(description = "外部等级唯一标识（null 回显空串）", example = "")
	public String external_id;

	@Schema(
			description = "创建时间（yyyy-MM-dd HH:mm:ss，系统时区；查无为空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String created;

	@Schema(
			description = "更新时间（yyyy-MM-dd HH:mm:ss，系统时区；查无为空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String updated;
}