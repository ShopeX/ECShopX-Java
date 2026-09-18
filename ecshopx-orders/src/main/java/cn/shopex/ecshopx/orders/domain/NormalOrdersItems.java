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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 实体订单明细表 */
@Data
@MpTable(value = "orders_normal_orders_items", comment = "实体订单明细表", indexes = {@MpIndex(name = "idx_company_order_id", columns = {"company_id", "order_id"}), @MpIndex(name = "idx_order_id", columns = {"order_id"})})
public class NormalOrdersItems {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 产品id */
    @MpField(value = "goods_id", columnType = "bigint", comment = "产品id")
    private Long goodsId;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 商品编码 */
    @MpField(value = "item_bn", columnType = "string", nullable = true, comment = "商品编码")
    private String itemBn;

    /** 商品spu编码 */
    @MpField(value = "goods_bn", columnType = "string", nullable = true, comment = "商品spu编码")
    private String goodsBn;

    /** 商品成本价，以分为单位 */
    @MpField(value = "cost_fee", columnType = "integer", comment = "商品成本价，以分为单位")
    private Integer costFee = 0;

    /** 佣金(分) */
    @MpField(value = "commission_fee", columnType = "integer", comment = "佣金(分)", defaultValue = "0")
    private Integer commissionFee = 0;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id", defaultValue = "0")
    private Long shopId;

    /** 营销活动ID，团购ID，社区拼团ID，秒杀活动ID等 */
    @MpField(value = "act_id", columnType = "bigint", nullable = true, comment = "营销活动ID，团购ID，社区拼团ID，秒杀活动ID等")
    private Long actId;

    /** 是否是总部库存(true:总部库存，false:店铺库存) */
    @MpField(value = "is_total_store", columnType = "boolean", nullable = true, comment = "是否是总部库存(true:总部库存，false:店铺库存)", defaultValue = "True")
    private Boolean isTotalStore = true;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", nullable = true, comment = "商品名称")
    private String itemName;

    /** 商品计量单位 */
    @MpField(value = "item_unit", columnType = "string", nullable = true, comment = "商品计量单位")
    private String itemUnit;

    /** 商品图片 */
    @MpField(value = "pic", columnType = "string", nullable = true, comment = "商品图片")
    private String pic;

    /** 购买商品数量 */
    @MpField(value = "num", columnType = "integer", comment = "购买商品数量")
    private Integer num;

    /** 单价，以分为单位 */
    @MpField(value = "price", columnType = "integer", comment = "单价，以分为单位")
    private Integer price;

    /** 原价价，以分为单位 */
    @MpField(value = "cost_price", columnType = "integer", comment = "原价价，以分为单位", defaultValue = "0")
    private Integer costPrice = 0;

    /** 原价，以分为单位 */
    @MpField(value = "market_price", columnType = "integer", comment = "原价，以分为单位", defaultValue = "0")
    private Integer marketPrice = 0;

    /** 订单金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "integer", comment = "订单金额，以分为单位")
    private Integer totalFee;

    /** 单个分销金额，以分为单位 */
    @MpField(value = "rebate", columnType = "integer", comment = "单个分销金额，以分为单位", defaultValue = "0")
    private Integer rebate = 0;

    /** 总分销金额，以分为单位 */
    @MpField(value = "total_rebate", columnType = "integer", comment = "总分销金额，以分为单位", defaultValue = "0")
    private Integer totalRebate = 0;

    /** 运费模板id */
    @MpField(value = "templates_id", columnType = "integer", comment = "运费模板id", defaultValue = "0")
    private Integer templatesId = 0;

    /** 商品总金额，以分为单位 */
    @MpField(value = "item_fee", columnType = "integer", comment = "商品总金额，以分为单位")
    private Integer itemFee = 0;

    /** 会员折扣金额，以分为单位 */
    @MpField(value = "member_discount", columnType = "integer", comment = "会员折扣金额，以分为单位")
    private Integer memberDiscount = 0;

    /** 优惠券抵扣金额，以分为单位 */
    @MpField(value = "coupon_discount", columnType = "integer", comment = "优惠券抵扣金额，以分为单位")
    private Integer couponDiscount = 0;

    /** 订单优惠金额，以分为单位 */
    @MpField(value = "discount_fee", columnType = "integer", comment = "订单优惠金额，以分为单位", defaultValue = "0")
    private Integer discountFee = 0;

    /** 订单优惠详情 */
    @MpField(value = "discount_info", columnType = "text", nullable = true, comment = "订单优惠详情")
    private String discountInfo;

    /** 订单商品附加信息 */
    @MpField(value = "add_service_info", columnType = "text", nullable = true, comment = "订单商品附加信息")
    private String addServiceInfo;

    /** 订单商品类型,normal:正常商品，gift: 赠品, plus_buy: 加价购商品 */
    @MpField(value = "order_item_type", columnType = "string", comment = "订单商品类型,normal:正常商品，gift: 赠品, plus_buy: 加价购商品", defaultValue = "normal")
    private String orderItemType = "normal";

    /** 优惠券使用详情 */
    @MpField(value = "coupon_discount_desc", columnType = "text", nullable = true, comment = "优惠券使用详情")
    private String couponDiscountDesc = "";

    /** 会员折扣使用详情 */
    @MpField(value = "member_discount_desc", columnType = "text", nullable = true, comment = "会员折扣使用详情")
    private String memberDiscountDesc = "";

    /** 是否评价 */
    @MpField(value = "is_rate", columnType = "boolean", nullable = true, comment = "是否评价", defaultValue = "0")
    private Boolean isRate = false;

    /** 自动关闭售后时间 */
    @MpField(value = "auto_close_aftersales_time", columnType = "integer", nullable = true, comment = "自动关闭售后时间")
    private Integer autoCloseAftersalesTime;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;

    /** 快递公司 */
    @MpField(value = "delivery_corp", columnType = "string", nullable = true, comment = "快递公司")
    private String deliveryCorp;

    /** 快递单号 */
    @MpField(value = "delivery_code", columnType = "string", nullable = true, comment = "快递单号")
    private String deliveryCode;

    /** 快递发货凭证 */
    @MpField(value = "delivery_img", columnType = "string", nullable = true, comment = "快递发货凭证")
    private String deliveryImg;

    /** 发货时间 */
    @MpField(value = "delivery_time", columnType = "integer", nullable = true, comment = "发货时间")
    private Integer deliveryTime;

    /** 发货状态。可选值有 DONE—已发货;PENDING—待发货 */
    @MpField(value = "delivery_status", columnType = "string", comment = "发货状态。可选值有 DONE—已发货;PENDING—待发货", defaultValue = "PENDING")
    private String deliveryStatus = "PENDING";

    /** 售后状态。可选值有 WAIT_SELLER_AGREE 0 等待商家处理;WAIT_BUYER_RETURN_GOODS 1 商家接受申请，等待消费者回寄;WAIT_SELLER_CONFIRM_GOODS 2 消费者回寄，等待商家收货确认;SELLER_REFUSE_BUYER 3 售后驳回;SELLER_SEND_GOODS 4 卖家重新发货 换货完成;REFUND_SUCCESS 5 退款成功;REFUND_CLOSED 6 退款关闭;CLOSED 7 售后关闭 */
    @MpField(value = "aftersales_status", columnType = "string", nullable = true, comment = "售后状态。可选值有 WAIT_SELLER_AGREE 0 等待商家处理;WAIT_BUYER_RETURN_GOODS 1 商家接受申请，等待消费者回寄;WAIT_SELLER_CONFIRM_GOODS 2 消费者回寄，等待商家收货确认;SELLER_REFUSE_BUYER 3 售后驳回;SELLER_SEND_GOODS 4 卖家重新发货 换货完成;REFUND_SUCCESS 5 退款成功;REFUND_CLOSED 6 退款关闭;CLOSED 7 售后关闭")
    private String aftersalesStatus;

    /** 退款金额，以分为单位 */
    @MpField(value = "refunded_fee", columnType = "integer", comment = "退款金额，以分为单位", defaultValue = "0")
    private Integer refundedFee = 0;

    /** 货币类型 */
    @MpField(value = "fee_type", columnType = "string", length = 5, comment = "货币类型", defaultValue = "CNY")
    private String feeType = "CNY";

    /** 货币汇率 */
    @MpField(value = "fee_rate", columnType = "float", precision = 15, scale = 4, comment = "货币汇率", defaultValue = "1")
    private Float feeRate = 1.0f;

    /** 货币符号 */
    @MpField(value = "fee_symbol", columnType = "string", comment = "货币符号", defaultValue = "￥")
    private String feeSymbol = "￥";

    /** 商品积分 */
    @MpField(value = "item_point", columnType = "integer", nullable = true, comment = "商品积分", defaultValue = "0")
    private Integer itemPoint = 0;

    /** 商品总积分 */
    @MpField(value = "point", columnType = "integer", nullable = true, comment = "商品总积分", defaultValue = "0")
    private Integer point = 0;

    /** 商品规格描述 */
    @MpField(value = "item_spec_desc", columnType = "text", nullable = true, comment = "商品规格描述")
    private String itemSpecDesc;

    /** 商品体积 */
    @MpField(value = "volume", columnType = "integer", nullable = true, comment = "商品体积", defaultValue = "0")
    private Integer volume = 0;

    /** 商品重量 */
    @MpField(value = "weight", columnType = "float", nullable = true, precision = 15, scale = 4, comment = "商品重量", defaultValue = "0")
    private Float weight = 0.0f;

    /** 订单类型，0普通订单,1跨境订单,....其他 */
    @MpField(value = "type", columnType = "integer", comment = "订单类型，0普通订单,1跨境订单,....其他", defaultValue = "0")
    private Integer type = 0;

    /** 商品税率 */
    @MpField(value = "tax_rate", columnType = "string", length = 6, nullable = true, comment = "商品税率")
    private String taxRate;

    /** 商品跨境税费 */
    @MpField(value = "cross_border_tax", columnType = "integer", comment = "商品跨境税费", defaultValue = "0")
    private Integer crossBorderTax = 0;

    /** 产地国名称 */
    @MpField(value = "origincountry_name", columnType = "string", length = 50, nullable = true, comment = "产地国名称")
    private String origincountryName;

    /** 产地国国旗 */
    @MpField(value = "origincountry_img_url", columnType = "string", nullable = true, comment = "产地国国旗")
    private String origincountryImgUrl;

    /** 积分抵扣时分摊的积分的金额，以分为单位 */
    @MpField(value = "point_fee", columnType = "integer", nullable = true, comment = "积分抵扣时分摊的积分的金额，以分为单位", defaultValue = "0")
    private Integer pointFee = 0;

    /** 门店缺货商品总部快递发货 */
    @MpField(value = "is_logistics", columnType = "boolean", nullable = true, comment = "门店缺货商品总部快递发货", defaultValue = "False")
    private Boolean isLogistics = false;

    /** 积分抵扣时分摊的积分值 */
    @MpField(value = "share_points", columnType = "integer", nullable = true, comment = "积分抵扣时分摊的积分值", defaultValue = "0")
    private Integer sharePoints = 0;

    /** 积分抵扣时分摊的积分升值数 */
    @MpField(value = "up_share_points", columnType = "integer", nullable = true, comment = "积分抵扣时分摊的积分升值数", defaultValue = "0")
    private Integer shareUppoints = 0;

    /** 计税总价，以分为单位 */
    @MpField(value = "taxable_fee", columnType = "integer", comment = "计税总价，以分为单位", defaultValue = "0")
    private Integer taxableFee = 0;

    /** 发货单发货数量 */
    @MpField(value = "delivery_item_num", columnType = "integer", nullable = true, comment = "发货单发货数量")
    private Integer deliveryItemNum;

    /** 取消数量 */
    @MpField(value = "cancel_item_num", columnType = "integer", nullable = true, comment = "取消数量")
    private Integer cancelItemNum;

    /** 商品获取积分 */
    @MpField(value = "get_points", columnType = "integer", nullable = true, comment = "商品获取积分", defaultValue = "0")
    private Integer getPoints = 0;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "integer", comment = "供应商id", defaultValue = "0")
    private Integer supplierId = 0;

    /** 是否为处方药商品,0否 1是 */
    @MpField(value = "is_prescription", columnType = "integer", comment = "是否为处方药商品,0否 1是", defaultValue = "0")
    private Integer isPrescription = 0;

    /** 是否开票,0否 1已开票 2开票中 3红冲 */
    @MpField(value = "is_invoice", columnType = "integer", comment = "是否开票,0否 1已开票 2开票中 3红冲", defaultValue = "0")
    private Integer isInvoice = 0;
}
