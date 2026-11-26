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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code MemberCardGradeController#batchSave}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key。
 */
@Schema(
		name = "EcxCardGradeBatchSaveBody",
		description =
				"数云批量设置会员卡等级参数（body 与 query 同名 key 双通道：body 含键取原始值不 trim，否则用 query）。"
						+ " 全量覆盖语义：本次未保留的既有等级会被删除（有会员在用的等级阻止整个请求）。")
public class EcxCardGradeBatchSaveBodyDoc {

	@ArraySchema(
			arraySchema =
					@Schema(
							description =
									"等级数组 JSON 字符串（也可直接传 JSON 数组，originalString 会序列化）；"
											+ "必填非空数组且至少一个对象元素，否则 E5001「请求参数错误」",
							requiredMode = Schema.RequiredMode.REQUIRED),
			schema = @Schema(implementation = EcxCardGradeBatchSaveItemDoc.class))
	public List<EcxCardGradeBatchSaveItemDoc> grade_info;
}