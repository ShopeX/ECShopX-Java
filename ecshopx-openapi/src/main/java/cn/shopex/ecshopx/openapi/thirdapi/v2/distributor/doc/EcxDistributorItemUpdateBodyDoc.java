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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；PATCH 请求可选 JSON body，字段来源
 * {@code OpenapiThirdApiV2DistributorItemUpdateParams#resolve} 读取的全部 key，
 * 落库语义见 {@code OpenapiThirdApiV2DistributorItemUpdateService}。
 */
@Schema(
		name = "EcxDistributorItemUpdateBody",
		description =
				"更新店铺商品参数（部分更新：四个可选字段键不存在则不改动；store/price 键存在但非数值静默忽略）。"
						+ " shop_code/item_code 走 mergeString（body 含键优先，null/空白不回退 query）；"
						+ " 其余走键存在语义（body 含键——含 null——或 query 提供即视为提交）。"
						+ " 无任何有效补丁字段时直接成功返回（不定位店铺/商品）。")
public class EcxDistributorItemUpdateBodyDoc {

	@Schema(
			description = "店铺号（shop_code 精确匹配）；必填非空白否则 E5001「店铺号参数有误」；查无 E5401「店铺找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "S001")
	public String shop_code;

	@Schema(
			description = "商品货号（items.item_bn 精确匹配）；必填非空白否则 E5001「商品货号参数错误」；查无 E5101「商品找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "A1")
	public String item_code;

	@Schema(
			description =
					"店铺商品是否上架；可选，键存在时仅 \"0\"/\"1\"（trim 后）合法，否则 E5001「店铺商品是否上架参数错误」；"
							+ "更新后按 SPU 组合同步 goods_can_sale",
			allowableValues = {"0", "1"},
			example = "1")
	public String is_can_sale;

	@Schema(
			description =
					"是否总部发货（总部库存）；可选，键存在时仅 \"0\"/\"1\" 合法，否则 E5001「店铺商品是否总部发货参数错误」；"
							+ "仅支持 SPU 级商品（SKU 行报 E5410「是否总部发货, 当前只支持更新spu级商品」）；"
							+ "更新后同步整个 SPU 组；生效值为 1 时禁止同时提交 store/price"
							+ "（E5410「商品总部发货无法更新库存和价格」）",
			allowableValues = {"0", "1"},
			example = "0")
	public String is_total_store;

	@Schema(
			description =
					"店铺独立库存；可选，数值字符串（BigDecimal 解析取整；非数值静默忽略）；"
							+ "仅 is_total_store 生效值为 0 时可提交；写入后覆盖 Redis item_store 并发布库存变更事件；"
							+ "新建店铺商品行未提交时默认取总部库存",
			example = "10")
	public String store;

	@Schema(
			description =
					"店铺独立价格（元）；可选，数值字符串（×100 HALF_UP 转分；非数值静默忽略）；"
							+ "仅 is_total_store 生效值为 0 时可提交；不得小于进行中拼团活动价"
							+ "（否则 E0001「拼团名称【…】的活动价格大于商品价格，请检查后再提交！」）；"
							+ "新建店铺商品行未提交时默认取总部价格",
			example = "99.00")
	public String price;
}