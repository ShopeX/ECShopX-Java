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

package cn.shopex.ecshopx.goods.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 商品表
 */
@Data
@MpTable(
		value = "items",
		comment = "商品表",
		indexes = {
			@MpIndex(name = "ix_company_id", columns = {"company_id"}),
			@MpIndex(name = "ix_default_item_id", columns = {"default_item_id"}),
			@MpIndex(name = "ix_supplier_item_id", columns = {"supplier_item_id"}),
			@MpIndex(name = "ix_item_category", columns = {"item_category"}),
			@MpIndex(name = "ix_item_bn", columns = {"item_bn"}),
			@MpIndex(name = "ix_goods_id", columns = {"goods_id"}),
			@MpIndex(name = "ix_is_default", columns = {"is_default"}),
			@MpIndex(name = "ix_is_medicine", columns = {"is_medicine"}),
			@MpIndex(name = "ix_is_prescription", columns = {"is_prescription"}),
			@MpIndex(
					name = "ix_default_item_list",
					columns = {"company_id", "default_item_id", "approve_status", "item_type", "item_category"})
		},
		uniqueIndexes = {
			@MpIndex(name = "uk_items_distributor_item_bn", columns = {"company_id", "distributor_id", "item_bn"})
		})
public class Items {

	/** 商品ID */
	@MpId(value = "item_id", type = IdType.AUTO, columnType = "bigint", comment = "商品ID")
	private Long itemId;

	/**
	 * 商品类型，services：服务商品，normal: 普通商品
	 */
	@MpField(value = "item_type", columnType = "string", length = 15, comment = "商品类型，services：服务商品，normal: 普通商品", defaultValue = "services")
	private String itemType = "services";

	/** 商品主类目 */
	@MpField(value = "item_category", columnType = "string", length = 15, nullable = true, comment = "商品主类目", defaultValue = "null")
	private String itemCategory;

	/**
	 * 核销类型，every：每个物料都要核销(例如3个物料要核销3次)，all：所有物料作为一个整体核销一次(例如3个物料只需要核销1次)
	 */
	@MpField(value = "consume_type", columnType = "string", length = 15, comment = "核销类型，every：每个物料都要核销(例如3个物料要核销3次)，all：所有物料作为一个整体核销一次(例如3个物料只需要核销1次)", defaultValue = "every")
	private String consumeType = "every";

	/** 商品名称 */
	@MpField(value = "item_name", columnType = "string", length = 255, comment = "商品名称")
	private String itemName;

	/** 商品编号 */
	@MpField(value = "item_bn", columnType = "string", length = 255, comment = "商品编号")
	private String itemBn;

	/** 商品条形码 */
	@MpField(value = "barcode", columnType = "text", comment = "商品条形码")
	private String barcode;

	/** 简洁的描述 */
	@MpField(value = "brief", columnType = "string", length = 255, comment = "简洁的描述")
	private String brief;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 销售价,单位为‘分’ */
	@MpField(value = "price", columnType = "integer", comment = "销售价,单位为‘分’")
	private Integer price;

	/** 成本价,单位为‘分’ */
	@MpField(value = "cost_price", columnType = "integer", nullable = true, comment = "成本价,单位为‘分’", defaultValue = "0")
	private Integer costPrice = 0;

	/** 商品计量单位 */
	@MpField(value = "item_unit", columnType = "string", nullable = true, comment = "商品计量单位")
	private String itemUnit;

	/** 商品特殊类型 drug 处方药 normal 普通商品 */
	@MpField(value = "special_type", columnType = "string", nullable = true, comment = "商品特殊类型 drug 处方药 normal 普通商品", defaultValue = "normal")
	private String specialType = "normal";

	/** 产地省 */
	@MpField(value = "item_address_province", columnType = "string", nullable = true, comment = "产地省")
	private String itemAddressProvince;

	/** 产地市 */
	@MpField(value = "item_address_city", columnType = "string", nullable = true, comment = "产地市")
	private String itemAddressCity;

	/** 产地地区id */
	@MpField(value = "regions_id", columnType = "string", nullable = true, comment = "产地地区id")
	private String regionsId;

	/** 产地地区 */
	@MpField(value = "regions", columnType = "text", nullable = true, comment = "产地地区")
	private String regions;

	/** 库存 */
	@MpField(value = "store", columnType = "integer", nullable = true, comment = "库存")
	private Integer store;

	/** 销量 */
	@MpField(value = "sales", columnType = "integer", nullable = true, comment = "销量", defaultValue = "0")
	private Integer sales;

	/** 分销配置 */
	@MpField(value = "rebate_conf", columnType = "json_array", nullable = true, comment = "分销配置")
	private String rebateConf;

	/** 推广商品 1已选择 0未选择 2申请加入 3拒绝 */
	@MpField(value = "rebate", columnType = "integer", nullable = true, comment = "推广商品 1已选择 0未选择 2申请加入 3拒绝")
	private Integer rebate;

	/** 分佣计算方式 */
	@MpField(value = "rebate_type", columnType = "string", nullable = true, comment = "分佣计算方式", defaultValue = "default")
	private String rebateType = "default";

	/**
	 * 商品状态 onsale 前台可销售，offline_sale前端不展示，instock 不可销售, only_show 前台仅展示
	 */
	@MpField(value = "approve_status", columnType = "string", comment = "商品状态 onsale 前台可销售，offline_sale前端不展示，instock 不可销售, only_show 前台仅展示")
	private String approveStatus = "onsale";

	/**
	 * 审核状态 submitting待提交 approved成功 processing审核中 rejected审核拒绝
	 */
	@MpField(value = "audit_status", columnType = "string", nullable = true, comment = "审核状态 submitting待提交 approved成功 processing审核中 rejected审核拒绝", defaultValue = "approved")
	private String auditStatus;

	/** 审核拒绝原因 */
	@MpField(value = "audit_reason", columnType = "text", nullable = true, comment = "审核拒绝原因")
	private String auditReason;

	/** 原价,单位为‘分’ */
	@MpField(value = "market_price", columnType = "integer", comment = "原价,单位为‘分’")
	private Integer marketPrice;

	/** 商品功能 */
	@MpField(value = "goods_function", columnType = "string", length = 255, nullable = true, comment = "商品功能")
	private String goodsFunction;

	/** 商品系列 */
	@MpField(value = "goods_series", columnType = "string", length = 255, nullable = true, comment = "商品系列")
	private String goodsSeries;

	/** 商品颜色 */
	@MpField(value = "goods_color", columnType = "string", length = 255, nullable = true, comment = "商品颜色")
	private String goodsColor;

	/** 商品品牌 */
	@MpField(value = "goods_brand", columnType = "string", length = 255, nullable = true, comment = "商品品牌")
	private String goodsBrand;

	/** 商品是否为默认商品 */
	@MpField(value = "is_default", columnType = "boolean", nullable = true, comment = "商品是否为默认商品", defaultValue = "True")
	private Boolean isDefault;

	/** 默认商品ID */
	@MpField(value = "default_item_id", columnType = "bigint", nullable = true, comment = "默认商品ID", defaultValue = "0")
	private Long defaultItemId = 0L;

	/** 产品ID */
	@MpField(value = "goods_id", columnType = "bigint", nullable = true, comment = "产品ID", defaultValue = "0")
	private Long goodsId = 0L;

	/** 商品是否为单规格 */
	@MpField(value = "nospec", columnType = "string", nullable = true, comment = "商品是否为单规格", defaultValue = "true")
	private String nospec = "true";

	/** 商品重量 */
	@MpField(value = "weight", columnType = "float", nullable = true, precision = 15, scale = 4, comment = "商品重量", defaultValue = "0")
	private Double weight = 0.0;

	/** 商品排序 */
	@MpField(value = "sort", columnType = "integer", comment = "商品排序", defaultValue = "0")
	private Integer sort = 0;

	/** 是否为疫情需要登记的商品  1:是 0:否 */
	@MpField(value = "is_epidemic", columnType = "integer", nullable = true, comment = "是否为疫情需要登记的商品  1:是 0:否", defaultValue = "0")
	private Integer isEpidemic = 0;

	/** 运费模板id */
	@MpField(value = "templates_id", columnType = "integer", nullable = true, comment = "运费模板id")
	private Integer templatesId;

	/** 图片 */
	@MpField(value = "pics", columnType = "json_array", comment = "图片")
	private String pics;

	/** 图片是否生成小程序码 */
	@MpField(value = "pics_create_qrcode", columnType = "json_array", nullable = true, comment = "图片是否生成小程序码")
	private String picsCreateQrcode;

	/** 视频类型 local:本地视频 tencent:腾讯视频 */
	@MpField(value = "video_type", columnType = "string", comment = "视频类型 local:本地视频 tencent:腾讯视频", defaultValue = "local")
	private String videoType = "local";

	/** 视频 */
	@MpField(value = "videos", columnType = "text", nullable = true, comment = "视频")
	private String videos;

	/** 视频封面图 */
	@MpField(value = "video_pic_url", columnType = "text", nullable = true, comment = "视频封面图")
	private String videoPicUrl;

	/** 图文详情 */
	@MpField(value = "intro", columnType = "text", nullable = true, comment = "图文详情")
	private String intro;

	/** 购买协议 */
	@MpField(value = "purchase_agreement", columnType = "text", nullable = true, comment = "购买协议")
	private String purchaseAgreement;

	/** 详情页是否显示规格图片 */
	@MpField(value = "is_show_specimg", columnType = "boolean", comment = "详情页是否显示规格图片", defaultValue = "False")
	private Boolean isShowSpecimg = false;

	/** 开启购买协议 */
	@MpField(value = "enable_agreement", columnType = "boolean", comment = "开启购买协议", defaultValue = "False")
	private Boolean enableAgreement = false;

	/**
	 * 有效期的类型, DATE_TYPE_FIX_TIME_RANGE:指定日期范围内, DATE_TYPE_FIX_TERM:固定天数后
	 */
	@MpField(value = "date_type", columnType = "string", nullable = true, comment = "有效期的类型, DATE_TYPE_FIX_TIME_RANGE:指定日期范围内, DATE_TYPE_FIX_TERM:固定天数后")
	private String dateType;

	/** 有效期开始时间 */
	@MpField(value = "begin_date", columnType = "integer", nullable = true, comment = "有效期开始时间")
	private Integer beginDate;

	/** 有效期结束时间 */
	@MpField(value = "end_date", columnType = "integer", nullable = true, comment = "有效期结束时间")
	private Integer endDate;

	/** 有效期的有效天数 */
	@MpField(value = "fixed_term", columnType = "integer", nullable = true, comment = "有效期的有效天数")
	private Integer fixedTerm;

	/** 品牌图片 */
	@MpField(value = "brand_logo", columnType = "string", length = 1024, nullable = true, comment = "品牌图片")
	private String brandLogo;

	/** 是否积分兑换 true可以 false不可以 */
	@MpField(value = "is_point", columnType = "boolean", nullable = true, comment = "是否积分兑换 true可以 false不可以", defaultValue = "False")
	private Boolean isPoint;

	/** 积分个数 */
	@MpField(value = "point", columnType = "integer", nullable = true, comment = "积分个数", defaultValue = "0")
	private Integer point = 0;

	/** 店铺id,为0时表示该商品为商城商品，否则为店铺自有商品 */
	@MpField(value = "distributor_id", columnType = "integer", comment = "店铺id,为0时表示该商品为商城商品，否则为店铺自有商品", defaultValue = "0")
	private Integer distributorId = 0;

	/** 商品体积 */
	@MpField(value = "volume", columnType = "float", nullable = true, precision = 15, scale = 4, comment = "商品体积")
	private Double volume;

	/** 商品来源:mall:主商城;distributor:店铺自有;openapi:开放接口; */
	@MpField(value = "item_source", columnType = "string", comment = "商品来源:mall:主商城;distributor:店铺自有;openapi:开放接口;", defaultValue = "mall")
	private String itemSource = "mall";

	/** 品牌id */
	@MpField(value = "brand_id", columnType = "integer", nullable = true, comment = "品牌id", defaultValue = "0")
	private Integer brandId = 0;

	/** 税率, 百分之～/100 */
	@MpField(value = "tax_rate", columnType = "integer", comment = "税率, 百分之～/100")
	private Integer taxRate;

	/** 跨境税率，百分比，小数点2位 */
	@MpField(value = "crossborder_tax_rate", columnType = "string", length = 10, comment = "跨境税率，百分比，小数点2位")
	private String crossborderTaxRate;

	/**
	 * 分润类型, 默认为0配置分润,1主类目分润,2商品指定分润(比例),3商品指定分润(金额)
	 */
	@MpField(value = "profit_type", columnType = "integer", nullable = true, comment = "分润类型, 默认为0配置分润,1主类目分润,2商品指定分润(比例),3商品指定分润(金额)", defaultValue = "0")
	private Integer profitType = 0;

	/** 产地国id */
	@MpField(value = "origincountry_id", columnType = "bigint", comment = "产地国id", defaultValue = "0")
	private Long origincountryId = 0L;

	/** 税费策略id */
	@MpField(value = "taxstrategy_id", columnType = "bigint", comment = "税费策略id", defaultValue = "0")
	private Long taxstrategyId = 0L;

	/** 计税单位份数 */
	@MpField(value = "taxation_num", columnType = "integer", comment = "计税单位份数", defaultValue = "0")
	private Integer taxationNum = 0;

	/** 分润金额,单位为分 冗余字段 */
	@MpField(value = "profit_fee", columnType = "integer", nullable = true, comment = "分润金额,单位为分 冗余字段", defaultValue = "0")
	private Integer profitFee = 0;

	/** 商品类型，0普通，1跨境商品，可扩展 */
	@MpField(value = "type", columnType = "integer", length = 4, comment = "商品类型，0普通，1跨境商品，可扩展", defaultValue = "0")
	private Integer type = 0;

	/** 是否支持分润 */
	@MpField(value = "is_profit", columnType = "boolean", nullable = true, comment = "是否支持分润", defaultValue = "False")
	private Boolean isProfit = false;

	/** 是否为药品，0否 1是 */
	@MpField(value = "is_medicine", columnType = "smallint", comment = "是否为药品，0否 1是", defaultValue = "0")
	private Integer isMedicine = 0;

	/** 是否为处方药，0否 1是 */
	@MpField(value = "is_prescription", columnType = "smallint", comment = "是否为处方药，0否 1是", defaultValue = "0")
	private Integer isPrescription = 0;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;

	/** 是否为赠品 */
	@MpField(value = "is_gift", columnType = "boolean", nullable = true, comment = "是否为赠品", defaultValue = "False")
	private Boolean isGift = false;

	/** 是否为打包产品 */
	@MpField(value = "is_package", columnType = "boolean", nullable = true, comment = "是否为打包产品", defaultValue = "False")
	private Boolean isPackage = false;

	/** tdk详情 */
	@MpField(value = "tdk_content", columnType = "text", nullable = true, comment = "tdk详情")
	private String tdkContent;

	/** 供应商id */
	@MpField(value = "supplier_id", columnType = "integer", comment = "供应商id", defaultValue = "0")
	private Integer supplierId = 0;

	/** 供应商商品id */
	@MpField(value = "supplier_item_id", columnType = "integer", comment = "供应商商品id", defaultValue = "0")
	private Integer supplierItemId = 0;

	/** 供应商控制商品是否可售，0不可售，1可售 */
	@MpField(value = "is_market", columnType = "integer", length = 4, comment = "供应商控制商品是否可售，0不可售，1可售", defaultValue = "1")
	private Integer isMarket = 1;

	/** 产品编号 */
	@MpField(value = "goods_bn", columnType = "string", length = 255, nullable = true, comment = "产品编号")
	private String goodsBn;

	/** 供应商货号 */
	@MpField(value = "supplier_goods_bn", columnType = "string", nullable = true, comment = "供应商货号")
	private String supplierGoodsBn;

	/** 商品审核时间 */
	@MpField(value = "audit_date", columnType = "integer", nullable = true, comment = "商品审核时间")
	private Integer auditDate;

	/** 起订量 */
	@MpField(value = "start_num", columnType = "integer", nullable = true, comment = "起订量", defaultValue = "0")
	private Integer startNum = 0;

	/** 发货时间，如2，表示2天发货 */
	@MpField(value = "delivery_time", columnType = "integer", nullable = true, comment = "发货时间，如2，表示2天发货", defaultValue = "0")
	private Integer deliveryTime = 0;

	/** 是否淘宝商品 */
	@MpField(value = "is_taobao", columnType = "integer", nullable = true, comment = "是否淘宝商品", defaultValue = "0")
	private Integer isTaobao = 0;

	/** 数据来源，映射列 data_source；supplier_goods 表示供应商货源主档（列可能不存在于部分库，默认 ORM 不选该列） */
	@MpField(value = "data_source", exist = false)
	private String dataSource;
}
