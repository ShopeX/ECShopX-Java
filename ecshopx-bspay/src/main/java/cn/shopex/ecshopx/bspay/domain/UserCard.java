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
 * 用户结算账户
 */
@Data
@MpTable(value = "bspay_user_card", comment = "用户结算账户", indexes = {@MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_card_no", columns = {"card_no"}, lengths = {64}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_huifu_id", columns = {"huifu_id"})})
public class UserCard {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 商户的 huifu_id */
    @MpField(value = "sys_id", columnType = "string", nullable = true, comment = "商户的huifu_id")
    private String sysId = "";

    /** 汇付ID */
    @MpField(value = "huifu_id", columnType = "string", nullable = true, comment = "汇付ID")
    private String huifuId = "";

    /** 开户进件ID */
    @MpField(value = "user_id", columnType = "bigint", comment = "开户进件ID")
    private Long userId;

    /** 请求流水号 */
    @MpField(value = "req_seq_id", columnType = "string", length = 64, nullable = true, comment = "请求流水号")
    private String reqSeqId = "";

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;

    /**
     * 进件类型，默认 indv；indv=个人，ent=企业
     */
    @MpField(value = "user_type", columnType = "string", length = 20, comment = "进件类型", defaultValue = "indv")
    private String userType = "indv";

    /**
     * 结算银行卡账户类型 0：对公，1：对私，2：对私非法人；个人商户/用户不支持对公类型，对私非法人类型
     */
    @MpField(value = "card_type", columnType = "string", length = 10, comment = "结算银行卡账户类型 0：对公，1：对私，2：对私非法人；", defaultValue = "0")
    private String cardType = "0";

    /** 结算银行卡持卡人姓名 */
    @MpField(value = "card_name", columnType = "string", length = 500, comment = "结算银行卡持卡人姓名")
    private String cardName = "";

    /** 结算银行卡卡号 */
    @MpField(value = "card_no", columnType = "string", length = 100, comment = "结算银行卡卡号")
    private String cardNo = "";

    /** 结算卡银行所在省 */
    @MpField(value = "prov_id", columnType = "string", length = 12, comment = "结算卡银行所在省")
    private String provId = "";

    /** 结算卡银行所在市 */
    @MpField(value = "area_id", columnType = "string", length = 12, comment = "结算卡银行所在市")
    private String areaId = "";

    /** 结算卡银行号，对公必填 */
    @MpField(value = "bank_code", columnType = "string", length = 12, nullable = true, comment = "结算卡银行号，对公必填")
    private String bankCode = "";

    /** 结算卡支行名称，对公时必填 */
    @MpField(value = "branch_name", columnType = "string", length = 100, nullable = true, comment = "结算卡支行名称，对公时必填")
    private String branchName = "";

    /** 结算卡持卡人身份证号，对私必填 */
    @MpField(value = "cert_no", columnType = "string", length = 32, nullable = true, comment = "结算卡持卡人身份证号，对私必填")
    private String certNo = "";

    /** 结算卡持卡人身份证有效期类型 1:长期有效 0:非长期有效 */
    @MpField(value = "cert_validity_type", columnType = "integer", nullable = true, comment = "结算卡持卡人身份证有效期类型 1:长期有效 0:非长期有效；", defaultValue = "0")
    private Integer certValidityType = 0;

    /** 结算卡持卡人证件有效期（起始） yyyyMMdd */
    @MpField(value = "cert_begin_date", columnType = "string", length = 8, comment = "结算卡持卡人证件有效期（起始） 日期格式：yyyyMMdd")
    private String certBeginDate = "";

    /** 结算卡持卡人证件有效期（截止） yyyyMMdd；非长期有效时必填 */
    @MpField(value = "cert_end_date", columnType = "string", length = 8, nullable = true, comment = "结算卡持卡人证件有效期（截止） 日期格式：yyyyMMdd;非长期有效时必填")
    private String certEndDate = "";

    /** 结算银行卡绑定手机号 */
    @MpField(value = "mp", columnType = "string", length = 255, nullable = true, comment = "结算银行卡绑定手机号")
    private String mp = "";

    /** 申请单号 */
    @MpField(value = "apply_no", columnType = "string", length = 30, nullable = true, comment = "申请单号")
    private String applyNo = "";

    /**
     * 审核状态，状态包括：A-审核中；B-配置成功；C-配置失败
     */
    @MpField(value = "audit_state", columnType = "string", length = 50, nullable = true, comment = "审核状态，状态包括：A-审核中；B-配置成功；C-配置失败")
    private String auditState = "";

    /** 审核结果描述 */
    @MpField(value = "audit_desc", columnType = "string", length = 200, nullable = true, comment = "审核结果描述")
    private String auditDesc = "";

    /** 错误描述 */
    @MpField(value = "error_info", columnType = "string", length = 500, nullable = true, comment = "错误描述")
    private String errorInfo = "";

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
