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

package cn.shopex.ecshopx.distribution.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.math.BigDecimal;
import lombok.Data;

/** 店铺表 */
@Data
@MpTable(value = "distribution_distributor", comment = "店铺表", indexes = {@MpIndex(name = "ix_is_distributor", columns = {"is_distributor"}), @MpIndex(name = "ix_shop_id", columns = {"shop_id"}), @MpIndex(name = "ix_company_id_shop_code", columns = {"company_id", "shop_code"}), @MpIndex(name = "ix_is_ziti", columns = {"is_ziti"}), @MpIndex(name = "idx_merchant_id", columns = {"merchant_id"})})
public class Distributor {

    @MpId(value = "distributor_id", type = IdType.AUTO, columnType = "bigint")
    private Long distributorId;

    /** 是否是主店铺 */
    @MpField(value = "is_distributor", columnType = "boolean", comment = "是否是主店铺", defaultValue = "True")
    private Boolean isDistributor = true;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺手机号 */
    @MpField(value = "mobile", columnType = "string", length = 255, comment = "店铺手机号")
    private String mobile;

    /** 店铺地址 */
    @MpField(value = "address", columnType = "string", nullable = true, comment = "店铺地址")
    private String address;

    /** 详细地址，门牌号 */
    @MpField(value = "house_number", columnType = "string", nullable = true, comment = "详细地址，门牌号")
    private String houseNumber;

    /** 店铺名称 */
    @MpField(value = "name", columnType = "string", comment = "店铺名称")
    private String name;

    /** 店铺名称首字母 */
    @MpField(value = "first_letter", columnType = "string", length = 5, comment = "店铺名称首字母")
    private String firstLetter = "";

    /** 自动同步总部商品 */
    @MpField(value = "auto_sync_goods", columnType = "boolean", comment = "自动同步总部商品", defaultValue = "False")
    private Boolean autoSyncGoods = false;

    /** 店铺logo */
    @MpField(value = "logo", columnType = "string", nullable = true, comment = "店铺logo")
    private String logo;

    /** 其他联系方式 */
    @MpField(value = "contract_phone", columnType = "string", length = 20, comment = "其他联系方式", defaultValue = "0")
    private String contractPhone = "0";

    /** 店铺banner */
    @MpField(value = "banner", columnType = "string", nullable = true, comment = "店铺banner")
    private String banner;

    /** 联系人名称 */
    @MpField(value = "contact", columnType = "string", length = 500, nullable = true, comment = "联系人名称")
    private String contact;

    /** 店铺是否有效 */
    @MpField(value = "is_valid", columnType = "string", comment = "店铺是否有效", defaultValue = "true")
    private String isValid = "true";

    /** 腾讯地图纬度 */
    @MpField(value = "lng", columnType = "string", nullable = true, comment = "腾讯地图纬度")
    private String lng;

    /** 腾讯地图经度 */
    @MpField(value = "lat", columnType = "string", nullable = true, comment = "腾讯地图经度")
    private String lat;

    @MpField(value = "province", columnType = "string", nullable = true)
    private String province;

    @MpField(value = "city", columnType = "string", nullable = true)
    private String city;

    /** 营业时间 */
    @MpField(value = "hour", columnType = "string", length = 150, nullable = true, comment = "营业时间")
    private String hour;

    @MpField(value = "area", columnType = "string", nullable = true)
    private String area;

    /** 国家行政区划编码组合，逗号隔开 */
    @MpField(value = "regions_id", columnType = "text", nullable = true, comment = "国家行政区划编码组合，逗号隔开")
    private String regionsId;

    /** 是否是中国国内门店 1:国内(包含港澳台),2:非国内 */
    @MpField(value = "is_domestic", columnType = "smallint", length = 1, nullable = true, comment = "是否是中国国内门店 1:国内(包含港澳台),2:非国内", defaultValue = "1")
    private Integer isDomestic = 1;

    /** 是否为直营店 1:直营店,2:非直营店 */
    @MpField(value = "is_direct_store", columnType = "smallint", length = 1, nullable = true, comment = "是否为直营店 1:直营店,2:非直营店", defaultValue = "1")
    private Integer isDirectStore = 1;

    @MpField(value = "child_count", columnType = "integer", nullable = true)
    private Integer childCount = 0;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    /** 门店id */
    @MpField(value = "is_default", columnType = "integer", nullable = true, comment = "门店id", defaultValue = "0")
    private Integer isDefault = 0;

    /** 是否审核店铺商品 */
    @MpField(value = "is_audit_goods", columnType = "boolean", nullable = true, comment = "是否审核店铺商品", defaultValue = "False")
    private Boolean isAuditGoods = false;

    /** 是否支持自提 */
    @MpField(value = "is_ziti", columnType = "boolean", nullable = true, comment = "是否支持自提", defaultValue = "False")
    private Boolean isZiti = false;

    /** 是否支持配送 */
    @MpField(value = "is_delivery", columnType = "boolean", nullable = true, comment = "是否支持配送", defaultValue = "True")
    private Boolean isDelivery = true;

    /** 是否支持自配送 */
    @MpField(value = "is_self_delivery", columnType = "boolean", nullable = true, comment = "是否支持自配送", defaultValue = "False")
    private Boolean isSelfDelivery = false;

    /** 是否开启业务员 */
    @MpField(value = "is_open_salesman", columnType = "boolean", nullable = true, comment = "是否开启业务员", defaultValue = "False")
    private Boolean isOpenSalesman = false;

    /** 是否展示导购 0:不展示 1:展示固定URL 2:展示归属导购 */
    @MpField(value = "show_salesperson", columnType = "smallint", comment = "是否展示导购 0:不展示 1:展示固定URL 2:展示归属导购", defaultValue = "1")
    private Integer showSalesperson = 1;

    /** 导购固定码URL */
    @MpField(value = "fixed_salesperson_qrcode_url", columnType = "string", length = 255, nullable = true, comment = "导购固定码URL")
    private String fixedSalespersonQrcodeUrl;

    /** 地区名称组合。json格式 */
    @MpField(value = "regions", columnType = "text", nullable = true, comment = "地区名称组合。json格式")
    private String regions;

    /** 入驻审核状态，0未审核，1已审核 */
    @MpField(value = "review_status", columnType = "boolean", nullable = true, comment = "入驻审核状态，0未审核，1已审核", defaultValue = "False")
    private Boolean reviewStatus = false;

    /** 店铺来源，1管理端添加，2小程序申请入驻，3外部开放接口添加 */
    @MpField(value = "source_from", columnType = "integer", nullable = true, comment = "店铺来源，1管理端添加，2小程序申请入驻，3外部开放接口添加", defaultValue = "1")
    private Integer sourceFrom = 1;

    /** 经销商ID */
    @MpField(value = "dealer_id", columnType = "integer", nullable = true, comment = "经销商ID", defaultValue = "0")
    private Integer dealerId = 0;

    /** 分账信息 */
    @MpField(value = "split_ledger_info", columnType = "string", nullable = true, comment = "分账信息")
    private String splitLedgerInfo;

    /** 斗拱分账信息 */
    @MpField(value = "bspay_split_ledger_info", columnType = "string", nullable = true, comment = "斗拱分账信息")
    private String bspaySplitLedgerInfo;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;

    /** 店铺号 */
    @MpField(value = "shop_code", columnType = "string", nullable = true, comment = "店铺号")
    private String shopCode;

    /** 是否是总店配置 */
    @MpField(value = "distributor_self", columnType = "integer", nullable = true, comment = "是否是总店配置", defaultValue = "0")
    private Integer distributorSelf = 0;

    /** 企业微信的部门ID */
    @MpField(value = "wechat_work_department_id", columnType = "integer", nullable = true, comment = "企业微信的部门ID", defaultValue = "0")
    private Integer wechatWorkDepartmentId = 0;

    /** 区域id */
    @MpField(value = "regionauth_id", columnType = "bigint", comment = "区域id", defaultValue = "0")
    private Long regionauthId = 0L;

    /** 是否开启分账 */
    @MpField(value = "is_open", columnType = "string", comment = "是否开启分账", defaultValue = "false")
    private String isOpen = "false";

    /** 平台服务费率 */
    @MpField(value = "rate", columnType = "integer", nullable = true, comment = "平台服务费率")
    private Integer rate;

    /** 是否开启同城配（不仅限于达达）,0:未开启,1:已开启 */
    @MpField(value = "is_dada", columnType = "boolean", comment = "是否开启同城配（不仅限于达达）,0:未开启,1:已开启", defaultValue = "0")
    private Boolean isDada = false;

    /**
     * 业务类型(食品小吃-1,饮料-2,鲜花绿植-3,文印票务-8,便利店-9,水果生鲜-13,同城电商-19,
     * 医药-20,蛋糕-21,酒品-24,小商品市场-25,服装-26,汽修零配-27,数码家电-28,小龙虾-29,个人-50,火锅-51,
     * 个护美妆-53、母婴-55,家居家纺-57,手机-59,家装-61,其他-5)
     */
    @MpField(value = "business", columnType = "smallint", nullable = true, comment = "业务类型(食品小吃-1,饮料-2,鲜花绿植-3,文印票务-8,便利店-9,水果生鲜-13,同城电商-19, 医药-20,蛋糕-21,酒品-24,小商品市场-25,服装-26,汽修零配-27,数码家电-28,小龙虾-29,个人-50,火锅-51,个护美妆-53、母婴-55,家居家纺-57,手机-59,家装-61,其他-5)")
    private Integer business;

    /** 该门店在达达是否已创建,0:未创建,1:已创建 */
    @MpField(value = "dada_shop_create", columnType = "boolean", comment = "该门店在达达是否已创建,0:未创建,1:已创建", defaultValue = "0")
    private Boolean dadaShopCreate = false;

    /** 该门店在闪送是否已创建,0:未创建,1:已创建 */
    @MpField(value = "shansong_shop_create", columnType = "boolean", comment = "该门店在闪送是否已创建,0:未创建,1:已创建", defaultValue = "0")
    private Boolean shansongShopCreate = false;

    /** 闪送店铺ID */
    @MpField(value = "shansong_store_id", columnType = "bigint", nullable = true, comment = "闪送店铺ID")
    private Long shansongStoreId;

    /** 店铺介绍 */
    @MpField(value = "introduce", columnType = "text", nullable = true, comment = "店铺介绍")
    private String introduce;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;

    /** 店铺类型:0:自营,1:加盟 */
    @MpField(value = "distribution_type", columnType = "smallint", comment = "店铺类型:0:自营,1:加盟", defaultValue = "0")
    private Integer distributionType = 0;

    /** 下单是否需要选择街道社区 */
    @MpField(value = "is_require_subdistrict", columnType = "boolean", nullable = true, comment = "下单是否需要选择街道社区", defaultValue = "False")
    private Boolean isRequireSubdistrict = false;

    /** 下单是否需要填写楼栋门牌号 */
    @MpField(value = "is_require_building", columnType = "boolean", nullable = true, comment = "下单是否需要填写楼栋门牌号", defaultValue = "False")
    private Boolean isRequireBuilding = false;

    /** 配送距离 */
    @MpField(value = "delivery_distance", columnType = "integer", comment = "配送距离", defaultValue = "0")
    private Integer deliveryDistance = 0;

    /** 本店订单到店售后 */
    @MpField(value = "offline_aftersales", columnType = "integer", comment = "本店订单到店售后", defaultValue = "0")
    private Integer offlineAftersales = 0;

    /** 退货到本店退货点 */
    @MpField(value = "offline_aftersales_self", columnType = "integer", comment = "退货到本店退货点", defaultValue = "0")
    private Integer offlineAftersalesSelf = 0;

    /** 本店订单到其他店铺售后 */
    @MpField(value = "offline_aftersales_distributor_id", columnType = "string", nullable = true, comment = "本店订单到其他店铺售后")
    private String offlineAftersalesDistributorId;

    /** 其他店铺订单到本店售后 */
    @MpField(value = "offline_aftersales_other", columnType = "integer", comment = "其他店铺订单到本店售后", defaultValue = "0")
    private Integer offlineAftersalesOther = 0;

    /** 自配送预计需要的时间（小时） */
    @MpField(value = "freight_time", columnType = "integer", nullable = true, comment = "自配送预计需要的时间（小时）", defaultValue = "2")
    private Integer freightTime = 2;

    /** 退款退货可退运费 1是 0否 */
    @MpField(value = "is_refund_freight", columnType = "integer", nullable = true, comment = "退款退货可退运费 1是 0否", defaultValue = "0")
    private Integer isRefundFreight = 0;

    /** 旺店通门店编号 */
    @MpField(value = "wdt_shop_no", columnType = "string", length = 30, nullable = true, comment = "旺店通门店编号")
    private String wdtShopNo;

    /** 旺店通门店ID */
    @MpField(value = "wdt_shop_id", columnType = "bigint", comment = "旺店通门店ID", defaultValue = "0")
    private Long wdtShopId = 0L;

    /** kuaizhen580门店ID */
    @MpField(value = "kuaizhen_store_id", columnType = "bigint", comment = "kuaizhen580门店ID", defaultValue = "0")
    private Long kuaizhenStoreId = 0L;

    /** 是否开启店铺隔离 */
    @MpField(value = "open_divided", columnType = "bigint", comment = "是否开启店铺隔离", defaultValue = "0")
    private Long openDivided = 0L;

    /** 聚水潭店铺编号 */
    @MpField(value = "jst_shop_id", columnType = "bigint", comment = "聚水潭店铺编号", defaultValue = "0")
    private Long jstShopId = 0L;

    /** 收款主体，0=平台，1=店铺 */
    @MpField(value = "payment_subject", columnType = "smallint", defaultValue = "0")
    private Integer paymentSubject = 0;

    /** 店铺分类id */
    @MpField(value = "distributor_category_id", columnType = "bigint", comment = "店铺分类id", defaultValue = "0")
    private Long distributorCategoryId = 0L;

    @MpField(exist = false)
    private String merchantName;

    /** Spherical distance in km from optional geo query; not a table column. */
    @MpField(exist = false)
    private BigDecimal distance;
}
