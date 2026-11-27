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
 * 仅 OpenAPI 文档；data 结构（4 固定键，键序 page/page_size/total_count/goods_list），来源
 * {@code OpenapiThirdApiV2ProductGoodsListService#executeOpenapiProductGoodsList}。
 * page/page_size 为生效入参回显（整型）；total_count 为满足过滤的 SKU 行总数；
 * goods_list 行 8 固定键；item_name 经 zh-CN 语言分表覆盖；
 * sku 由 items_rel_attributes.custom_attribute_value 拼接（该查询**全局 LIMIT 100 且无排序**——
 * 行多时部分商品 sku 可能为空，按现状记录）；无 ORDER BY，分页顺序不保证稳定。
 */
@Schema(name = "EcxProductGoodsListData")
public class EcxProductGoodsListDataDoc {

	@Schema(description = "生效页码回显（整型，默认 1）", example = "1")
	public Integer page;

	@Schema(description = "生效每页数量回显（整型，默认 100；≤0 表示全量返回）", example = "100")
	public Integer page_size;

	@Schema(description = "满足过滤条件的商品 SKU 行总数（整型）", example = "1")
	public Long total_count;

	@Schema(description = "商品行数组（无排序保证；无结果为空数组）")
	public List<RowDoc> goods_list;

	/** 商品行（8 固定键）。 */
	@Schema(name = "EcxProductGoodsListRow")
	public static class RowDoc {

		@Schema(description = "商品名称（zh-CN 语言分表覆盖；null 转空串）", example = "Wire test goods")
		public String item_name;

		@Schema(description = "销售价，单位分（整型，null/非数值转 0）", example = "10")
		public Integer item_price;

		@Schema(
				description = "首图 URL（pics JSON 数组第一个字符串元素；空/非法/非字符串首元素为空串；示例取 seed 真实图片 URL，仅作格式演示）",
				example =
						"https://b-img-cdn.yuanyuanke.cn/image/21/2021/05/21/f425f5ae2e6032eb6fded1015fc979e4xPndgZMDIyfeGTWsMJOTcmWvaPQjWPbL")
		public String pic;

		@Schema(
				description =
						"规格值拼接（items_rel_attributes.custom_attribute_value 以空格连接；"
								+ "该查询全局 LIMIT 100 无排序——超出部分商品本键为空串；无规格为空串）",
				example = "")
		public String sku;

		@Schema(description = "**实为货品 item_id**（整型 int 值；键名 goods_bn 为 PHP 历史契约，非商品编码）", example = "200")
		public Integer goods_bn;

		@Schema(description = "货品编码（items.item_bn，null 转空串）", example = "SKU200")
		public String item_bn;

		@Schema(
				description = "商品状态：onsale 前台可销售 / offline_sale 前端不展示 / instock 不可销售 / only_show 前台仅展示",
				example = "onsale")
		public String approve_status;

		@Schema(description = "是否平台自营商品（布尔：distributor_id 为 null 或 0 → true）", example = "true")
		public Boolean is_self;
	}
}