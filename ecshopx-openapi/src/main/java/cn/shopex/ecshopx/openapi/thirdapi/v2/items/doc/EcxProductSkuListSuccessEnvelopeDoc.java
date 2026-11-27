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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；成功信封。Handler 显式构造 {@code new OpenapiEnvelope("success","E0000","",data)}
 * （不经 Advice 包装）：**message 固定为空字符串 ""**（PHP 对齐契约，区别于本域「成功」/"ok"）。
 */
@Schema(name = "EcxProductSkuListSuccessEnvelope")
public class EcxProductSkuListSuccessEnvelopeDoc {

	@Schema(description = "业务状态", example = "success")
	public String status;

	@Schema(description = "业务码", example = "E0000")
	public String code;

	@Schema(description = "提示文案（Handler 硬编码固定空字符串）", example = "")
	public String message;

	@Schema(description = "SKU 列表数据（4 固定键 page/page_size/total_count/sku_list）")
	public EcxProductSkuListDataDoc data;
}