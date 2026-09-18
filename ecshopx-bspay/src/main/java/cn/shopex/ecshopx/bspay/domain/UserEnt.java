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
 * 企业用户对象
 */
@Data
@MpTable(value = "bspay_user_ent", comment = "企业用户对象", indexes = {@MpIndex(name = "idx_req_seq_id", columns = {"req_seq_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_sys_id", columns = {"sys_id"}), @MpIndex(name = "idx_huifu_id", columns = {"huifu_id"}), @MpIndex(name = "idx_audit_state", columns = {"audit_state"})})
public class UserEnt {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 请求流水号 */
    @MpField(value = "req_seq_id", columnType = "string", length = 64, nullable = true, comment = "请求流水号")
    private String reqSeqId = "";

    /** 商户的 huifu_id */
    @MpField(value = "sys_id", columnType = "string", comment = "商户的huifu_id")
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

    /** 企业名称 */
    @MpField(value = "reg_name", columnType = "string", length = 255, comment = "企业名称")
    private String regName = "";

    /** 营业执照编号 */
    @MpField(value = "license_code", columnType = "string", length = 32, comment = "营业执照编号")
    private String licenseCode = "";

    /** 营业执照有效期类型 1:长期有效 0:非长期有效 */
    @MpField(value = "license_validity_type", columnType = "integer", nullable = true, comment = "营业执照有效期类型 1:长期有效 0:非长期有效；", defaultValue = "0")
    private Integer licenseValidityType = 0;

    /** 营业执照有效期起始日期 yyyyMMdd */
    @MpField(value = "license_begin_date", columnType = "string", length = 8, comment = "营业执照有效期起始日期 日期格式：yyyyMMdd")
    private String licenseBeginDate = "";

    /** 营业执照有效期结束日期 yyyyMMdd；非长期有效时必填 */
    @MpField(value = "license_end_date", columnType = "string", length = 8, nullable = true, comment = "营业执照有效期结束日期 日期格式：yyyyMMdd;非长期有效时必填")
    private String licenseEndDate = "";

    /** 注册地址(省) */
    @MpField(value = "reg_prov_id", columnType = "string", length = 12, nullable = true, comment = "注册地址(省)")
    private String regProvId = "";

    /** 注册地址(市) */
    @MpField(value = "reg_area_id", columnType = "string", length = 12, comment = "注册地址(市)")
    private String regAreaId = "";

    /** 注册地址(区) */
    @MpField(value = "reg_district_id", columnType = "string", length = 12, comment = "注册地址(区)")
    private String regDistrictId = "";

    /** 注册地址(详细信息) */
    @MpField(value = "reg_detail", columnType = "string", length = 300, comment = "注册地址(详细信息)")
    private String regDetail = "";

    /** 法人姓名 */
    @MpField(value = "legal_name", columnType = "string", length = 500, comment = "法人姓名")
    private String legalName = "";

    /** 法人身份证号码 */
    @MpField(value = "legal_cert_no", columnType = "string", length = 255, comment = "法人身份证号码")
    private String legalCertNo = "";

    /** 法人身份证有效期类型 1:长期有效 0:非长期有效 */
    @MpField(value = "legal_cert_validity_type", columnType = "integer", nullable = true, comment = "法人身份证有效期类型 1:长期有效 0:非长期有效；", defaultValue = "0")
    private Integer legalCertValidityType = 0;

    /** 法人身份证有效期开始日期 */
    @MpField(value = "legal_cert_begin_date", columnType = "string", length = 8, comment = "法人身份证有效期开始日期")
    private String legalCertBeginDate = "";

    /** 法人身份证有效期截止日期 yyyyMMdd；非长期有效时必填 */
    @MpField(value = "legal_cert_end_date", columnType = "string", length = 8, nullable = true, comment = "法人身份证有效期截止日期 日期格式：yyyyMMdd;非长期有效时必填")
    private String legalCertEndDate = "";

    /** 联系人姓名 */
    @MpField(value = "contact_name", columnType = "string", length = 500, comment = "联系人姓名")
    private String contactName = "";

    /** 联系人手机 */
    @MpField(value = "contact_mobile", columnType = "string", length = 255, comment = "联系人手机")
    private String contactMobile = "";

    /**
     * 公司类型 1:政府机构 2:国营企业 3:私营企业 4:外资企业 5:个体工商户 6:其它组织 7:事业单位 8:集体经济
     */
    @MpField(value = "ent_type", columnType = "integer", comment = "公司类型 1:政府机构 2:国营企业 3:私营企业 4:外资企业 5:个体工商户 6:其它组织 7:事业单位 8:集体经济", defaultValue = "1")
    private Integer entType = 1;

    /**
     * 审核状态，状态包括：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功
     */
    @MpField(value = "audit_state", columnType = "string", length = 50, nullable = true, comment = "审核状态，状态包括：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功")
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
