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
 * 仅 OpenAPI 文档；列表行（10 固定键），来源
 * {@code OpenapiThirdApiV2NormalItemsEntityListService#executeOpenapiGetEntityList}
 * 输出行构造（item_id 降序分页，每 SPU 默认 SKU 一行）。
 * item_id/price/store/distributor_id/updated 为 items 表整型字段；nospec 库内字符串转布尔。
 */
@Schema(name = "EcxItemsEntityListRow")
public class EcxItemsEntityListRowDoc {

	@Schema(description = "默认 SKU 货品 ID（整型，降序排序键）", example = "200")
	public Long item_id;

	@Schema(description = "货品编码（items.item_bn）", example = "SKU200")
	public String item_bn;

	@Schema(description = "商品名称（zh-CN 分表覆盖）", example = "Wire test goods")
	public String item_name;

	@Schema(description = "售价（分，整型，默认 SKU 值）", example = "10")
	public Integer price;

	@Schema(description = "库存（整型，默认 SKU 值）", example = "10")
	public Integer store;

	@Schema(
			description = "默认 SKU 上下架状态（onsale/instock/offline_sale/only_show）",
			example = "onsale")
	public String approve_status;

	@Schema(
			description = "是否单规格（布尔：库值 trim 后 \"true\"（忽略大小写）或 \"1\" 为 true，null/其他为 false）",
			example = "false")
	public Boolean nospec;

	@Schema(
			description =
					"销售分类名数组；每项为 \"[分类名]\" 方括号包裹（items_rel_cats 关联序，"
							+ "分类查无名为空串仍保留 \"[]\" 项）；无关联为空数组",
			example = "[\"[聚合测试类目]\"]")
	public List<String> category_name;

	@Schema(description = "是否自营（布尔：distributor_id 为 null 或 0 → true）", example = "true")
	public Boolean is_self;

	@Schema(description = "更新时间（items.updated Unix 秒，整型；示例为格式演示值）", example = "1735689600")
	public Integer update_time;
}