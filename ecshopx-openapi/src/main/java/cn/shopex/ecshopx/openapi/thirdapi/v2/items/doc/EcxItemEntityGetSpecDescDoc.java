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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；item_spec_desc 元素（规格描述，4 键）。
 * {@code OpenapiThirdApiV2NormalItemsEntityDetailService#formatItemSpecDesc}。
 */
@Schema(name = "EcxItemEntityGetSpecDesc")
public class EcxItemEntityGetSpecDescDoc {

	@Schema(description = "规格属性 ID", example = "1")
	public Long spec_id;

	@Schema(description = "规格名（属性名）", example = "500mg")
	public String spec_name;

	@Schema(description = "是否图片规格（库原样值）")
	public Object is_image;

	@ArraySchema(
			arraySchema = @Schema(description = "规格值数组"),
			schema = @Schema(implementation = EcxItemEntityGetSpecValueDoc.class))
	public List<EcxItemEntityGetSpecValueDoc> spec_values;
}