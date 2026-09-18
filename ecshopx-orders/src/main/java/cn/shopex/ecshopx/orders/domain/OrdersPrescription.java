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

/** 问诊单开方数据 */
@Data
@MpTable(value = "orders_prescription", comment = "问诊单开方数据", indexes = {@MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_diagnosis_id", columns = {"diagnosis_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "idx_status", columns = {"status"}), @MpIndex(name = "idx_is_deleted", columns = {"is_deleted"}), @MpIndex(name = "idx_user_family_name", columns = {"user_family_name"}), @MpIndex(name = "idx_user_family_phone", columns = {"user_family_phone"}), @MpIndex(name = "idx_user_family_id_card", columns = {"user_family_id_card"}), @MpIndex(name = "idx_doctor_name", columns = {"doctor_name"}), @MpIndex(name = "idx_serial_no", columns = {"serial_no"}), @MpIndex(name = "idx_audit_apothecary_name", columns = {"audit_apothecary_name"}), @MpIndex(name = "idx_audit_status", columns = {"audit_status"})})
public class OrdersPrescription {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", length = 64, comment = "自增id")
    private Long id;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", length = 64, comment = "订单号")
    private String orderId;

    /** 问诊单id */
    @MpField(value = "diagnosis_id", columnType = "bigint", comment = "问诊单id")
    private Long diagnosisId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id")
    private Long distributorId;

    /** 快诊580处方ID */
    @MpField(value = "prescription_id", columnType = "bigint", comment = "快诊580处方ID")
    private Long prescriptionId;

    /** 互联网医院名称 */
    @MpField(value = "hospital_name", columnType = "string", comment = "互联网医院名称")
    private String hospitalName;

    /** 580门店Id */
    @MpField(value = "kuaizhen_store_id", columnType = "bigint", comment = "580门店Id")
    private Long kuaizhenStoreId;

    /** 580门店名称 */
    @MpField(value = "kuaizhen_store_name", columnType = "string", comment = "580门店名称")
    private String kuaizhenStoreName;

    /** 580问诊单ID */
    @MpField(value = "kuaizhen_diagnosis_id", columnType = "bigint", comment = "580问诊单ID")
    private Long kuaizhenDiagnosisId;

    /** 医生签署时间（时间戳） */
    @MpField(value = "doctor_sign_time", columnType = "bigint", comment = "医生签署时间(时间戳)")
    private Long doctorSignTime;

    /** 医生科室 */
    @MpField(value = "doctor_office", columnType = "string", comment = "医生科室")
    private String doctorOffice;

    /** 医生id */
    @MpField(value = "doctor_id", columnType = "bigint", comment = "医生id")
    private Long doctorId;

    /** 医生姓名 */
    @MpField(value = "doctor_name", columnType = "string", comment = "医生姓名")
    private String doctorName;

    /** 就诊人姓名 */
    @MpField(value = "user_family_name", columnType = "string", comment = "就诊人姓名")
    private String userFamilyName;

    /** 就诊人手机号码 */
    @MpField(value = "user_family_phone", columnType = "string", comment = "就诊人手机号码")
    private String userFamilyPhone;

    /** 就诊人年龄 */
    @MpField(value = "user_family_age", columnType = "integer", comment = "就诊人年龄")
    private Integer userFamilyAge;

    /** 就诊人性别：0 未知，1 男，2 女 */
    @MpField(value = "user_family_gender", columnType = "smallint", comment = "就诊人性别，0未知，1男，2女")
    private Integer userFamilyGender;

    /** 用药人身份证号 */
    @MpField(value = "user_family_id_card", columnType = "string", comment = "用药人身份证号")
    private String userFamilyIdCard;

    /** 诊断标签 */
    @MpField(value = "tags", columnType = "string", comment = "诊断标签")
    private String tags;

    /** 处方状态：1 正常，2 已作废 */
    @MpField(value = "status", columnType = "smallint", comment = "处方状态(1正常 2已作废)")
    private Integer status;

    /** 备注 */
    @MpField(value = "memo", columnType = "string", comment = "备注")
    private String memo;

    /** 补充说明 */
    @MpField(value = "remarks", columnType = "string", comment = "补充说明")
    private String remarks;

    /** 审核不通过的理由（可能为空） */
    @MpField(value = "reason", columnType = "string", comment = "审核不通过的理由（可能为空）")
    private String reason;

    /** 处方图片地址 */
    @MpField(value = "dst_file_path", columnType = "string", comment = "处方图片地址")
    private String dstFilePath;

    /** 处方编号 */
    @MpField(value = "serial_no", columnType = "string", comment = "处方编号")
    private String serialNo;

    /** 药品信息说明 */
    @MpField(value = "drug_rsp_list", columnType = "text", nullable = true, comment = "药品信息说明")
    private String drugRspList;

    /** 处方审核状态：1 未审核，2 审核通过，3 审核不通过，4 不需要审方 */
    @MpField(value = "audit_status", columnType = "smallint", comment = "处方审核状态，1未审核，2审核通过，3审核不通过，4不需要审方", defaultValue = "1")
    private Integer auditStatus;

    /** 审方时间 */
    @MpField(value = "audit_time", columnType = "integer", comment = "审方时间", defaultValue = "0")
    private Integer auditTime;

    /** 审方不通过原因 */
    @MpField(value = "audit_reason", columnType = "string", comment = "审方不通过原因")
    private String auditReason;

    /** 审方药师名称 */
    @MpField(value = "audit_apothecary_name", columnType = "string", comment = "审方药师名称")
    private String auditApothecaryName;

    /** 是否已废弃：0 否，1 是 */
    @MpField(value = "is_deleted", columnType = "smallint", comment = "是否已废弃, 0否  1是", defaultValue = "0")
    private Integer isDeleted;

    /** 废弃时间 */
    @MpField(value = "delete_time", columnType = "integer", comment = "废弃时间", defaultValue = "0")
    private Integer deleteTime;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
