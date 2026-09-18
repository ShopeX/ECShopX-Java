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

package cn.shopex.ecshopx.pointsmall.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 积分商品表 */
@Data
@MpTable(value = "pointsmall_items", comment = "积分商品表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_default_item_id", columns = {"default_item_id"}), @MpIndex(name = "ix_item_category", columns = {"item_category"}), @MpIndex(name = "ix_is_default", columns = {"is_default"}), @MpIndex(name = "ix_goods_id", columns = {"goods_id"}), @MpIndex(name = "ix_default_item_list", columns = {"company_id", "default_item_id", "approve_status", "item_type", "item_category"})})
public class PointsmallItems {

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
    @MpField(value = "barcode", columnType = "string", length = 255, comment = "商品条形码")
    private String barcode;

    /** 简洁的描述 */
    @MpField(value = "brief", columnType = "string", length = 255, comment = "简洁的描述")
    private String brief;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 价格,单位为‘分’ */
    @MpField(value = "price", columnType = "integer", comment = "价格,单位为‘分’")
    private Integer price;

    /** 价格,单位为‘分’ */
    @MpField(value = "cost_price", columnType = "integer", nullable = true, comment = "价格,单位为‘分’", defaultValue = "0")
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

    /** 库存 */
    @MpField(value = "store", columnType = "integer", nullable = true, comment = "库存")
    private Integer store;

    /** 销量 */
    @MpField(value = "sales", columnType = "integer", nullable = true, comment = "销量", defaultValue = "0")
    private Integer sales;

    /** 商品状态 onsale 前台可销售，offline_sale前端不展示，instock 不可销售 */
    @MpField(value = "approve_status", columnType = "string", comment = "商品状态 onsale 前台可销售，offline_sale前端不展示，instock 不可销售")
    private String approveStatus = "onsale";

    /** 审核状态 approved成功 processing审核中 rejected审核拒绝 */
    @MpField(value = "audit_status", columnType = "string", nullable = true, comment = "审核状态 approved成功 processing审核中 rejected审核拒绝", defaultValue = "approved")
    private String auditStatus = "approved";

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
    private Boolean isDefault = true;

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

    /** 运费模板id */
    @MpField(value = "templates_id", columnType = "integer", nullable = true, comment = "运费模板id")
    private Integer templatesId;

    /** 图片 */
    @MpField(value = "pics", columnType = "json_array", comment = "图片")
    private String pics;

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

    /** 积分兑换价格 */
    @MpField(value = "point", columnType = "integer", nullable = true, comment = "积分兑换价格", defaultValue = "0")
    private Integer point = 0;

    /** 支付方式 point积分支付 online在线支付 mix混合支付 */
    @MpField(value = "pay_class", columnType = "string", nullable = true, comment = "支付方式 point积分支付 online在线支付 mix混合支付", defaultValue = "point")
    private String payClass = "point";

    /** 商品体积 */
    @MpField(value = "volume", columnType = "float", nullable = true, precision = 15, scale = 4, comment = "商品体积")
    private Double volume;

    /** 品牌id */
    @MpField(value = "brand_id", columnType = "integer", nullable = true, comment = "品牌id", defaultValue = "0")
    private Integer brandId = 0;

    /** 税率, 百分之～/100 */
    @MpField(value = "tax_rate", columnType = "integer", comment = "税率, 百分之～/100")
    private Integer taxRate;

    /** 跨境税率，百分比，小数点2位 */
    @MpField(value = "crossborder_tax_rate", columnType = "string", length = 10, comment = "跨境税率，百分比，小数点2位")
    private String crossborderTaxRate;

    /** 产地国id */
    @MpField(value = "origincountry_id", columnType = "bigint", comment = "产地国id", defaultValue = "0")
    private Long origincountryId = 0L;

    /** 商品类型，0普通，1跨境商品，可扩展 */
    @MpField(value = "type", columnType = "integer", length = 4, comment = "商品类型，0普通，1跨境商品，可扩展", defaultValue = "0")
    private Integer type = 0;

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
}
