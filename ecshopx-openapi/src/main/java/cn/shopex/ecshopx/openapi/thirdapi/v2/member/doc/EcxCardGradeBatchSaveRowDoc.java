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
 * 仅 OpenAPI 文档；成功 data 数组元素（数云 5 字段）。
 * {@code OpenapiMemberCardGradeOpenApiFormatSupport#formatShuyunGradeRow}：
 * 保存后回读租户全量 membercard_grade（zh-CN 分表覆盖）逐行格式化。
 */
@Schema(name = "EcxCardGradeBatchSaveRow")
public class EcxCardGradeBatchSaveRowDoc {

	@Schema(description = "外部等级标识（external_id；空值回退空串）——注意非本系统 grade_id", example = "1")
	public String grade_id;

	@Schema(description = "等级名称（zh-CN 分表非空覆盖；空值回退空串）", example = "普通会员")
	public String grade_name;

	@Schema(
			description = "等级门槛（promotion_condition.total_consumption 字符串化；缺失回退 \"0\"）",
			example = "0")
	public String grade_level;

	@Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss（系统时区；非数值为 \"\"）", example = "2020-09-13 20:26:40")
	public String created;

	@Schema(description = "更新时间 yyyy-MM-dd HH:mm:ss（非数值为 \"\"）", example = "2023-11-15 06:13:21")
	public String updated;
}