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
 * 仅 OpenAPI 文档；POST 请求 JSON body，1 键与 query 同名双通道（{@code mergeString}：
 * body 含键优先——null/空白视为未传；**body 值可直接传 JSON 数组**，自动序列化为字符串）。
 * 解析/校验见 {@code OpenapiThirdApiV2ProductStockUpdateService#parseSkuList/validateSkuListElements}，
 * 总店库存覆盖见 {@code ItemStoreService#overwriteItemStore}。
 */
@Schema(
		name = "EcxProductStockUpdateBody",
		description =
				"库存同步参数（1 键，body 含键优先否则 query 同名键）。"
						+ " 按货号（item_bn）覆盖式更新总店库存；带 shop_code 的行进入店铺分支（当前实现恒失败，见元素说明）。")
public class EcxProductStockUpdateBodyDoc {

	@Schema(
			description =
					"库存更新列表；必填，JSON 数组（字符串或 body 内直接数组）。空/非法 JSON/非数组/元素非对象 →"
							+ " E5002「商品信息格式错误」（空数组 [] 合法——无操作，返回三个空数组）。"
							+ " 每元素读取 3 键：sku_id✱（货号，缺失/null/空白 → E5001「商品信息的货号必填」）、"
							+ " stock✱（覆盖库存，须 0..999999999 整数——整数值的 Number 或 ^-?\\d+$ 字符串，"
							+ "小数/非法/越界 → E5001「商品信息的库存为0-999999999的整数」）、"
							+ " shop_code（可选；PHP truthy 才进店铺分支——按租户查店铺，查无 → update_error「门店不存在」；"
							+ "**查到店铺时当前实现恒抛「请填写店铺ID」进 update_error，店铺库存实际不可经本接口更新**，按现状记录）。"
							+ " 货号匹配**不含 company_id 条件**（同 bn 多行后者覆盖，跨租户隐患按现状记录）；"
							+ " 全部货号都查不到 → E5101「商品找不到」；部分查不到 → 记入 not_find_sku 不报错",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "[{\"sku_id\":\"SKU200\",\"stock\":10}]")
	public List<SkuItemDoc> sku_list;

	/** sku_list 元素（3 读取键）。仅文档形状；运行时仍为 Map。 */
	@Schema(name = "EcxProductStockUpdateSkuItem")
	public static class SkuItemDoc {

		@Schema(description = "货号（items.item_bn）；必填非空白", requiredMode = Schema.RequiredMode.REQUIRED, example = "SKU200")
		public String sku_id;

		@Schema(
				description = "覆盖库存（0..999999999 整数；整数值 Number 或整数字符串）",
				requiredMode = Schema.RequiredMode.REQUIRED,
				example = "10")
		public Integer stock;

		@Schema(
				description =
						"店铺编码（可选；PHP truthy 进店铺分支：查无 → update_error 门店不存在；"
								+ "查到 → 当前实现恒 update_error 请填写店铺ID）",
				example = "SHOP001")
		public String shop_code;
	}
}