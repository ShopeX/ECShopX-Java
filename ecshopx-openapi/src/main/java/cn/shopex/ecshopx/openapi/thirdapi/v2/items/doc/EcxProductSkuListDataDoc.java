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
import java.util.List;

/**
 * 仅 OpenAPI 文档；data 结构（4 固定键，键序 page/page_size/total_count/sku_list），来源
 * {@code OpenapiThirdApiV2ProductSkuListService#executeOpenapiProductSkuList}。
 * page/page_size 为**归一后生效值**回显（page≥1；page_size≤0→100、>2000→2000）；
 * total_count 为满足过滤（item_type=normal + updated 窗口）的 SKU 行总数；
 * sku_list 行 3 固定键（item_id 降序）；name 经 country_code 语言分表覆盖（默认 zh-CN）。
 */
@Schema(name = "EcxProductSkuListData")
public class EcxProductSkuListDataDoc {

	@Schema(description = "生效页码回显（整型，≥1）", example = "1")
	public Integer page;

	@Schema(description = "生效每页数量回显（整型，[1,2000]；入参 ≤0 归一为 100）", example = "100")
	public Integer page_size;

	@Schema(description = "满足过滤条件的 SKU 行总数（整型）", example = "1")
	public Long total_count;

	@Schema(description = "SKU 行数组（item_id 降序；无结果为空数组）")
	public List<RowDoc> sku_list;

	/** SKU 行（3 固定键）。 */
	@Schema(name = "EcxProductSkuListRow")
	public static class RowDoc {

		@Schema(description = "商品名称（item_name，country_code 语言分表覆盖；库值 null 透传 null）", nullable = true, example = "Wire test goods")
		public String name;

		@Schema(description = "货品编码（items.item_bn 原值透传；库值 null 透传 null）", nullable = true, example = "SKU200")
		public String sku_id;

		@Schema(description = "是否平台自营商品（布尔：distributor_id 为 null 或 0 → true）", example = "true")
		public Boolean is_self;
	}
}