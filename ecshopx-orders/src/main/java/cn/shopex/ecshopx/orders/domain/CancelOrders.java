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

/** 已付钱订单取消退款记录表 */
@Data
@MpTable(value = "orders_cancel_orders", comment = "已付钱订单取消退款记录表", indexes = {@MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"})})
public class CancelOrders {

    /** 取消ID */
    @MpId(value = "cancel_id", type = IdType.AUTO, columnType = "bigint", comment = "取消ID")
    private Long cancelId;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商id", defaultValue = "0")
    private Long supplierId = 0L;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 订单类型。可选值有 service 服务业订单;bargain 砍价订单;distribution 分销订单;normal 普通实体订单 */
    @MpField(value = "order_type", columnType = "string", nullable = true, comment = "订单类型。可选值有 service 服务业订单;bargain 砍价订单;distribution 分销订单;normal 普通实体订单")
    private String orderType;

    /** 订单金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "bigint", comment = "订单金额，以分为单位")
    private Long totalFee;

    /** 处理进度。可选值有 0 待处理;1 已取消;2 处理中;3 已完成; 4 已驳回 */
    @MpField(value = "progress", columnType = "smallint", comment = "处理进度。可选值有 0 待处理;1 已取消;2 处理中;3 已完成; 4 已驳回", defaultValue = "0")
    private Integer progress = 0;

    /** 取消来源。可选值有 buyer 用户取消订单;shop 商家取消订单 */
    @MpField(value = "cancel_from", columnType = "string", comment = "取消来源。可选值有 buyer 用户取消订单;shop 商家取消订单", defaultValue = "buyer")
    private String cancelFrom = "buyer";

    /** 取消原因 */
    @MpField(value = "cancel_reason", columnType = "string", length = 300, nullable = true, comment = "取消原因")
    private String cancelReason;

    /** 商家拒绝理由 */
    @MpField(value = "shop_reject_reason", columnType = "string", length = 300, nullable = true, comment = "商家拒绝理由")
    private String shopRejectReason;

    /** 退款状态。可选值有 READY 待审核;AUDIT_SUCCESS 审核成功待退款;SUCCESS 退款成功;SHOP_CHECK_FAILS 商家审核不通过;CANCEL 撤销退款;PROCESSING 已发起退款等待到账;FAILS 退款失败; */
    @MpField(value = "refund_status", columnType = "string", length = 30, comment = "退款状态。可选值有 READY 待审核;AUDIT_SUCCESS 审核成功待退款;SUCCESS 退款成功;SHOP_CHECK_FAILS 商家审核不通过;CANCEL 撤销退款;PROCESSING 已发起退款等待到账;FAILS 退款失败;", defaultValue = "READY")
    private String refundStatus = "READY";

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

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

    /** 消费积分 */
    @MpField(value = "point", columnType = "integer", nullable = true, comment = "消费积分", defaultValue = "0")
    private Integer point = 0;

    /** 支付方式 */
    @MpField(value = "pay_type", columnType = "string", nullable = true, comment = "支付方式")
    private String payType = "";
}
