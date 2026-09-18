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

/** 订单主关联表 */
@Data
@MpTable(value = "orders_associations", comment = "订单主关联表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_order_type", columns = {"order_type"}), @MpIndex(name = "idx_order_class", columns = {"order_class"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_salesman_id", columns = {"salesman_id"})})
public class OrderAssociations {

    /** 订单号 */
    @MpId(value = "order_id", type = IdType.INPUT, columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 公众号的appid */
    @MpField(value = "authorizer_appid", columnType = "string", length = 64, nullable = true, comment = "公众号的appid")
    private String authorizerAppid;

    /** 小程序的appid */
    @MpField(value = "wxa_appid", columnType = "string", length = 64, nullable = true, comment = "小程序的appid")
    private String wxaAppid;

    /** 订单标题 */
    @MpField(value = "title", columnType = "string", nullable = true, comment = "订单标题")
    private String title;

    /** 订单金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "bigint", nullable = true, comment = "订单金额，以分为单位")
    private Long totalFee;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "店铺id")
    private Long shopId;

    /** 店铺名称 */
    @MpField(value = "store_name", columnType = "string", length = 100, nullable = true, comment = "店铺名称")
    private String storeName;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 推广员user_id */
    @MpField(value = "promoter_user_id", columnType = "bigint", nullable = true, comment = "推广员user_id")
    private Long promoterUserId;

    /** 推广员店铺id，实际为推广员的user_id */
    @MpField(value = "promoter_shop_id", columnType = "bigint", nullable = true, comment = "推广员店铺id，实际为推广员的user_id")
    private Long promoterShopId;

    /** 订单来源id */
    @MpField(value = "source_id", columnType = "bigint", nullable = true, comment = "订单来源id")
    private Long sourceId;

    /** 订单监控页面id */
    @MpField(value = "monitor_id", columnType = "bigint", nullable = true, comment = "订单监控页面id")
    private Long monitorId;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", length = 255, nullable = true, comment = "手机号")
    private String mobile;

    /** 订单种类。可选值有 normal:普通订单;groups:拼团订单;;community 社区活动订单;bargain:助力订单;seckill:秒杀订单;shopguide:导购订单 */
    @MpField(value = "order_class", columnType = "string", comment = "订单种类。可选值有 normal:普通订单;groups:拼团订单;;community 社区活动订单;bargain:助力订单;seckill:秒杀订单;shopguide:导购订单", defaultValue = "normal")
    private String orderClass = "normal";

    /** 订单类型。可选值有 service 服务业订单;bargain 砍价订单;distribution 分销订单;normal 普通实体订单 */
    @MpField(value = "order_type", columnType = "string", nullable = true, comment = "订单类型。可选值有 service 服务业订单;bargain 砍价订单;distribution 分销订单;normal 普通实体订单")
    private String orderType;

    /** 订单状态。可选值有 DONE—订单完成;PAYED-已支付;NOTPAY—未支付;CANCEL—已取消;WAIT_BUYER_CONFIRM-待用户收货 */
    @MpField(value = "order_status", columnType = "string", comment = "订单状态。可选值有 DONE—订单完成;PAYED-已支付;NOTPAY—未支付;CANCEL—已取消;WAIT_BUYER_CONFIRM-待用户收货")
    private String orderStatus;

    /** 是否是分销订单 */
    @MpField(value = "is_distribution", columnType = "boolean", comment = "是否是分销订单", defaultValue = "False")
    private Boolean isDistribution = false;

    /** 订单总分销金额，以分为单位 */
    @MpField(value = "total_rebate", columnType = "integer", comment = "订单总分销金额，以分为单位", defaultValue = "0")
    private Integer totalRebate = 0;

    /** 快递公司 */
    @MpField(value = "delivery_corp", columnType = "string", nullable = true, comment = "快递公司")
    private String deliveryCorp;

    /** 快递单号 */
    @MpField(value = "delivery_code", columnType = "string", nullable = true, comment = "快递单号")
    private String deliveryCode;

    /** 发货时间 */
    @MpField(value = "delivery_time", columnType = "integer", nullable = true, comment = "发货时间")
    private Integer deliveryTime;

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

    /** 发货状态。可选值有 DONE—已发货;PENDING—待发货;PARTAIL_DELIVERY-部分发货 */
    @MpField(value = "delivery_status", columnType = "string", comment = "发货状态。可选值有 DONE—已发货;PENDING—待发货;PARTAIL_DELIVERY-部分发货", defaultValue = "PENDING")
    private String deliveryStatus = "PENDING";

    /** 取消订单状态。可选值有 NO_APPLY_CANCEL 未申请;WAIT_PROCESS 等待审核;REFUND_PROCESS 退款处理;SUCCESS 取消成功;FAILS 取消失败 */
    @MpField(value = "cancel_status", columnType = "string", comment = "取消订单状态。可选值有 NO_APPLY_CANCEL 未申请;WAIT_PROCESS 等待审核;REFUND_PROCESS 退款处理;SUCCESS 取消成功;FAILS 取消失败", defaultValue = "NO_APPLY_CANCEL")
    private String cancelStatus = "NO_APPLY_CANCEL";

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单完成时间 */
    @MpField(value = "end_time", columnType = "bigint", nullable = true, comment = "订单完成时间")
    private Long endTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;

    /** 货币类型 */
    @MpField(value = "fee_type", columnType = "string", length = 5, comment = "货币类型", defaultValue = "CNY")
    private String feeType = "CNY";

    /** 货币汇率 */
    @MpField(value = "fee_rate", columnType = "float", precision = 15, scale = 4, comment = "货币汇率", defaultValue = "1")
    private Float feeRate = 1.0f;

    /** 货币符号 */
    @MpField(value = "fee_symbol", columnType = "string", comment = "货币符号", defaultValue = "￥")
    private String feeSymbol = "￥";

    /** 导购员ID */
    @MpField(value = "salesman_id", columnType = "bigint", nullable = true, comment = "导购员ID", defaultValue = "0")
    private Long salesmanId = 0L;
}
