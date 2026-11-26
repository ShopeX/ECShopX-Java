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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，字段来源 {@code DistributorItemController#list}
 * 读取的全部 key（presentOptionalString / originalString），过滤语义见
 * {@code OpenapiThirdApiV2DistributorItemListService} 与 {@code OpenapiDistributorItemV2ListFilterBuilder}。
 */
@Schema(
		name = "EcxDistributorItemListBody",
		description =
				"店铺商品列表参数（body 与 query 同名 key 双通道：body 含键取原始值不 trim，否则用 query）。"
						+ " 固定过滤 SPU 主行（is_default=1）+ item_type=normal；店铺定位失败不报错、返回空列表。")
public class EcxDistributorItemListBodyDoc {

	@Schema(description = "页码；PHP 整型解析（含小数取整、非法按 0），缺省 1，小于 1 归一为 1", example = "1")
	public String page;

	@Schema(
			description =
					"每页条数；缺省 20；解析后 ≥500 报 E0001「每页的大小最大为500！」（500 本身也报错）；"
							+ "小于 20 一律归一为 20（PHP 旧契约）",
			example = "20")
	public String page_size;

	@Schema(
			description =
					"店铺号（shop_code 精确匹配 LIMIT 1）；键必须存在（body 含键——含 null 值——或 query 提供），"
							+ "否则 E5001「店铺号参数错误」；提供 distributor_id 时优先按 distributor_id 定位，本值被忽略；"
							+ "查无店铺返回空列表（不报错）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "S001")
	public String shop_code;

	@Schema(
			description = "店铺 ID；可选，非空时优先于 shop_code 按 distributor_id 精确匹配（非整数/查无返回空列表）",
			example = "1001")
	public String distributor_id;

	@Schema(description = "商品货号（items.item_bn 精确等值过滤）；可选，非空才生效", example = "A1")
	public String item_code;

	@Schema(
			description = "商品名称模糊过滤（contains 预查 SPU id 集合）；可选，非空才生效；查无匹配直接返回空列表",
			example = "示例商品")
	public String item_name;

	@Schema(
			description = "商品上下架状态精确过滤（approve_status：onsale/instock/offline_sale/only_show）；可选，非空才生效",
			example = "onsale")
	public String status;

	@Schema(
			description = "店铺可售过滤——当前实现读取但不参与过滤（无效果，PHP 迁移现状，如实记录）",
			example = "1")
	public String goods_can_sale;

	@Schema(
			description = "是否使用总部库存过滤——当前实现读取但不参与过滤（无效果，如实记录）",
			example = "1")
	public String is_total_store;
}