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

/** 线下转账支付订单 */
@Data
@MpTable(value = "offline_payment", comment = "线下转账支付订单", indexes = {@MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_create_time", columns = {"create_time"}), @MpIndex(name = "idx_check_status", columns = {"check_status"})})
public class OfflinePayment {

    /** 自增ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "自增ID")
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

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 订单金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "bigint", comment = "订单金额，以分为单位")
    private Long totalFee;

    /** 支付金额，以分为单位 */
    @MpField(value = "pay_fee", columnType = "bigint", comment = "支付金额，以分为单位")
    private Long payFee;

    /** 审核状态。可选值有 0 待处理;1 已审核;2 已拒绝;9 已取消 */
    @MpField(value = "check_status", columnType = "smallint", comment = "审核状态。可选值有 0 待处理;1 已审核;2 已拒绝;9 已取消", defaultValue = "0")
    private Integer checkStatus = 0;

    /** 收款账户id */
    @MpField(value = "bank_account_id", columnType = "bigint", comment = "收款账户id")
    private Long bankAccountId;

    /** 收款账户名称 */
    @MpField(value = "bank_account_name", columnType = "string", length = 50, comment = "收款账户名称")
    private String bankAccountName;

    /** 银行账号 */
    @MpField(value = "bank_account_no", columnType = "string", length = 30, comment = "银行账号")
    private String bankAccountNo;

    /** 开户银行 */
    @MpField(value = "bank_name", columnType = "string", length = 100, comment = "开户银行")
    private String bankName;

    /** 银联号 */
    @MpField(value = "china_ums_no", columnType = "string", length = 20, comment = "银联号")
    private String chinaUmsNo;

    /** 付款账户名 */
    @MpField(value = "pay_account_name", columnType = "string", length = 100, nullable = true, comment = "付款账户名")
    private String payAccountName;

    /** 付款银行 */
    @MpField(value = "pay_account_bank", columnType = "string", length = 100, nullable = true, comment = "付款银行")
    private String payAccountBank;

    /** 付款账号 */
    @MpField(value = "pay_account_no", columnType = "string", length = 100, nullable = true, comment = "付款账号")
    private String payAccountNo;

    /** 付款流水单号 */
    @MpField(value = "pay_sn", columnType = "string", length = 100, nullable = true, comment = "付款流水单号")
    private String paySn;

    /** 付款凭证图片 */
    @MpField(value = "voucher_pic", columnType = "json_array", comment = "付款凭证图片")
    private String voucherPic;

    /** 转账备注 */
    @MpField(value = "transfer_remark", columnType = "string", length = 100, nullable = true, comment = "转账备注")
    private String transferRemark;

    /** 审核人 */
    @MpField(value = "operator_name", columnType = "string", length = 50, nullable = true, comment = "审核人")
    private String operatorName;

    /** 审核备注 */
    @MpField(value = "remark", columnType = "string", length = 500, nullable = true, comment = "审核备注")
    private String remark;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
