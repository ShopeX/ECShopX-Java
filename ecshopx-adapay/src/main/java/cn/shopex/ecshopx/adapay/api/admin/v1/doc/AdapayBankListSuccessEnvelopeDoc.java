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

package cn.shopex.ecshopx.adapay.api.admin.v1.doc;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；成功信封，对应运行时 {@code ApiResult.ok(List)}
 * （code=200、msg=success，NON_NULL 序列化）。GET 形态无 total_count。
 */
@Schema(name = "AdapayBankListSuccessEnvelope")
public class AdapayBankListSuccessEnvelopeDoc {

	@Schema(description = "业务码，成功固定 200", example = "200")
	public Integer code;

	@Schema(description = "提示文案，成功固定 success", example = "success")
	public String msg;

	@ArraySchema(
			schema = @Schema(implementation = AdapayBankListRowDoc.class),
			arraySchema =
					@Schema(
							description =
									"银行数组（当前页；page_size≤0 时为全量）。无匹配时 []，不含总数字段"))
	public List<AdapayBankListRowDoc> data;
}