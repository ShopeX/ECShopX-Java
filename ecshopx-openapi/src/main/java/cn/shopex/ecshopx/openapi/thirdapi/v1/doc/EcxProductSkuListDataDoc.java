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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code OpenapiProductSkuListPortImpl#listNormalSkus}：item_type=normal 过滤，item_id 倒序分页；
 * page/page_size 为归一化后回显（page≥1、page_size≤0→100、>2000→2000）。
 */
@Schema(name = "EcxProductSkuListData", description = "SKU 分页（归一化 page/page_size 回显）")
public class EcxProductSkuListDataDoc {

	@Schema(description = "当前页码（归一化后，≥1）", example = "1")
	public Integer page;

	@Schema(description = "每页条数（归一化后：≤0→100，>2000→2000）", example = "100")
	public Integer page_size;

	@Schema(description = "普通商品（item_type=normal）总数", example = "1")
	public Long total_count;

	@ArraySchema(arraySchema = @Schema(description = "SKU 列表（item_id 倒序）；无商品时为空数组"))
	public List<SkuRowDoc> sku_list;

	@Schema(name = "EcxProductSkuListRow", description = "单条 SKU（2 键）")
	public static class SkuRowDoc {

		@Schema(description = "商品名称（按 country_code 语言分表覆盖）", nullable = true)
		public String name;

		@Schema(description = "SKU 编码（items.item_bn 货品编码）", nullable = true)
		public String sku_id;
	}
}