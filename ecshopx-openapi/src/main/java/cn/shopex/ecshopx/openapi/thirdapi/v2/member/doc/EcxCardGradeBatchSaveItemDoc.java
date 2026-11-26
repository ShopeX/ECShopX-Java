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
 * 仅 OpenAPI 文档；grade_info 数组元素。服务端实际读取的键（{@code OpenapiThirdApiV2MemberCardGradeBatchSaveService}）：
 * grade_id（→ external_id 匹配）、grade_name、grade_level（排序 + promotion_condition.total_consumption）。
 * 其余键透传但本流程不读取。
 */
@Schema(name = "EcxCardGradeBatchSaveItem")
public class EcxCardGradeBatchSaveItemDoc {

	@Schema(
			description =
					"外部等级唯一标识（数云 gradeId，字符串）；写入 membercard_grade.external_id 并按其匹配已有非默认等级；"
							+ "缺省按空串",
			example = "1")
	public String grade_id;

	@Schema(description = "等级名称（原样落库；zh-CN 多语言分表同步）", example = "普通会员")
	public String grade_name;

	@Schema(
			description =
					"等级门槛（累计消费）；数值或数字字符串，非法/缺失按 0；"
							+ "全部行按本值升序排序，最小行成为默认等级（复用既有默认等级行），"
							+ "并写入 promotion_condition.total_consumption",
			example = "0")
	public Object grade_level;
}