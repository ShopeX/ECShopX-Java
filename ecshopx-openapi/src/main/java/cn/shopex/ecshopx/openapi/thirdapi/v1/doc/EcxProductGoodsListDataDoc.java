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
 * {@code OpenapiProductGoodsListPortImpl#listGoods}：租户全部商品（无类型/状态过滤、无排序），
 * item_name zh-CN 分表覆盖；sku 由关联属性 custom_attribute_value 空格拼接（全局 LIMIT 100 关联行）。
 */
@Schema(name = "EcxProductGoodsListData", description = "商品分页（page/page_size 回显）")
public class EcxProductGoodsListDataDoc {

	@Schema(description = "当前页码（入参回显）", example = "1")
	public Integer page;

	@Schema(description = "每页条数（入参回显）", example = "100")
	public Integer page_size;

	@Schema(description = "租户商品总数", example = "1")
	public Long total_count;

	@ArraySchema(arraySchema = @Schema(description = "商品列表；无商品时为空数组"))
	public List<GoodsRowDoc> goods_list;

	@Schema(name = "EcxProductGoodsListRow", description = "单条商品（7 键）")
	public static class GoodsRowDoc {

		@Schema(description = "商品名称（zh-CN 分表覆盖）；null→空串", example = "示例商品")
		public String item_name;

		@Schema(description = "销售价（分，整型；null→0）", example = "9900")
		public Integer item_price;

		@Schema(description = "首图 URL（pics JSON 数组首个字符串元素；空/非法为空串）", example = "https://example.test/key.jpg")
		public String pic;

		@Schema(
				description = "规格串：该商品关联属性 custom_attribute_value 以空格拼接（关联行全局 LIMIT 100）；无关联为空串",
				example = "红 L")
		public String sku;

		@Schema(description = "货品编号（实为 item_id 的整型值，PHP 旧契约键名 goods_bn）", example = "1001")
		public Integer goods_bn;

		@Schema(description = "商品编码（items.item_bn）；null→空串", example = "A1")
		public String item_bn;

		@Schema(
				description = "商品状态：onsale 可销售；offline_sale 前端不展示；instock 不可销售；only_show 仅展示；null→空串",
				example = "onsale")
		public String approve_status;
	}
}