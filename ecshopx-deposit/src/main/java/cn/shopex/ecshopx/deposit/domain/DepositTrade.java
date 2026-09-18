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

package cn.shopex.ecshopx.deposit.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 预存款交易记录表 */
@Data
@MpTable(value = "deposit_trade", comment = "预存款交易记录表")
public class DepositTrade {

    /** 储值流水号 */
    @MpId(value = "deposit_trade_id", type = IdType.INPUT, columnType = "string", length = 64, comment = "储值流水号")
    private String depositTradeId;

    /** 企业ID */
    @MpField(value = "company_id", columnType = "string", comment = "企业ID")
    private String companyId;

    /** 充值会员卡号 */
    @MpField(value = "member_card_code", columnType = "string", comment = "充值会员卡号")
    private String memberCardCode;

    /** 门店ID */
    @MpField(value = "shop_id", columnType = "string", nullable = true, comment = "门店ID")
    private String shopId;

    /** 门店名称 */
    @MpField(value = "shop_name", columnType = "string", nullable = true, comment = "门店名称")
    private String shopName;

    /** 购买用户 */
    @MpField(value = "user_id", columnType = "string", comment = "购买用户")
    private String userId;

    /** 购买用户手机号 */
    @MpField(value = "mobile", columnType = "string", comment = "购买用户手机号")
    private String mobile;

    /** 用户open_id */
    @MpField(value = "open_id", columnType = "string", nullable = true, comment = "用户open_id")
    private String openId;

    /** 充值金额/消费金额。 单位是分 */
    @MpField(value = "money", columnType = "string", comment = "充值金额/消费金额。 单位是分")
    private String money;

    /** 交易类型充值或消费。consume:消费，recharge:充值 */
    @MpField(value = "trade_type", columnType = "string", length = 16, comment = "交易类型充值或消费。consume:消费，recharge:充值")
    private String tradeType;

    /** 交易状态 */
    @MpField(value = "trade_status", columnType = "string", length = 16, comment = "交易状态")
    private String tradeStatus;

    /** 充值支付订单号 */
    @MpField(value = "transaction_id", columnType = "string", nullable = true, comment = "充值支付订单号")
    private String transactionId;

    /** 充值满足活动规则ID */
    @MpField(value = "recharge_rule_id", columnType = "string", nullable = true, comment = "充值满足活动规则ID")
    private String rechargeRuleId;

    /** 付款银行 */
    @MpField(value = "bank_type", columnType = "string", nullable = true, comment = "付款银行")
    private String bankType;

    /** 公众号的appid */
    @MpField(value = "authorizer_appid", columnType = "string", length = 64, nullable = true, comment = "公众号的appid")
    private String authorizerAppid;

    /** 充值支付方式 */
    @MpField(value = "pay_type", columnType = "string", length = 64, nullable = true, comment = "充值支付方式")
    private String payType;

    /** 支付小程序的appid */
    @MpField(value = "wxa_appid", columnType = "string", length = 64, nullable = true, comment = "支付小程序的appid")
    private String wxaAppid;

    /** 交易详情 */
    @MpField(value = "detail", columnType = "string", comment = "交易详情")
    private String detail;

    /** 交易起始时间 */
    @MpField(value = "time_start", columnType = "string", comment = "交易起始时间")
    private String timeStart;

    /** 交易结束时间 */
    @MpField(value = "time_expire", columnType = "string", nullable = true, comment = "交易结束时间")
    private String timeExpire;

    /** 货币类型 */
    @MpField(value = "fee_type", columnType = "string", length = 16, comment = "货币类型", defaultValue = "CNY")
    private String feeType;

    /** 系统配置货币类型 */
    @MpField(value = "cur_fee_type", columnType = "string", length = 5, comment = "系统配置货币类型", defaultValue = "CNY")
    private String curFeeType;

    /** 系统配置货币汇率 */
    @MpField(value = "cur_fee_rate", columnType = "float", precision = 15, scale = 4, comment = "系统配置货币汇率", defaultValue = "1")
    private Double curFeeRate;

    /** 系统配置货币符号 */
    @MpField(value = "cur_fee_symbol", columnType = "string", comment = "系统配置货币符号", defaultValue = "￥")
    private String curFeeSymbol;

    /** 系统货币支付金额 */
    @MpField(value = "cur_pay_fee", columnType = "string", comment = "系统货币支付金额")
    private String curPayFee;
}
