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
 * 仅 OpenAPI 文档；{@code data.itemTreeLists} 单条商品主行（多规格时含 {@code spec_items}）。
 *
 * <p>字段来源：{@code GoodsItemsListRowMapper#toRow} 去掉
 * {@code DiscountCardKaquanDetailItemsService.ADMIN_LIST_ONLY_KEYS}，追加 use_limit；经
 * {@code DistributorItemsListSkuSpecApplier#apply}（nospec/item_spec/item_spec_desc）、
 * {@code ItemsSkuListAssembler#applyTypeLabels}、{@code ItemsMedicineService#applyMedicineDataToRows}、
 * {@code MultiSpecItemsTreeFormatter#formatItemsList}（按 default_item_id 合并 spec_items），
 * 最后 {@code applyPhpListWireScalars} 把 PDO_STRING_KEYS 列转成字符串（对齐 PHP PDO 线格式）。
 */
@Schema(
		name = "EcxDiscountcardInfoItemTreeRow",
		description =
				"商品 SKU 树主行；数值型列多为字符串（PHP PDO 线格式），空值保持 null。"
						+ " item_spec / item_spec_desc 为空时被移除；spec_items 仅多规格商品存在")
public class EcxDiscountcardInfoItemTreeRowDoc {

	@Schema(description = "商品/SKU ID（多规格主行为 default_item_id）", example = "1001")
	public Object item_id;

	@Schema(description = "货品 ID", example = "500")
	public Object goods_id;

	@Schema(description = "商品名称", nullable = true)
	public String item_name;

	@Schema(description = "售价（分，字符串）", example = "9900")
	public Object price;

	@Schema(description = "库存（字符串）", example = "10")
	public Object store;

	@Schema(description = "是否无规格：true 无规格；false 多规格（其行会挂到 spec_items）", example = "true")
	public Boolean nospec;

	@Schema(description = "主分类 ID 原始串", nullable = true)
	public String item_category;

	@Schema(description = "主分类 ID（由 item_category 字符串化）", example = "12")
	public Object item_main_cat_id;

	@Schema(description = "商品类型；库值空时为 services", example = "normal")
	public String item_type;

	@Schema(description = "消费类型", example = "every")
	public String consume_type;

	@Schema(description = "商品图片：JSON 数组或原样字符串；空/非法为 []")
	public Object pics;

	@ArraySchema(arraySchema = @Schema(description = "其他素材列表 [{name,url,size?}]；空/非法为 []"))
	public List<Object> other_product_materials;

	@ArraySchema(arraySchema = @Schema(description = "商品类型标签列表（items_rel_type）"))
	public List<TypeLabelDoc> type_labels;

	@Schema(description = "品牌 logo", nullable = true)
	public String brand_logo;

	@Schema(description = "品牌名称", nullable = true)
	public String goods_brand;

	@Schema(description = "供应商 ID（字符串）", example = "0")
	public Object supplier_id;

	@Schema(description = "所属店铺 ID（字符串）", example = "0")
	public Object distributor_id;

	@Schema(description = "品牌 ID（字符串）", example = "0")
	public Object brand_id;

	@Schema(description = "上架状态", example = "onsale")
	public String approve_status;

	@Schema(description = "审核状态；空/未设置为 null", nullable = true)
	public String audit_status;

	@Schema(description = "审核原因", nullable = true)
	public String audit_reason;

	@Schema(description = "货品编码", nullable = true)
	public String item_bn;

	@Schema(description = "条形码", nullable = true)
	public String barcode;

	@Schema(description = "简介", nullable = true)
	public String brief;

	@Schema(description = "是否默认 SKU（字符串 \"1\"/\"0\"）", example = "1")
	public Object is_default;

	@Schema(description = "默认 SKU ID（字符串；原值≤0 时回填为自身 item_id）", example = "1001")
	public Object default_item_id;

	@Schema(description = "供应商商品 ID（字符串）", example = "0")
	public Object supplier_item_id;

	@Schema(description = "市场价（分，字符串）", nullable = true)
	public Object market_price;

	@Schema(description = "成本价（分，字符串）", example = "0")
	public Object cost_price;

	@Schema(description = "货品编号", nullable = true)
	public String goods_bn;

	@Schema(description = "运费模板 ID（字符串）", nullable = true)
	public Object templates_id;

	@Schema(description = "排序值（字符串）", example = "0")
	public Object sort;

	@Schema(description = "重量（去尾零的十进制字符串）", example = "0")
	public Object weight;

	@Schema(description = "体积（字符串）", nullable = true)
	public Object volume;

	@Schema(description = "返利（字符串；null→空串）", example = "")
	public String rebate;

	@Schema(description = "返利配置 JSON；空/非法为 null", nullable = true)
	public Object rebate_conf;

	@Schema(description = "返利类型", example = "default")
	public String rebate_type;

	@Schema(description = "是否赠品（字符串 \"1\"/\"0\"）", example = "0")
	public Object is_gift;

	@Schema(description = "是否上架市场（字符串）", example = "1")
	public Object is_market;

	@ArraySchema(arraySchema = @Schema(description = "用于生成二维码的图片布尔数组；空/非法为 []"))
	public List<Object> pics_create_qrcode;

	@Schema(description = "视频类型", example = "local")
	public String video_type;

	@Schema(description = "视频（库原样）", nullable = true)
	public String videos;

	@Schema(description = "视频封面图", nullable = true)
	public String video_pic_url;

	@Schema(description = "是否启用协议（字符串 \"1\"/\"0\"）", example = "0")
	public Object enable_agreement;

	@Schema(description = "是否显示规格图（字符串 \"1\"/\"0\"）", example = "0")
	public Object is_show_specimg;

	@Schema(description = "有效期类型（商品维度）", nullable = true)
	public String date_type;

	@Schema(description = "有效期开始（Unix 秒，保持整型）", nullable = true)
	public Integer begin_date;

	@Schema(description = "有效期结束（Unix 秒，保持整型）", nullable = true)
	public Integer end_date;

	@Schema(description = "有效天数（保持整型）", nullable = true)
	public Integer fixed_term;

	@Schema(description = "是否积分商品（字符串 \"1\"/\"0\"）", nullable = true)
	public Object is_point;

	@Schema(description = "积分（字符串）", nullable = true)
	public Object point;

	@Schema(description = "商品来源", example = "mall")
	public String item_source;

	@Schema(description = "税率（字符串）", nullable = true)
	public Object tax_rate;

	@Schema(description = "跨境税率；null→空串", example = "")
	public String crossborder_tax_rate;

	@Schema(description = "原产国 ID（字符串）", example = "0")
	public Object origincountry_id;

	@Schema(description = "税收策略 ID（字符串）", example = "0")
	public Object taxstrategy_id;

	@Schema(description = "税号（字符串）", example = "0")
	public Object taxation_num;

	@Schema(description = "地区 ID", nullable = true)
	public String regions_id;

	@Schema(description = "地区", nullable = true)
	public String regions;

	@Schema(description = "特殊类型", example = "normal")
	public String special_type;

	@Schema(description = "商品所在省", nullable = true)
	public String item_address_province;

	@Schema(description = "商品所在市", nullable = true)
	public String item_address_city;

	@Schema(description = "分润类型（字符串）", example = "0")
	public Object profit_type;

	@Schema(description = "分润费用（字符串）", example = "0")
	public Object profit_fee;

	@Schema(description = "是否分润（字符串 \"1\"/\"0\"）", example = "0")
	public Object is_profit;

	@Schema(description = "是否组合商品（字符串 \"1\"/\"0\"）", example = "0")
	public Object is_package;

	@Schema(description = "类型（字符串）", example = "0")
	public Object type;

	@Schema(description = "起购数（字符串）", example = "0")
	public Object start_num;

	@Schema(description = "发货时间（字符串）", example = "0")
	public Object delivery_time;

	@Schema(description = "单位", nullable = true)
	public String item_unit;

	@Schema(description = "商品功能", nullable = true)
	public String goods_function;

	@Schema(description = "商品系列", nullable = true)
	public String goods_series;

	@Schema(description = "商品颜色", nullable = true)
	public String goods_color;

	@Schema(description = "销量（字符串）", nullable = true)
	public Object sales;

	@Schema(description = "创建时间（Unix 秒，字符串）", nullable = true)
	public Object created;

	@Schema(description = "更新时间（Unix 秒，字符串）", nullable = true)
	public Object updated;

	@Schema(description = "是否药品（字符串 \"1\"/\"0\"）", example = "0")
	public Object is_medicine;

	@Schema(description = "是否处方药（字符串 \"1\"/\"0\"）", example = "0")
	public Object is_prescription;

	@Schema(description = "TDK 内容", nullable = true)
	public String tdk_content;

	@Schema(description = "是否疫情商品（字符串）", example = "0")
	public Object is_epidemic;

	@Schema(description = "租户/公司 ID（字符串）", example = "100")
	public Object company_id;

	@Schema(description = "审核时间（Unix 秒，保持整型）", nullable = true)
	public Integer audit_date;

	@Schema(description = "是否淘宝商品（字符串）", example = "0")
	public Object is_taobao;

	@Schema(description = "本券对该商品的核销数量限制（rel_items.use_limit，整型）", example = "0")
	public Integer use_limit;

	@Schema(description = "item_id 的 camelCase 别名（同值）", example = "1001")
	public Object itemId;

	@Schema(description = "consume_type 的 camelCase 别名（null→空串）", example = "every")
	public String consumeType;

	@Schema(description = "item_name 的 camelCase 别名（null→空串）", example = "")
	public String itemName;

	@Schema(description = "item_bn 的 camelCase 别名（null→空串）", example = "")
	public String itemBn;

	@Schema(description = "company_id 的 camelCase 别名（null→空串）", example = "100")
	public Object companyId;

	@ArraySchema(
			arraySchema = @Schema(description = "规格列表（item_rel_attributes item_spec）；为空数组时该键被移除"))
	public List<ItemSpecDoc> item_spec;

	@Schema(description = "规格描述，形如「颜色:红,尺码:L」；为空时该键被移除", example = "颜色:红")
	public String item_spec_desc;

	@ArraySchema(arraySchema = @Schema(description = "多规格子 SKU 行（结构与主行一致）；仅多规格商品存在"))
	public List<EcxDiscountcardInfoItemTreeRowDoc> spec_items;

	@Schema(description = "药品类型（仅 is_medicine=1 且有药品记录时存在）", nullable = true)
	public Integer medicine_type;

	@Schema(description = "通用名（药品行）", nullable = true)
	public String common_name;

	@Schema(description = "剂型（药品行）", nullable = true)
	public String dosage;

	@Schema(description = "规格（药品行）", nullable = true)
	public String spec;

	@Schema(description = "包装规格（药品行）", nullable = true)
	public String packing_spec;

	@Schema(description = "生产厂家（药品行）", nullable = true)
	public String manufacturer;

	@Schema(description = "批准文号（药品行）", nullable = true)
	public String approval_number;

	@Schema(description = "单位（药品行）", nullable = true)
	public String unit;

	@Schema(description = "是否处方药（药品表值，药品行）", nullable = true)
	public Integer medicine_is_prescription;

	@Schema(description = "特殊通用名（药品行）", nullable = true)
	public String special_common_name;

	@Schema(description = "特殊规格（药品行）", nullable = true)
	public String special_spec;

	@Schema(description = "药品审核状态（药品行）", nullable = true)
	public Integer medicine_audit_status;

	@Schema(description = "药品审核原因（药品行）", nullable = true)
	public String medicine_audit_reason;

	@Schema(description = "药品商品类型（药品行）", nullable = true)
	public String medicine_item_type;

	@Schema(description = "用药提示（药品行）", nullable = true)
	public String use_tip;

	@Schema(description = "症状（药品行）", nullable = true)
	public String symptom;

	@Schema(description = "单次最大购买数（药品行）", nullable = true)
	public Integer max_num;

	@Schema(
			description = "药品摘要 {max_num?, is_prescription?}；仅药品行且至少一项非 null 时存在",
			example = "{\"max_num\":2,\"is_prescription\":0}")
	public MedicineDataDoc medicine_data;

	@Schema(name = "EcxDiscountcardInfoItemTypeLabel", description = "商品类型标签（camelCase 键）")
	public static class TypeLabelDoc {

		@Schema(description = "商品 ID（字符串）", example = "1001")
		public String itemId;

		@Schema(description = "标签 ID（字符串）", nullable = true)
		public String labelId;

		@Schema(description = "标签名称", nullable = true)
		public String labelName;

		@Schema(description = "标签价格", nullable = true)
		public Integer labelPrice;

		@Schema(description = "数量类型", nullable = true)
		public String numType;

		@Schema(description = "数量（字符串）", nullable = true)
		public String num;

		@Schema(description = "是否不限数量", nullable = true)
		public Integer isNotLimitNum;

		@Schema(description = "限时（字符串）", nullable = true)
		public String limitTime;

		@Schema(description = "租户 ID（字符串）", nullable = true)
		public String companyId;

		@Schema(description = "创建时间（Unix 秒）", nullable = true)
		public Integer created;

		@Schema(description = "更新时间（Unix 秒）", nullable = true)
		public Integer updated;
	}

	@Schema(name = "EcxDiscountcardInfoItemSpec", description = "商品规格项")
	public static class ItemSpecDoc {

		@Schema(description = "商品 ID", example = "1001")
		public Long item_id;

		@Schema(description = "规格（属性）ID", example = "7")
		public Long spec_id;

		@Schema(description = "规格值 ID", example = "21")
		public Long spec_value_id;

		@Schema(description = "规格名（属性名缺失时为 \"-\"）", example = "颜色")
		public String spec_name;

		@Schema(description = "自定义规格值名；未设置时 null", nullable = true)
		public String spec_custom_value_name;

		@Schema(description = "规格值展示名（自定义优先，否则属性值）", example = "红")
		public String spec_value_name;

		@ArraySchema(arraySchema = @Schema(description = "该规格值的商品图片列表"))
		public List<String> item_image_url;

		@Schema(description = "规格图 URL（商品图优先，否则属性值图）", example = "")
		public String spec_image_url;
	}

	@Schema(name = "EcxDiscountcardInfoMedicineData", description = "药品摘要")
	public static class MedicineDataDoc {

		@Schema(description = "单次最大购买数（非 null 才写键）", example = "2")
		public Integer max_num;

		@Schema(description = "是否处方药（药品表值优先，回退商品 is_prescription；非 null 才写键）", example = "0")
		public Object is_prescription;
	}
}