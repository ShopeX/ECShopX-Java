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

package cn.shopex.ecshopx.aftersales.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 退款表
 */
@Data
@MpTable(value = "aftersales_refund", comment = "退款表", indexes = {@MpIndex(name = "idx_aftersales_bn", columns = {"aftersales_bn"}), @MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_merchant_id", columns = {"merchant_id"}), @MpIndex(name = "idx_supplier_id", columns = {"supplier_id"})})
public class AftersalesRefund {

    /** 申请退款单号 */
    @MpId(value = "refund_bn", type = IdType.INPUT, columnType = "bigint", comment = "申请退款单号")
    private Long refundBn;

    /** 售后单号 */
    @MpField(value = "aftersales_bn", columnType = "bigint", nullable = true, comment = "售后单号")
    private Long aftersalesBn;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 支付单号 */
    @MpField(value = "trade_id", columnType = "string", length = 64, comment = "支付单号")
    private String tradeId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商id", defaultValue = "0")
    private Long supplierId = 0L;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /**
     * 退款类型。0 售后申请退款；1 取消订单退款；2 拒收订单退款。默认 0（与库内字符串存储一致）
     */
    @MpField(value = "refund_type", columnType = "string", comment = "退款类型", defaultValue = "0")
    private String refundType = "0";

    /**
     * 退款渠道。offline 线下退款；original 原路返回
     */
    @MpField(value = "refund_channel", columnType = "string", comment = "退款渠道")
    private String refundChannel;

    /**
     * 退款状态，列默认 READY。READY 未审核；AUDIT_SUCCESS 审核成功待退款；SUCCESS 退款成功；REFUSE 退款驳回；CANCEL 撤销退款；REFUNDCLOSE 退款关闭；PROCESSING 退款处理中或已发起退款等待到账；CHANGE 退款异常
     */
    @MpField(value = "refund_status", columnType = "string", comment = "退款状态", defaultValue = "READY")
    private String refundStatus = "READY";

    /** 应退金额，以分为单位，非积分支付 */
    @MpField(value = "refund_fee", columnType = "integer", comment = "应退金额，以分为单位，非积分支付")
    private Integer refundFee;

    /** 实退金额，以分为单位 */
    @MpField(value = "refunded_fee", columnType = "integer", comment = "实退金额，以分为单位", defaultValue = "0")
    private Integer refundedFee = 0;

    /** 应退积分，以分为单位 */
    @MpField(value = "refund_point", columnType = "integer", comment = "应退积分，以分为单位")
    private Integer refundPoint;

    /** 实退积分 */
    @MpField(value = "refunded_point", columnType = "integer", comment = "实退积分", defaultValue = "0")
    private Integer refundedPoint = 0;

    /** 退还(下单获得的)积分，以分为单位 */
    @MpField(value = "return_point", columnType = "integer", comment = "退还(下单获得的)积分，以分为单位")
    private Integer returnPoint = 0;

    /**
     * 是否退运费。0 不退运费；1 退运费。默认 0
     */
    @MpField(value = "return_freight", columnType = "integer", comment = "是否退运费", defaultValue = "0")
    private Integer returnFreight = 0;

    /** 支付方式（同对应支付单），可为空，默认空串 */
    @MpField(value = "pay_type", columnType = "string", nullable = true, comment = "支付方式，同对应支付单")
    private String payType = "";

    /** 货币类型，最长 16，可为空 */
    @MpField(value = "currency", columnType = "string", length = 16, nullable = true, comment = "货币类型")
    private String currency;

    /** 退款备注 */
    @MpField(value = "refunds_memo", columnType = "string", nullable = true, comment = "退款备注")
    private String refundsMemo;

    /** 退款成功时间（bigint 时间戳），可为空 */
    @MpField(value = "refund_success_time", columnType = "bigint", nullable = true, comment = "退款成功时间")
    private Long refundSuccessTime;

    /** 商户返回退款单号，最长 50，可为空 */
    @MpField(value = "refund_id", columnType = "string", length = 50, nullable = true, comment = "商户返回退款单号")
    private String refundId;

    /** 创建时间（整型时间戳） */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;

    /** 系统配置货币类型，最长 5，可为空，默认 CNY */
    @MpField(value = "cur_fee_type", columnType = "string", length = 5, nullable = true, comment = "系统配置货币类型", defaultValue = "CNY")
    private String curFeeType = "CNY";

    /** 系统配置货币汇率，精度约 15 位、小数 4 位，默认 1 */
    @MpField(value = "cur_fee_rate", columnType = "float", precision = 15, scale = 4, comment = "系统配置货币汇率", defaultValue = "1")
    private Double curFeeRate = 1.0;

    /** 系统配置货币符号，可为空，默认 ￥ */
    @MpField(value = "cur_fee_symbol", columnType = "string", nullable = true, comment = "系统配置货币符号", defaultValue = "￥")
    private String curFeeSymbol = "￥";

    /** 系统货币支付金额 */
    @MpField(value = "cur_pay_fee", columnType = "string", comment = "系统货币支付金额")
    private String curPayFee;

    /** hf_order_id */
    @MpField(value = "hf_order_id", columnType = "string", nullable = true, comment = "hf_order_id")
    private String hfOrderId;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;

    /** 退款运费，根据freight_type存储现金（分）或积分值 */
    @MpField(value = "freight", columnType = "integer", nullable = true, comment = "退款运费，根据freight_type存储现金（分）或积分值", defaultValue = "0")
    private Integer freight = 0;

    /** 运费类型-用于积分商城 cash:现金 point:积分 */
    @MpField(value = "freight_type", columnType = "string", length = 10, comment = "运费类型-用于积分商城 cash:现金 point:积分", defaultValue = "cash")
    private String freightType = "cash";
}
