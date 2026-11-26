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
 * 仅 OpenAPI 文档；data 新建等级行（9 固定键），来源
 * {@code OpenapiMemberCardGradeOpenApiFormatSupport#formatOpenApiGradeRow}
 * （插入后回读实体格式化；discount/total_consumption 回显入参原样字符串）。
 * 该格式同时用于本域 update/list/detail 接口。
 */
@Schema(name = "EcxMemberCardGradeCreateData")
public class EcxMemberCardGradeCreateDataDoc {

	@Schema(description = "新建等级 ID（membercard_grade.grade_id，整型）", example = "1")
	public Integer grade_id;

	@Schema(description = "是否默认等级（整型 0/1；本接口新建恒 0）", example = "0")
	public Integer is_default;

	@Schema(description = "等级名称（入参原样）", example = "普通会员")
	public String grade_name;

	@Schema(description = "会员折扣（privileges.discount_desc 回显，入参原样字符串；缺失默认 \"0\"）", example = "10")
	public String discount;

	@Schema(
			description = "升级条件（promotion_condition.total_consumption 回显，入参原样字符串；缺失默认 \"0\"）",
			example = "0")
	public String total_consumption;

	@Schema(description = "会员卡背景图 URL（未传为空串）", example = "https://example.test/key.jpg")
	public String background_pic_url;

	@Schema(description = "外部等级唯一标识（未传为空串）", example = "1")
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