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
 * 仅 OpenAPI 文档；data 付费等级行（15 固定键），来源
 * {@code OpenapiMemberCardVipGradeOpenApiFormatSupport#formatOpenApiVipGradeRow}。
 * 该格式同时用于本域 update/list/detail 接口。
 * monthly_fee/quarter_fee/year_fee 回显 price_list 中入参原样字符串（未传档为 null）；
 * type 为 lv_type（vip/svip）。
 */
@Schema(name = "EcxVipGradeCreateData")
public class EcxVipGradeCreateDataDoc {

	@Schema(description = "付费等级 ID（kaquan_vip_grade.vip_grade_id，整型）", example = "2")
	public Integer vip_grade_id;

	@Schema(description = "等级类型（自动分配：vip/svip）", example = "vip")
	public String type;

	@Schema(description = "付费等级名称（入参原样）", example = "Gold")
	public String grade_name;

	@Schema(description = "月卡金额（price_list 回显，入参原样字符串；未传为 null）", nullable = true, example = "0")
	public String monthly_fee;

	@Schema(description = "季卡金额（回显同上；未传为 null）", nullable = true)
	public String quarter_fee;

	@Schema(description = "年卡金额（回显同上；未传为 null）", nullable = true)
	public String year_fee;

	@Schema(description = "会员折扣（privileges.discount_desc 回显，入参原样字符串；缺失默认 \"0\"）", example = "10")
	public String discount;

	@Schema(description = "导购标题（null 回显空串）", example = "")
	public String guide_title;

	@Schema(description = "等级描述（null 回显空串）", example = "")
	public String description;

	@Schema(description = "背景图 URL（create 恒写空串）", example = "")
	public String background_pic_url;

	@Schema(description = "是否默认等级（整型 0/1）", example = "0")
	public Integer is_default;

	@Schema(description = "是否停用（整型 0/1）", example = "0")
	public Integer is_disabled;

	@Schema(description = "外部等级唯一标识（create 恒空串，入参被忽略）", example = "")
	public String external_id;

	@Schema(
			description = "创建时间（yyyy-MM-dd HH:mm:ss，系统时区；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String created;

	@Schema(
			description = "更新时间（yyyy-MM-dd HH:mm:ss，系统时区；新建时与 created 相同；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String updated;
}