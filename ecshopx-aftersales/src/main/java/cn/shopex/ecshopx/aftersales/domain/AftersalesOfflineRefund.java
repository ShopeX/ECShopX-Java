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
 * 线下转账退款
 */
@Data
@MpTable(value = "aftersales_offline_refund", comment = "线下转账退款", indexes = {@MpIndex(name = "idx_refund_bn", columns = {"refund_bn"}), @MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class AftersalesOfflineRefund {

    /** 自增ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "自增ID")
    private Long id;

    /** 申请退款单号 */
    @MpField(value = "refund_bn", columnType = "bigint", comment = "申请退款单号")
    private Long refundBn;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 应退金额，以分为单位，非积分支付 */
    @MpField(value = "refund_fee", columnType = "integer", comment = "应退金额，以分为单位，非积分支付")
    private Integer refundFee;

    /** 收款账户名称，最长 50 */
    @MpField(value = "bank_account_name", columnType = "string", length = 50, comment = "收款账户名称")
    private String bankAccountName;

    /** 收款银行账号，最长 30 */
    @MpField(value = "bank_account_no", columnType = "string", length = 30, comment = "收款银行账号")
    private String bankAccountNo;

    /** 收款开户银行，最长 100 */
    @MpField(value = "bank_name", columnType = "string", length = 100, comment = "收款开户银行")
    private String bankName;

    /** 退款账户名，最长 100，可为空，默认空串 */
    @MpField(value = "refund_account_name", columnType = "string", length = 100, nullable = true, comment = "退款账户名")
    private String refundAccountName = "";

    /** 退款银行，最长 100，可为空，默认空串 */
    @MpField(value = "refund_account_bank", columnType = "string", length = 100, nullable = true, comment = "退款银行")
    private String refundAccountBank = "";

    /** 退款账号，最长 100，可为空，默认空串 */
    @MpField(value = "refund_account_no", columnType = "string", length = 100, nullable = true, comment = "退款账号")
    private String refundAccountNo = "";

    /** 创建时间（整型时间戳） */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
