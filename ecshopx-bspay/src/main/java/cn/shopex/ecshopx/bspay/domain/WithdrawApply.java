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

package cn.shopex.ecshopx.bspay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 汇付斗拱提现申请表
 */
@Data
@MpTable(value = "bspay_withdraw_apply", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_company_status", columns = {"company_id", "status"}), @MpIndex(name = "idx_operator", columns = {"operator_type", "operator_id"}), @MpIndex(name = "idx_status", columns = {"status"}), @MpIndex(name = "idx_created", columns = {"created"})})
public class WithdrawApply {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 商户ID */
    @MpField(value = "merchant_id", columnType = "bigint", nullable = true, comment = "商户ID", defaultValue = "0")
    private Long merchantId;

    /** 店铺ID */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "店铺ID", defaultValue = "0")
    private Long distributorId;

    /**
     * 操作者类型：distributor=店铺, merchant=商户, admin=超级管理员, staff=员工
     */
    @MpField(value = "operator_type", columnType = "string", length = 20)
    private String operatorType = "";

    /** 操作账号ID */
    @MpField(value = "operator_id", columnType = "bigint", comment = "操作账号ID")
    private Long operatorId;

    /** 汇付ID */
    @MpField(value = "huifu_id", columnType = "string", length = 255, nullable = true, comment = "汇付ID")
    private String huifuId = "";

    /** 申请金额（分） */
    @MpField(value = "amount", columnType = "integer", comment = "申请金额（分）", defaultValue = "0")
    private Integer amount = 0;

    /** 提现类型 */
    @MpField(value = "withdraw_type", columnType = "string", length = 10, comment = "提现类型", defaultValue = "T1")
    private String withdrawType = "T1";

    /** 发票文件路径 */
    @MpField(value = "invoice_file", columnType = "string", length = 500, nullable = true, comment = "发票文件路径")
    private String invoiceFile = "";

    /**
     * 申请状态 0=审核中 1=审核通过 2=已拒绝 3=处理中 4=处理成功 5=处理失败 (参见WithdrawStatus枚举)
     */
    @MpField(value = "status", columnType = "smallint", defaultValue = "0")
    private Integer status = 0;

    /** 审核时间 */
    @MpField(value = "audit_time", columnType = "integer", nullable = true, comment = "审核时间")
    private Integer auditTime;

    /** 审核人 */
    @MpField(value = "auditor", columnType = "string", length = 100, nullable = true, comment = "审核人")
    private String auditor = "";

    /** 审核人操作账号ID */
    @MpField(value = "auditor_operator_id", columnType = "bigint", nullable = true, comment = "审核人操作账号ID")
    private Long auditorOperatorId;

    /** 审核备注 */
    @MpField(value = "audit_remark", columnType = "text", nullable = true, comment = "审核备注")
    private String auditRemark;

    /** 汇付全局流水号 */
    @MpField(value = "hf_seq_id", columnType = "string", length = 128, nullable = true, comment = "汇付全局流水号")
    private String hfSeqId = "";

    /** 请求流水号 */
    @MpField(value = "req_seq_id", columnType = "string", length = 128, nullable = true, comment = "请求流水号")
    private String reqSeqId = "";

    /** 请求汇付时间 */
    @MpField(value = "request_time", columnType = "integer", nullable = true, comment = "请求汇付时间")
    private Integer requestTime;

    /** 失败原因 */
    @MpField(value = "failure_reason", columnType = "text", nullable = true, comment = "失败原因")
    private String failureReason;

    /** 申请人账号 */
    @MpField(value = "operator", columnType = "string", length = 32, nullable = true, comment = "申请人账号")
    private String operator = "";

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
