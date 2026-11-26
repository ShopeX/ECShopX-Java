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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body。字段全集为
 * {@code OpenapiThirdApiV2NormalItemsEntityAddParams#WHITELIST} 的 24 键
 *（body 含键取 body 值，否则取 query 同名值；白名单外键忽略），
 * 校验见 {@code OpenapiThirdApiV2NormalItemsEntityAddService}，
 * 落库映射见 {@code PlatformItemsAddService}（commonParams/mapToItems/applyRelData）。
 */
@Schema(
		name = "EcxItemEntityAddBody",
		description =
				"新增平台商品参数（24 键白名单，body 含键优先、否则读 query 同名键；白名单外键忽略）。"
						+ " 服务端固定 item_type=normal、special_type=normal、item_source=openapi，剔除 item_id（恒新建）。"
						+ " 金额字段单位为分（整数解析）；重量/体积数值解析非法按 0。")
public class EcxItemEntityAddBodyDoc {

	@Schema(
			description = "管理（主）类目 ID；必填非空否则 E5001「管理分类必填」；写入 items.item_category 列",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "77")
	public String item_main_cat_id;

	@Schema(description = "商品名称；必填非空否则 E5001「商品名称必填」", requiredMode = Schema.RequiredMode.REQUIRED,
			example = "Wire test goods")
	public String item_name;

	@ArraySchema(
			arraySchema = @Schema(
					description = "商品图片 URL 数组（JSON 数组字符串或数组）；必填非空否则 E5001「请上传商品图片」；"
							+ "非数组/空数组/含对象或数组元素报 E5100「商品图片格式错误」",
					requiredMode = Schema.RequiredMode.REQUIRED),
			schema = @Schema(type = "string", example = "https://example.test/key.jpg"))
	public List<String> pics;

	@Schema(description = "运费模板 ID；必填非空否则 E5001「运费模板必填」", requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String templates_id;

	@Schema(
			description = "品牌 ID；必填非空否则 E5001「品牌必填」；>0 时挂默认 SKU 的品牌关联（item_rel_attributes）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "3")
	public String brand_id;

	@ArraySchema(
			arraySchema = @Schema(
					description = "销售分类 ID 数组（JSON 数组字符串或数组）；必填非空否则 E5001「商品分类ID必填」；"
							+ "格式非法 E5100「商品分类格式错误」；仅正数 ID 生效，全非正数 E5100「请选择销售分类」；"
							+ "挂默认 SKU 销售分类关联（items_rel_cats）",
					requiredMode = Schema.RequiredMode.REQUIRED),
			schema = @Schema(type = "string", example = "1"))
	public List<String> item_category;

	@Schema(description = "商品货号（items.item_bn）；可选", example = "SKU200")
	public String item_bn;

	@Schema(description = "商品简介；可选", example = "")
	public String brief;

	@Schema(description = "排序值；可选，缺省 1（整数解析）", example = "1")
	public String sort;

	@Schema(description = "计量单位；可选，缺省空串", example = "")
	public String item_unit;

	@Schema(description = "是否赠品；可选，1/true 为真；赠品触发进行中活动守卫", example = "0")
	public String is_gift;

	@Schema(description = "商品详情 HTML；可选（data:image 内联图会被替换上传为存储 URL）", example = "")
	public String intro;

	@Schema(
			description = "是否多规格：缺省 true 单规格；false/\"0\" 走多规格（spec_items 必填非空）",
			example = "false")
	public String nospec;

	@Schema(description = "是否展示规格图；可选，1/true 为真", example = "0")
	public String is_show_specimg;

	@Schema(
			description = "上下架状态：onsale 上架 / instock 下架 / offline_sale 前端不展示 / only_show 仅展示；可选",
			allowableValues = {"onsale", "instock", "offline_sale", "only_show"},
			example = "onsale")
	public String approve_status;

	@Schema(description = "重量；可选，数值解析非法按 0", example = "1")
	public String weight;

	@Schema(description = "体积；可选，数值解析非法按 0")
	public String volume;

	@Schema(description = "售价（分）；可选，整数解析（非法数字抛异常→E5100 透传）；不得小于进行中拼团活动价", example = "10")
	public String price;

	@Schema(description = "市场价（分）；可选，缺省 0")
	public String market_price;

	@Schema(description = "成本价（分）；可选，缺省 0")
	public String cost_price;

	@Schema(description = "条形码；可选", example = "B200")
	public String barcode;

	@Schema(description = "库存；可选，整数解析", example = "10")
	public String store;

	@ArraySchema(
			arraySchema = @Schema(description = "商品参数关联数组；可选，逐行挂默认 SKU（attribute_type=item_params）"),
			schema = @Schema(implementation = EcxItemEntityAddItemParamDoc.class))
	public List<EcxItemEntityAddItemParamDoc> item_params;

	@ArraySchema(
			arraySchema = @Schema(description = "规格图数组；可选（多规格用），缺键/解析失败静默忽略"),
			schema = @Schema(implementation = EcxItemEntityAddSpecImageDoc.class))
	public List<EcxItemEntityAddSpecImageDoc> spec_images;

	@ArraySchema(
			arraySchema = @Schema(
					description = "多规格 SKU 行数组（JSON 数组字符串或数组）；nospec=false/0 时必填非空，"
							+ "否则 E5100「请填写正确的规格数据」；行内键覆盖 SPU 公共字段"),
			schema = @Schema(implementation = EcxItemEntityAddSpecItemDoc.class))
	public List<EcxItemEntityAddSpecItemDoc> spec_items;
}