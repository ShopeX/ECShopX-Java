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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code JurisdictionController#getuser} 经
 * {@code OpenapiRequestParams#mergeString} 读取的全部 key。
 */
@Schema(
		name = "EcxJurisdictionGetuserBody",
		description =
				"员工账号查询参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）")
public class EcxJurisdictionGetuserBodyDoc {

	@Schema(description = "员工手机号（operators.mobile 精确匹配）；查无记录返回失败信封", example = "13800000000")
	public String mobile;

	@Schema(
			description = "账号类型；仅 \"staff\" 会查库，其他值一律返回失败信封（E0001 操作失败）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			allowableValues = {"staff"},
			example = "staff")
	public String type;
}