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

package cn.shopex.ecshopx.supplier.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 供应商订单
 *
 * <p>索引：idx_order_id（order_id）、idx_order_status（order_status）、idx_receiver_mobile（receiver_mobile）、idx_supplier_id（supplier_id）、idx_create_time（create_time）
 */
@Data
@MpTable(value = "supplier_order", comment = "供应商订单", indexes = {@MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_order_status", columns = {"order_status"}), @MpIndex(name = "idx_receiver_mobile", columns = {"receiver_mobile"}), @MpIndex(name = "idx_supplier_id", columns = {"supplier_id"}), @MpIndex(name = "idx_create_time", columns = {"create_time"})})
public class SupplierOrder {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 订单标题 */
    @MpField(value = "title", columnType = "string", nullable = true, comment = "订单标题")
    private String title;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    /** 商品成本价，以分为单位 */
    @MpField(value = "cost_fee", columnType = "integer", comment = "商品成本价，以分为单位")
    private Integer costFee = 0;

    /** 佣金(分) */
    @MpField(value = "commission_fee", columnType = "integer", comment = "佣金(分)", defaultValue = "0")
    private Integer commissionFee = 0;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 营销活动ID，团购ID，社区拼团ID，秒杀活动ID等 */
    @MpField(value = "act_id", columnType = "bigint", nullable = true, comment = "营销活动ID，团购ID，社区拼团ID，秒杀活动ID等")
    private Long actId;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", length = 255, nullable = true, comment = "手机号")
    private String mobile;

    /** 订单种类。可选值有 normal:普通订单;groups:拼团订单;;community 社区活动订单;bargain:助力订单;seckill:秒杀订单;shopguide:导购订单;pointsmall:积分商城;excard:兑换券 */
    @MpField(value = "order_class", columnType = "string", comment = "订单种类。可选值有 normal:普通订单;groups:拼团订单;;community 社区活动订单;bargain:助力订单;seckill:秒杀订单;shopguide:导购订单;pointsmall:积分商城;excard:兑换券", defaultValue = "normal")
    private String orderClass = "normal";

    /** 运费价格，以分为单位 */
    @MpField(value = "freight_fee", columnType = "integer", nullable = true, comment = "运费价格，以分为单位", defaultValue = "0")
    private Integer freightFee = 0;

    /** 运费类型-用于积分商城 cash:现金 point:积分 */
    @MpField(value = "freight_type", columnType = "string", comment = "运费类型-用于积分商城 cash:现金 point:积分", defaultValue = "cash")
    private String freightType = "cash";

    /** 商品金额，以分为单位 */
    @MpField(value = "item_fee", columnType = "string", comment = "商品金额，以分为单位")
    private String itemFee;

    /** 订单金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "string", comment = "订单金额，以分为单位")
    private String totalFee;

    /** 销售价总金额，以分为单位 */
    @MpField(value = "market_fee", columnType = "string", nullable = true, comment = "销售价总金额，以分为单位")
    private String marketFee;

    /** 分阶段付款已支付金额，以分为单位 */
    @MpField(value = "step_paid_fee", columnType = "integer", nullable = true, comment = "分阶段付款已支付金额，以分为单位", defaultValue = "0")
    private Integer stepPaidFee = 0;

    /** 订单总分销金额，以分为单位 */
    @MpField(value = "total_rebate", columnType = "integer", comment = "订单总分销金额，以分为单位", defaultValue = "0")
    private Integer totalRebate = 0;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 收货方式。可选值有 【logistics 物流】【ziti 店铺自提】【dada 达达同城配】【merchant 商家自配】 */
    @MpField(value = "receipt_type", columnType = "string", comment = "收货方式。可选值有 【logistics 物流】【ziti 店铺自提】【dada 达达同城配】【merchant 商家自配】", defaultValue = "logistics")
    private String receiptType = "logistics";

    /** 店铺自提码 */
    @MpField(value = "ziti_code", columnType = "bigint", comment = "店铺自提码", defaultValue = "0")
    private Long zitiCode = 0L;

    /**
     * 店铺自提状态。可选值有 PENDING:等待自提;DONE:自提完成;NOTZITI:自提完成; APPROVE:审核通过,药品自提需要审核
     */
    @MpField(value = "ziti_status", columnType = "string", nullable = true, comment = "店铺自提状态。可选值有 PENDING:等待自提;DONE:自提完成;NOTZITI:自提完成; APPROVE:审核通过,药品自提需要审核", defaultValue = "NOTZITI")
    private String zitiStatus = "NOTZITI";

    /** 订单状态。可选值有 DONE—订单完成;NOTPAY—未支付;PART_PAYMENT-部分付款;WAIT_GROUPS_SUCCESS-等待拼团成功;PAYED-已支付;CANCEL—已取消;WAIT_BUYER_CONFIRM-待用户收货 */
    @MpField(value = "order_status", columnType = "string", comment = "订单状态。可选值有 DONE—订单完成;NOTPAY—未支付;PART_PAYMENT-部分付款;WAIT_GROUPS_SUCCESS-等待拼团成功;PAYED-已支付;CANCEL—已取消;WAIT_BUYER_CONFIRM-待用户收货")
    private String orderStatus;

    /** 支付状态。可选值有 NOTPAY—未支付;PAYED-已支付;ADVANCE_PAY-预付款完成;TAIL_PAY-支付尾款中 */
    @MpField(value = "pay_status", columnType = "string", comment = "支付状态。可选值有 NOTPAY—未支付;PAYED-已支付;ADVANCE_PAY-预付款完成;TAIL_PAY-支付尾款中", defaultValue = "NOTPAY")
    private String payStatus = "NOTPAY";

    /** 订单来源。可选值有 member-用户自主下单;shop-商家代客下单 */
    @MpField(value = "order_source", columnType = "string", nullable = true, comment = "订单来源。可选值有 member-用户自主下单;shop-商家代客下单", defaultValue = "member")
    private String orderSource = "member";

    /** 订单类型。可选值有 normal:普通实体订单 */
    @MpField(value = "order_type", columnType = "string", comment = "订单类型。可选值有 normal:普通实体订单", defaultValue = "normal")
    private String orderType = "normal";

    /** 是否分销订单 */
    @MpField(value = "is_distribution", columnType = "boolean", comment = "是否分销订单", defaultValue = "False")
    private Boolean isDistribution = false;

    /** 订单来源id */
    @MpField(value = "source_id", columnType = "bigint", nullable = true, comment = "订单来源id")
    private Long sourceId;

    /** 快递公司 */
    @MpField(value = "delivery_corp", columnType = "string", nullable = true, comment = "快递公司")
    private String deliveryCorp;

    /** 快递代码来源 */
    @MpField(value = "delivery_corp_source", columnType = "string", nullable = true, comment = "快递代码来源")
    private String deliveryCorpSource;

    /** 快递单号 */
    @MpField(value = "delivery_code", columnType = "string", nullable = true, comment = "快递单号")
    private String deliveryCode;

    /** 快递发货凭证 */
    @MpField(value = "delivery_img", columnType = "string", nullable = true, comment = "快递发货凭证")
    private String deliveryImg;

    /** 发货时间 */
    @MpField(value = "delivery_time", columnType = "integer", nullable = true, comment = "发货时间")
    private Integer deliveryTime;

    /** 订单完成时间 */
    @MpField(value = "end_time", columnType = "bigint", nullable = true, comment = "订单完成时间")
    private Long endTime;

    /** 发货状态。可选值有 DONE—已发货;PENDING—待发货;PARTAIL-部分发货 */
    @MpField(value = "delivery_status", columnType = "string", comment = "发货状态。可选值有 DONE—已发货;PENDING—待发货;PARTAIL-部分发货", defaultValue = "PENDING")
    private String deliveryStatus = "PENDING";

    /** 取消订单状态。可选值有 NO_APPLY_CANCEL 未申请;WAIT_PROCESS 等待审核;REFUND_PROCESS 退款处理;SUCCESS 取消成功;FAILS 取消失败 */
    @MpField(value = "cancel_status", columnType = "string", comment = "取消订单状态。可选值有 NO_APPLY_CANCEL 未申请;WAIT_PROCESS 等待审核;REFUND_PROCESS 退款处理;SUCCESS 取消成功;FAILS 取消失败", defaultValue = "NO_APPLY_CANCEL")
    private String cancelStatus = "NO_APPLY_CANCEL";

    /** 收货人姓名 */
    @MpField(value = "receiver_name", columnType = "string", length = 500, nullable = true, comment = "收货人姓名")
    private String receiverName;

    /** 收货人手机号 */
    @MpField(value = "receiver_mobile", columnType = "string", length = 255, nullable = true, comment = "收货人手机号")
    private String receiverMobile;

    /** 收货人邮编 */
    @MpField(value = "receiver_zip", columnType = "string", nullable = true, comment = "收货人邮编")
    private String receiverZip;

    /** 收货人所在省份 */
    @MpField(value = "receiver_state", columnType = "string", nullable = true, comment = "收货人所在省份")
    private String receiverState;

    /** 收货人所在城市 */
    @MpField(value = "receiver_city", columnType = "string", nullable = true, comment = "收货人所在城市")
    private String receiverCity;

    /** 收货人所在地区 */
    @MpField(value = "receiver_district", columnType = "string", nullable = true, comment = "收货人所在地区")
    private String receiverDistrict;

    /** 收货人详细地址 */
    @MpField(value = "receiver_address", columnType = "text", nullable = true, comment = "收货人详细地址")
    private String receiverAddress;

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

    /** 优惠券使用详情 */
    @MpField(value = "coupon_discount_desc", columnType = "text", nullable = true, comment = "优惠券使用详情")
    private String couponDiscountDesc = "";

    /** 会员折扣使用详情 */
    @MpField(value = "member_discount_desc", columnType = "text", nullable = true, comment = "会员折扣使用详情")
    private String memberDiscountDesc = "";

    /** 货币类型 */
    @MpField(value = "fee_type", columnType = "string", length = 5, comment = "货币类型", defaultValue = "CNY")
    private String feeType = "CNY";

    /** 货币汇率 */
    @MpField(value = "fee_rate", columnType = "float", precision = 15, scale = 4, comment = "货币汇率", defaultValue = "1")
    private Float feeRate = 1.0f;

    /** 货币符号 */
    @MpField(value = "fee_symbol", columnType = "string", comment = "货币符号", defaultValue = "￥")
    private String feeSymbol = "￥";

    /** 商品消费总积分 */
    @MpField(value = "item_point", columnType = "integer", nullable = true, comment = "商品消费总积分", defaultValue = "0")
    private Integer itemPoint = 0;

    /** 消费积分 */
    @MpField(value = "point", columnType = "integer", nullable = true, comment = "消费积分", defaultValue = "0")
    private Integer point = 0;

    /** 支付方式 */
    @MpField(value = "pay_type", columnType = "string", nullable = true, comment = "支付方式")
    private String payType = "";

    /** adapay支付渠道 */
    @MpField(value = "pay_channel", columnType = "string", nullable = true, comment = "adapay支付渠道")
    private String payChannel = "";

    /** 订单备注 */
    @MpField(value = "remark", columnType = "string", nullable = true, comment = "订单备注")
    private String remark;

    /** 发票信息 */
    @MpField(value = "invoice", columnType = "json_array", nullable = true, comment = "发票信息")
    private String invoice;

    /** 发票号 */
    @MpField(value = "invoice_number", columnType = "string", nullable = true, comment = "发票号")
    private String invoiceNumber = "";

    /** 是否已开发票 */
    @MpField(value = "is_invoiced", columnType = "boolean", nullable = true, comment = "是否已开发票", defaultValue = "0")
    private Boolean isInvoiced = false;

    /** 是否分发积分0否 1是 */
    @MpField(value = "send_point", columnType = "integer", comment = "是否分发积分0否 1是", defaultValue = "0")
    private Integer sendPoint = 0;

    /** 订单类型，0普通订单,1跨境订单,....其他 */
    @MpField(value = "type", columnType = "integer", comment = "订单类型，0普通订单,1跨境订单,....其他", defaultValue = "0")
    private Integer type = 0;

    /** 积分抵扣金额，以分为单位 */
    @MpField(value = "point_fee", columnType = "integer", nullable = true, comment = "积分抵扣金额，以分为单位", defaultValue = "0")
    private Integer pointFee = 0;

    /** 积分抵扣使用的积分数 */
    @MpField(value = "point_use", columnType = "integer", nullable = true, comment = "积分抵扣使用的积分数", defaultValue = "0")
    private Integer pointUse = 0;

    /** 包装 */
    @MpField(value = "pack", columnType = "string", nullable = true, comment = "包装")
    private String pack;

    /** 操作者id */
    @MpField(value = "operator_id", columnType = "integer", nullable = true, comment = "操作者id", defaultValue = "0")
    private Integer operatorId = 0;

    /** 订单来源 pc,h5,wxapp,aliapp,unknow,dianwu */
    @MpField(value = "source_from", columnType = "string", length = 10, nullable = true, comment = "订单来源 pc,h5,wxapp,aliapp,unknow,dianwu")
    private String sourceFrom;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "integer", comment = "供应商id", defaultValue = "0")
    private Integer supplierId = 0;

    /** 是否分账(斗拱) */
    @MpField(value = "is_settled", columnType = "boolean", comment = "是否分账(斗拱)", defaultValue = "False")
    private Boolean isSettled = false;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;
}
