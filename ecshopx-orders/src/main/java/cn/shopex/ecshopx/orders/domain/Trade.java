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

/** 交易单表 */
@Data
@MpTable(value = "trade", comment = "交易单表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "ix_order_id", columns = {"order_id"}), @MpIndex(name = "idx_supplier_id", columns = {"supplier_id"}), @MpIndex(name = "ix_user_id", columns = {"user_id"}), @MpIndex(name = "ix_list", columns = {"company_id", "order_id"}), @MpIndex(name = "idx_merchant_id", columns = {"merchant_id"})})
public class Trade {

    /** 交易单号 */
    @MpId(value = "trade_id", type = IdType.INPUT, columnType = "string", length = 64, comment = "交易单号")
    private String tradeId;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", length = 64, nullable = true, comment = "订单号")
    private String orderId;

    /** 企业ID */
    @MpField(value = "company_id", columnType = "string", comment = "企业ID")
    private String companyId;

    /** 门店ID */
    @MpField(value = "shop_id", columnType = "string", nullable = true, comment = "门店ID")
    private String shopId;

    /** 店铺ID */
    @MpField(value = "distributor_id", columnType = "string", nullable = true, comment = "店铺ID")
    private String distributorId;

    /** 经销商ID */
    @MpField(value = "dealer_id", columnType = "string", nullable = true, comment = "经销商ID", defaultValue = "0")
    private String dealerId = "0";

    /** 交易单来源类型。可选值有 membercard-会员卡购买;normal-实体订单购买;servers-服务订单购买;normal_community-社区订单购买;diposit-预存款购买;order_pay-买单购买; */
    @MpField(value = "trade_source_type", columnType = "string", nullable = true, comment = "交易单来源类型。可选值有 membercard-会员卡购买;normal-实体订单购买;servers-服务订单购买;normal_community-社区订单购买;diposit-预存款购买;order_pay-买单购买;")
    private String tradeSourceType;

    /** 购买用户 */
    @MpField(value = "user_id", columnType = "string", comment = "购买用户")
    private String userId;

    /** 购买用户手机号 */
    @MpField(value = "mobile", columnType = "string", length = 255, nullable = true, comment = "购买用户手机号")
    private String mobile;

    /** 用户open_id */
    @MpField(value = "open_id", columnType = "string", nullable = true, comment = "用户open_id")
    private String openId;

    /** 优惠金额，优惠金额，优惠原因json结构 */
    @MpField(value = "discount_info", columnType = "text", nullable = true, comment = "优惠金额，优惠金额，优惠原因json结构")
    private String discountInfo;

    /** 商户号，微信支付 */
    @MpField(value = "mch_id", columnType = "string", nullable = true, comment = "商户号，微信支付")
    private String mchId;

    /** 应付总金额,以分为单位 */
    @MpField(value = "total_fee", columnType = "integer", comment = "应付总金额,以分为单位", defaultValue = "0")
    private Integer totalFee = 0;

    /** 订单优惠金额 */
    @MpField(value = "discount_fee", columnType = "integer", comment = "订单优惠金额", defaultValue = "0")
    private Integer discountFee = 0;

    /** 货币类型 */
    @MpField(value = "fee_type", columnType = "string", length = 16, comment = "货币类型")
    private String feeType;

    /** 支付金额 */
    @MpField(value = "pay_fee", columnType = "integer", comment = "支付金额", defaultValue = "0")
    private Integer payFee = 0;

    /** 每日交易序号 */
    @MpField(value = "trade_no", columnType = "string", comment = "每日交易序号", defaultValue = "0")
    private String tradeNo = "0";

    /** 交易状态。可选值有 SUCCESS—支付成功;REFUND—转入退款;NOTPAY—未支付;CLOSED—已关闭;REVOKED—已撤销;PAYERROR--支付失败(其他原因，如银行返回失败) */
    @MpField(value = "trade_state", columnType = "string", comment = "交易状态。可选值有 SUCCESS—支付成功;REFUND—转入退款;NOTPAY—未支付;CLOSED—已关闭;REVOKED—已撤销;PAYERROR--支付失败(其他原因，如银行返回失败)")
    private String tradeState;

    /** 支付方式。wxpay-微信支付;deposit-预存款支付;pos-刷卡;point-积分 */
    @MpField(value = "pay_type", columnType = "string", comment = "支付方式。wxpay-微信支付;deposit-预存款支付;pos-刷卡;point-积分")
    private String payType;

    /** adapay支付渠道 */
    @MpField(value = "pay_channel", columnType = "string", nullable = true, comment = "adapay支付渠道")
    private String payChannel = "";

    /** 支付订单号 */
    @MpField(value = "transaction_id", columnType = "string", nullable = true, comment = "支付订单号")
    private String transactionId;

    /** 公众号的appid */
    @MpField(value = "authorizer_appid", columnType = "string", length = 64, nullable = true, comment = "公众号的appid")
    private String authorizerAppid;

    /** 支付小程序的appid */
    @MpField(value = "wxa_appid", columnType = "string", length = 64, nullable = true, comment = "支付小程序的appid")
    private String wxaAppid;

    /** 付款银行 */
    @MpField(value = "bank_type", columnType = "string", nullable = true, comment = "付款银行")
    private String bankType;

    /** 交易商品简单描述 */
    @MpField(value = "body", columnType = "string", comment = "交易商品简单描述")
    private String body;

    /** 交易商品详情 */
    @MpField(value = "detail", columnType = "string", comment = "交易商品详情")
    private String detail;

    /** 交易起始时间 */
    @MpField(value = "time_start", columnType = "string", comment = "交易起始时间")
    private String timeStart;

    /** 交易结束时间 */
    @MpField(value = "time_expire", columnType = "string", nullable = true, comment = "交易结束时间")
    private String timeExpire;

    /** 分账对象信息列表，最多仅支持7个分账方，json 数组形式 */
    @MpField(value = "div_members", columnType = "string", length = 500, nullable = true, comment = "分账对象信息列表，最多仅支持7个分账方，json 数组形式")
    private String divMembers = "";

    /** 实际退款金额，以分为单位 */
    @MpField(value = "refunded_fee", columnType = "integer", nullable = true, comment = "实际退款金额，以分为单位", defaultValue = "0")
    private Integer refundedFee = 0;

    /** adapay手续费收取模式 */
    @MpField(value = "adapay_fee_mode", columnType = "string", nullable = true, comment = "adapay手续费收取模式")
    private String adapayFeeMode = "";

    /** 分账手续费，以分为单位 */
    @MpField(value = "adapay_fee", columnType = "integer", nullable = true, comment = "分账手续费，以分为单位", defaultValue = "0")
    private Integer adapayFee = 0;

    /** 分账状态。可选值有NOTDIV—未分账;DIVED-已分账; */
    @MpField(value = "adapay_div_status", columnType = "string", nullable = true, comment = "分账状态。可选值有NOTDIV—未分账;DIVED-已分账;")
    private String adapayDivStatus;

    /** 系统配置货币类型 */
    @MpField(value = "cur_fee_type", columnType = "string", length = 5, comment = "系统配置货币类型", defaultValue = "CNY")
    private String curFeeType = "CNY";

    /** 系统配置货币汇率 */
    @MpField(value = "cur_fee_rate", columnType = "float", precision = 15, scale = 4, comment = "系统配置货币汇率", defaultValue = "1")
    private Float curFeeRate = 1.0f;

    /** 系统配置货币符号 */
    @MpField(value = "cur_fee_symbol", columnType = "string", comment = "系统配置货币符号", defaultValue = "￥")
    private String curFeeSymbol = "￥";

    /** 系统货币支付金额 */
    @MpField(value = "cur_pay_fee", columnType = "integer", comment = "系统货币支付金额", defaultValue = "0")
    private Integer curPayFee = 0;

    /** 优惠券抵扣金额，以分为单位 */
    @MpField(value = "coupon_fee", columnType = "integer", comment = "优惠券抵扣金额，以分为单位", defaultValue = "0")
    private Integer couponFee = 0;

    /** 优惠券信息json结构 */
    @MpField(value = "coupon_info", columnType = "text", nullable = true, comment = "优惠券信息json结构")
    private String couponInfo;

    /** 统一下单原始请求json结构 */
    @MpField(value = "inital_request", columnType = "text", nullable = true, comment = "统一下单原始请求json结构")
    private String initalRequest;

    /** 支付结果通知json结构 */
    @MpField(value = "inital_response", columnType = "text", nullable = true, comment = "支付结果通知json结构")
    private String initalResponse;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;

    /** 是否分账 */
    @MpField(value = "is_settled", columnType = "boolean", comment = "是否分账", defaultValue = "False")
    private Boolean isSettled = false;

    /** 支付参数 */
    @MpField(value = "payment_params", columnType = "text", nullable = true, comment = "支付参数")
    private String paymentParams;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "integer", comment = "供应商id", defaultValue = "0")
    private Integer supplierId = 0;

    /** 斗拱支付的请求日期 */
    @MpField(value = "bspay_req_date", columnType = "string", nullable = true, comment = "斗拱支付的请求日期")
    private String bspayReqDate;

    /** 斗拱分账对象信息列表，最多仅支持7个分账方，json 数组形式 */
    @MpField(value = "bspay_div_members", columnType = "string", length = 500, nullable = true, comment = "斗拱分账对象信息列表，最多仅支持7个分账方，json 数组形式")
    private String bspayDivMembers = "";

    /** 斗拱分账状态。可选值有NOTDIV—未分账;DIVED-已分账; */
    @MpField(value = "bspay_div_status", columnType = "string", nullable = true, comment = "斗拱分账状态。可选值有NOTDIV—未分账;DIVED-已分账;")
    private String bspayDivStatus;

    /** 斗拱手续费收取模式 1:外扣 2:内扣 */
    @MpField(value = "bspay_fee_mode", columnType = "string", nullable = true, comment = "斗拱手续费收取模式 1:外扣 2:内扣")
    private String bspayFeeMode;

    /** 斗拱分账手续费，以分为单位 */
    @MpField(value = "bspay_fee", columnType = "integer", nullable = true, comment = "斗拱分账手续费，以分为单位", defaultValue = "0")
    private Integer bspayFee = 0;
}
