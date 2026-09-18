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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 服务订单表 */
@Data
@MpTable(value = "service_orders", comment = "服务订单表")
public class ServiceOrders {

    /** 订单号 */
    @MpId(value = "order_id", type = IdType.INPUT, columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 订单标题 */
    @MpField(value = "title", columnType = "string", nullable = true, comment = "订单标题")
    private String title;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "shop_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long shopId = 0L;

    /** 店铺名称 */
    @MpField(value = "store_name", columnType = "string", nullable = true, comment = "店铺名称")
    private String storeName;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 核销类型，every：每个物料都要核销(例如3个物料要核销3次)，all：所有物料作为一个整体核销一次(例如3个物料只需要核销1次) */
    @MpField(value = "consume_type", columnType = "string", length = 15, comment = "核销类型，every：每个物料都要核销(例如3个物料要核销3次)，all：所有物料作为一个整体核销一次(例如3个物料只需要核销1次)")
    private String consumeType;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 商品描述 */
    @MpField(value = "item_brief", columnType = "string", length = 255, nullable = true, comment = "商品描述")
    private String itemBrief;

    /** 商品图片 */
    @MpField(value = "item_pics", columnType = "string", nullable = true, comment = "商品图片")
    private String itemPics;

    /** 订单来源id */
    @MpField(value = "source_id", columnType = "bigint", nullable = true, comment = "订单来源id")
    private Long sourceId;

    /** 活动id */
    @MpField(value = "bargain_id", columnType = "bigint", nullable = true, comment = "活动id")
    private Long bargainId;

    /** 订单监控页面id */
    @MpField(value = "monitor_id", columnType = "bigint", nullable = true, comment = "订单监控页面id")
    private Long monitorId;

    /** 导购员ID */
    @MpField(value = "salesman_id", columnType = "bigint", nullable = true, comment = "导购员ID", defaultValue = "0")
    private Long salesmanId = 0L;

    /** 购买商品数量 */
    @MpField(value = "item_num", columnType = "string", comment = "购买商品数量")
    private String itemNum;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", nullable = true, comment = "手机号")
    private String mobile;

    /** 订单金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "string", comment = "订单金额，以分为单位")
    private String totalFee;

    /** 分阶段付款已支付金额，以分为单位 */
    @MpField(value = "step_paid_fee", columnType = "integer", nullable = true, comment = "分阶段付款已支付金额，以分为单位", defaultValue = "0")
    private Integer stepPaidFee = 0;

    /** 订单种类。可选值有 normal 普通订单;groups 拼团订单 */
    @MpField(value = "order_class", columnType = "string", comment = "订单种类。可选值有 normal 普通订单;groups 拼团订单", defaultValue = "normal")
    private String orderClass = "normal";

    /** 订单状态。可选值有 DONE—订单完成;NOTPAY—未支付;PART_PAYMENT-部分付款;WAIT_GROUPS_SUCCESS-等待拼团成功;PAYED-已支付;CANCEL—已取消 */
    @MpField(value = "order_status", columnType = "string", comment = "订单状态。可选值有 DONE—订单完成;NOTPAY—未支付;PART_PAYMENT-部分付款;WAIT_GROUPS_SUCCESS-等待拼团成功;PAYED-已支付;CANCEL—已取消")
    private String orderStatus;

    /** 订单来源。可选值有 member 用户自主下单;shop 商家代客下单 */
    @MpField(value = "order_source", columnType = "string", nullable = true, comment = "订单来源。可选值有 member 用户自主下单;shop 商家代客下单", defaultValue = "member")
    private String orderSource = "member";

    /** 操作员信息 */
    @MpField(value = "operator_desc", columnType = "string", length = 100, nullable = true, comment = "操作员信息")
    private String operatorDesc;

    /** 订单类型 */
    @MpField(value = "order_type", columnType = "string", nullable = true, comment = "订单类型")
    private String orderType;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;

    /** 订单自动取消时间 */
    @MpField(value = "auto_cancel_time", columnType = "string", comment = "订单自动取消时间")
    private String autoCancelTime;

    /** 有效期的类型, DATE_TYPE_FIX_TIME_RANGE: 指定日期范围内，DATE_TYPE_FIX_TERM:固定天数后 */
    @MpField(value = "date_type", columnType = "string", nullable = true, comment = "有效期的类型, DATE_TYPE_FIX_TIME_RANGE: 指定日期范围内，DATE_TYPE_FIX_TERM:固定天数后")
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

    /** 商品成本价，以分为单位 */
    @MpField(value = "cost_fee", columnType = "integer", comment = "商品成本价，以分为单位")
    private Integer costFee = 0;

    /** 商品总金额，以分为单位 */
    @MpField(value = "item_fee", columnType = "integer", comment = "商品总金额，以分为单位")
    private Integer itemFee = 0;

    /** 会员折扣金额，以分为单位 */
    @MpField(value = "member_discount", columnType = "integer", comment = "会员折扣金额，以分为单位")
    private Integer memberDiscount = 0;

    /** 优惠券抵扣金额，以分为单位 */
    @MpField(value = "coupon_discount", columnType = "integer", comment = "优惠券抵扣金额，以分为单位")
    private Integer couponDiscount = 0;

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
}
