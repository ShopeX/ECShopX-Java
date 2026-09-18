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
 * 个人用户
 */
@Data
@MpTable(value = "bspay_user_indv", comment = "个人用户", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_sys_id", columns = {"sys_id"}), @MpIndex(name = "idx_huifu_id", columns = {"huifu_id"}), @MpIndex(name = "idx_audit_state", columns = {"audit_state"})})
public class UserIndv {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 请求流水号 */
    @MpField(value = "req_seq_id", columnType = "string", length = 64, nullable = true, comment = "请求流水号")
    private String reqSeqId = "";

    /** 商户的 huifu_id */
    @MpField(value = "sys_id", columnType = "string", nullable = true, comment = "商户的huifu_id")
    private String sysId = "";

    /** 汇付ID */
    @MpField(value = "huifu_id", columnType = "string", nullable = true, comment = "汇付ID")
    private String huifuId = "";

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;

    /** 是否审核成功后修改 1:是 0:否 */
    @MpField(value = "is_update", columnType = "integer", length = 10, comment = "是否审核成功后修改 1:是  0:否", defaultValue = "0")
    private Integer isUpdate = 0;

    /** 个人姓名 */
    @MpField(value = "name", columnType = "string", length = 500, comment = "个人姓名")
    private String name = "";

    /** 个人身份证号码 */
    @MpField(value = "cert_no", columnType = "string", length = 255, comment = "个人身份证号码")
    private String certNo = "";

    /** 个人身份证有效期类型 1:长期有效 0:非长期有效 */
    @MpField(value = "cert_validity_type", columnType = "integer", nullable = true, comment = "个人身份证有效期类型 1:长期有效 0:非长期有效；", defaultValue = "0")
    private Integer certValidityType = 0;

    /** 个人身份证有效期开始日期 */
    @MpField(value = "cert_begin_date", columnType = "string", length = 8, comment = "个人身份证有效期开始日期")
    private String certBeginDate = "";

    /** 个人身份证有效期截止日期，日期格式 yyyyMMdd；非长期有效时必填 */
    @MpField(value = "cert_end_date", columnType = "string", length = 8, nullable = true, comment = "个人身份证有效期截止日期 日期格式：yyyyMMdd;非长期有效时必填")
    private String certEndDate = "";

    /** 手机号 */
    @MpField(value = "mobile_no", columnType = "string", length = 255, comment = "手机号")
    private String mobileNo = "";

    /**
     * 审核状态，状态包括：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功
     */
    @MpField(value = "audit_state", columnType = "string", length = 50, nullable = true, comment = "审核状态，状态包括：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功", defaultValue = "A")
    private String auditState = "A";

    /** 审核结果描述 */
    @MpField(value = "audit_desc", columnType = "string", length = 500, nullable = true, comment = "审核结果描述")
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
