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
 * 仅 OpenAPI 文档；成功信封。Handler 显式构造 {@code new OpenapiEnvelope("success","E0000","成功",data)}
 * （不经 Advice 包装）：message 固定「成功」。逐行失败不影响整体 success——失败明细在 data 三数组中。
 */
@Schema(name = "EcxProductStockUpdateSuccessEnvelope")
public class EcxProductStockUpdateSuccessEnvelopeDoc {

	@Schema(description = "业务状态", example = "success")
	public String status;

	@Schema(description = "业务码", example = "E0000")
	public String code;

	@Schema(description = "提示文案（Handler 硬编码固定「成功」）", example = "成功")
	public String message;

	@Schema(description = "更新结果（3 固定键 not_find_sku/not_update_sku/update_error；均空即全部成功）")
	public EcxProductStockUpdateDataDoc data;
}