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

/** 商户支付交易 */
@Data
@MpTable(value = "orders_merchant_trade", comment = "企业付款账单")
public class MerchantPaymentTrade {

    /** 商家支付交易单号 */
    @MpId(value = "merchant_trade_id", type = IdType.INPUT, columnType = "string", length = 32, comment = "商家支付交易单号")
    private String merchantTradeId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 关联支付场景ID */
    @MpField(value = "rel_scene_id", columnType = "string", nullable = true, comment = "关联支付场景ID")
    private String relSceneId;

    /** 关联支付场景名称 */
    @MpField(value = "rel_scene_name", columnType = "string", nullable = true, comment = "关联支付场景名称")
    private String relSceneName;

    /** 商户APPId */
    @MpField(value = "mch_appid", columnType = "string", length = 32, nullable = true, comment = "商户APPId")
    private String mchAppid;

    /** 商户号 */
    @MpField(value = "mchid", columnType = "string", length = 32, nullable = true, comment = "商户号")
    private String mchid;

    /** 商户支付方式 */
    @MpField(value = "payment_action", columnType = "string", length = 32, comment = "商户支付方式")
    private String paymentAction;

    /** 是否强验用户姓名。可选值：NO_CHECK-不校验真实姓名；FORCE_CHECK-强校验真实姓名 */
    @MpField(value = "check_name", columnType = "string", length = 11, comment = "是否强验用户姓名")
    private String checkName;

    /** 支付手机号 */
    @MpField(value = "mobile", columnType = "string", length = 32, nullable = true, comment = "支付手机号")
    private String mobile;

    /** 收款用户姓名 */
    @MpField(value = "re_user_name", columnType = "string", nullable = true, comment = "收款用户姓名")
    private String reUserName;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 用户openId */
    @MpField(value = "open_id", columnType = "string", nullable = true, comment = "用户openId")
    private String openId;

    /** 付款金额(分) */
    @MpField(value = "amount", columnType = "bigint", comment = "付款金额(分)")
    private Long amount;

    /** 付款备注 */
    @MpField(value = "payment_desc", columnType = "string", comment = "付款备注")
    private String paymentDesc;

    /** ip地址 */
    @MpField(value = "spbill_create_ip", columnType = "string", comment = "ip地址")
    private String spbillCreateIp;

    /** 支付状态。可选值有 NOT_PAY:未付款;PAYING:付款中;SUCCESS:付款成功;FAIL:付款失败 */
    @MpField(value = "status", columnType = "string", comment = "支付状态。可选值有 NOT_PAY:未付款;PAYING:付款中;SUCCESS:付款成功;FAIL:付款失败")
    private String status;

    /** 微信支付订单号 */
    @MpField(value = "payment_no", columnType = "string", length = 64, nullable = true, comment = "微信支付订单号")
    private String paymentNo;

    /** 微信支付成功时间 */
    @MpField(value = "payment_time", columnType = "string", nullable = true, comment = "微信支付成功时间")
    private String paymentTime;

    /** 支付错误code码 */
    @MpField(value = "error_code", columnType = "string", nullable = true, comment = "支付错误code码")
    private String errorCode;

    /** 支付错误描述 */
    @MpField(value = "error_desc", columnType = "string", nullable = true, comment = "支付错误描述")
    private String errorDesc;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;

    /** 货币类型 */
    @MpField(value = "fee_type", columnType = "string", length = 5, comment = "货币类型", defaultValue = "CNY")
    private String feeType;

    /** 系统配置货币类型 */
    @MpField(value = "cur_fee_type", columnType = "string", length = 5, comment = "系统配置货币类型", defaultValue = "CNY")
    private String curFeeType;

    /** 系统配置货币汇率 */
    @MpField(value = "cur_fee_rate", columnType = "float", precision = 15, scale = 4, comment = "系统配置货币汇率", defaultValue = "1")
    private Float curFeeRate = 1.0f;

    /** 系统配置货币符号 */
    @MpField(value = "cur_fee_symbol", columnType = "string", comment = "系统配置货币符号", defaultValue = "￥")
    private String curFeeSymbol;

    /** 系统货币支付金额 */
    @MpField(value = "cur_pay_fee", columnType = "string", comment = "系统货币支付金额")
    private String curPayFee;

    /** 汇付请求订单号 */
    @MpField(value = "hf_order_id", columnType = "string", nullable = true, comment = "汇付请求订单号")
    private String hfOrderId;

    /** 汇付请求日期 */
    @MpField(value = "hf_order_date", columnType = "string", nullable = true, comment = "汇付请求日期")
    private String hfOrderDate;

    /** 汇付取现方式。可选值：T0-T0取现; T1-T1取现; D1-D1取现 */
    @MpField(value = "hf_cash_type", columnType = "string", nullable = true, comment = "汇付取现方式 T0：T0取现; T1：T1取现 D1：D1取现")
    private String hfCashType;

    /** 汇付商户客户号 */
    @MpField(value = "user_cust_id", columnType = "string", nullable = true, comment = "汇付商户客户号")
    private String userCustId;

    /** 汇付取现银行卡id */
    @MpField(value = "bind_card_id", columnType = "string", nullable = true, comment = "汇付取现银行卡id")
    private String bindCardId;
}
