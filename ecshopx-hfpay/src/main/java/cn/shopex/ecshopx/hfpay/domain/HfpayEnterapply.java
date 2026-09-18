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

package cn.shopex.ecshopx.hfpay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 入驻信息表
 */
@Data
@MpTable(value = "hfpay_enterapply", comment = "入驻信息表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"})})
public class HfpayEnterapply {

    /** 入驻信息表id */
    @MpId(value = "hfpay_enterapply_id", type = IdType.AUTO, columnType = "bigint", comment = "入驻信息表id")
    private Long hfpayEnterapplyId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 分销商id，可为空 */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "分销商id")
    private Long distributorId;

    /** 用户id，可为空 */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 汇付客户号，可为空 */
    @MpField(value = "user_cust_id", columnType = "string", nullable = true, comment = "汇付客户号")
    private String userCustId;

    /** 汇付子账户，可为空 */
    @MpField(value = "acct_id", columnType = "string", nullable = true, comment = "汇付子账户")
    private String acctId;

    /**
     * 入驻类型，可为空，最长 1。1 企业；2 个体户；3 个人
     */
    @MpField(value = "apply_type", columnType = "string", length = 1, nullable = true, comment = "入驻类型")
    private String applyType;

    /**
     * 企业证照类型，可为空，最长 1，默认 2。1 普通证照；2 三证合一
     */
    @MpField(value = "corp_license_type", columnType = "string", length = 1, nullable = true, comment = "企业证照类型")
    private String corpLicenseType = "2";

    /** 企业名称，可为空，最长 100 */
    @MpField(value = "corp_name", columnType = "string", length = 100, nullable = true, comment = "企业名称")
    private String corpName;

    /** 营业执照注册号，可为空，最长 100 */
    @MpField(value = "business_code", columnType = "string", length = 100, nullable = true, comment = "营业执照注册号")
    private String businessCode;

    /** 组织机构代码，可为空，最长 100 */
    @MpField(value = "institution_code", columnType = "string", length = 100, nullable = true, comment = "组织机构代码")
    private String institutionCode;

    /** 税务登记证号，可为空，最长 100 */
    @MpField(value = "tax_code", columnType = "string", length = 100, nullable = true, comment = "税务登记证号")
    private String taxCode;

    /** 统一社会信用代码，可为空，最长 100 */
    @MpField(value = "social_credit_code", columnType = "string", length = 100, nullable = true, comment = "统一社会信用代码")
    private String socialCreditCode;

    /** 证照起始日期，可为空，最长 30 */
    @MpField(value = "license_start_date", columnType = "string", length = 30, nullable = true, comment = "证照起始日期")
    private String licenseStartDate;

    /** 证照结束日期，可为空，最长 30 */
    @MpField(value = "license_end_date", columnType = "string", length = 30, nullable = true, comment = "证照结束日期")
    private String licenseEndDate;

    /** 实际控股人，可为空，最长 255 */
    @MpField(value = "controlling_shareholder", columnType = "string", length = 255, nullable = true, comment = "实际控股人")
    private String controllingShareholder;

    /** 法人姓名，可为空，最长 60 */
    @MpField(value = "legal_name", columnType = "string", length = 60, nullable = true, comment = "法人姓名")
    private String legalName;

    /** 法人证件类型，可为空，最长 2 */
    @MpField(value = "legal_id_card_type", columnType = "string", length = 2, nullable = true, comment = "法人证件类型")
    private String legalIdCardType;

    /** 法人证件号码，可为空，最长 30 */
    @MpField(value = "legal_id_card", columnType = "string", length = 30, nullable = true, comment = "法人证件号码")
    private String legalIdCard;

    /** 法人证件起始日期，可为空，最长 60 */
    @MpField(value = "legal_cert_start_date", columnType = "string", length = 60, nullable = true, comment = "法人证件起始日期")
    private String legalCertStartDate;

    /** 法人证件结束日期，可为空，最长 60 */
    @MpField(value = "legal_cert_end_date", columnType = "string", length = 60, nullable = true, comment = "法人证件结束日期")
    private String legalCertEndDate;

    /** 法人手机号码，可为空，最长 20 */
    @MpField(value = "legal_mobile", columnType = "string", length = 20, nullable = true, comment = "法人手机号码")
    private String legalMobile;

    /** 企业联系人姓名，可为空，最长 30 */
    @MpField(value = "contact_name", columnType = "string", length = 30, nullable = true, comment = "企业联系人姓名")
    private String contactName;

    /** 企业联系人手机号，可为空，最长 20 */
    @MpField(value = "contact_mobile", columnType = "string", length = 20, nullable = true, comment = "企业联系人手机号")
    private String contactMobile;

    /** 联系人邮箱，可为空，最长 30 */
    @MpField(value = "contact_email", columnType = "string", length = 30, nullable = true, comment = "联系人邮箱")
    private String contactEmail;

    /** 开户银行账户名，可为空，最长 30 */
    @MpField(value = "bank_acct_name", columnType = "string", length = 30, nullable = true, comment = "开户银行账户名")
    private String bankAcctName;

    /** 开户银行，可为空，最长 30 */
    @MpField(value = "bank_id", columnType = "string", length = 30, nullable = true, comment = "开户银行")
    private String bankId;

    /** 开户银行名称，可为空，最长 30 */
    @MpField(value = "bank_name", columnType = "string", length = 30, nullable = true, comment = "开户银行名称")
    private String bankName;

    /** 开户银行账号，可为空，最长 30 */
    @MpField(value = "bank_acct_num", columnType = "string", length = 30, nullable = true, comment = "开户银行账号")
    private String bankAcctNum;

    /** 银行卡正面照，可为空 */
    @MpField(value = "bank_acct_num_imgz", columnType = "string", nullable = true, comment = "银行卡正面照")
    private String bankAcctNumImgz;

    /** 本地银行卡正面照，可为空 */
    @MpField(value = "bank_acct_num_imgz_local", columnType = "string", nullable = true, comment = "本地银行卡正面照")
    private String bankAcctNumImgzLocal;

    /** 法人银行卡反面照，可为空 */
    @MpField(value = "bank_acct_num_imgf", columnType = "string", nullable = true, comment = "法人银行卡反面照")
    private String bankAcctNumImgf;

    /** 本地银行卡反面照，可为空 */
    @MpField(value = "bank_acct_num_imgf_local", columnType = "string", nullable = true, comment = "本地银行卡反面照")
    private String bankAcctNumImgfLocal;

    /** 开户银行省份，可为空，最长 30 */
    @MpField(value = "bank_prov", columnType = "string", length = 30, nullable = true, comment = "开户银行省份")
    private String bankProv;

    /** 开户银行省份名称，可为空，最长 30 */
    @MpField(value = "bank_prov_name", columnType = "string", length = 30, nullable = true, comment = "开户银行省份名称")
    private String bankProvName;

    /** 开户银行地区，可为空，最长 30 */
    @MpField(value = "bank_area", columnType = "string", length = 30, nullable = true, comment = "开户银行地区")
    private String bankArea;

    /** 开户银行地区名称，可为空，最长 30 */
    @MpField(value = "bank_area_name", columnType = "string", length = 30, nullable = true, comment = "开户银行地区名称")
    private String bankAreaName;

    /** 企业开户银行的支行名称，可为空，最长 30 */
    @MpField(value = "bank_branch", columnType = "string", length = 30, nullable = true, comment = "企业开户银行的支行名称")
    private String bankBranch;

    /** 个体户名称，可为空，最长 30 */
    @MpField(value = "solo_name", columnType = "string", length = 30, nullable = true, comment = "个体户名称")
    private String soloName;

    /** 个体户经营地址，可为空，最长 150 */
    @MpField(value = "solo_business_address", columnType = "string", length = 150, nullable = true, comment = "个体户经营地址")
    private String soloBusinessAddress;

    /** 个体户注册地址，可为空，最长 150 */
    @MpField(value = "solo_reg_address", columnType = "string", length = 150, nullable = true, comment = "个体户注册地址")
    private String soloRegAddress;

    /** 个体户固定电话，可为空，最长 30 */
    @MpField(value = "solo_fixed_telephone", columnType = "string", length = 30, nullable = true, comment = "个体户固定电话")
    private String soloFixedTelephone;

    /** 经营范围，可为空 */
    @MpField(value = "business_scope", columnType = "string", nullable = true, comment = "经营范围")
    private String businessScope;

    /** 职业，可为空，最长 80 */
    @MpField(value = "occupation", columnType = "string", length = 80, nullable = true, comment = "职业")
    private String occupation;

    /** 联系人证件号，可为空，最长 30 */
    @MpField(value = "contact_cert_num", columnType = "string", length = 30, nullable = true, comment = "联系人证件号")
    private String contactCertNum;

    /** 开户许可证核准号，可为空，最长 60 */
    @MpField(value = "open_license_no", columnType = "string", length = 60, nullable = true, comment = "开户许可证核准号")
    private String openLicenseNo;

    /** 用户姓名，可为空，最长 30 */
    @MpField(value = "user_name", columnType = "string", length = 30, nullable = true, comment = "用户姓名")
    private String userName;

    /** 证件类型，可为空，最长 2 */
    @MpField(value = "id_card_type", columnType = "string", length = 2, nullable = true, comment = "证件类型")
    private String idCardType;

    /** 身份证号，可为空，最长 30 */
    @MpField(value = "id_card", columnType = "string", length = 30, nullable = true, comment = "身份证号")
    private String idCard;

    /** 手机号，可为空，最长 20 */
    @MpField(value = "user_mobile", columnType = "string", length = 20, nullable = true, comment = "手机号")
    private String userMobile;

    /** 汇付订单号，可为空 */
    @MpField(value = "hf_order_id", columnType = "string", nullable = true, comment = "汇付订单号")
    private String hfOrderId;

    /** 汇付订单日期，可为空 */
    @MpField(value = "hf_order_date", columnType = "string", nullable = true, comment = "汇付订单日期")
    private String hfOrderDate;

    /** 汇付开户申请号，可为空 */
    @MpField(value = "hf_apply_id", columnType = "string", nullable = true, comment = "汇付开户申请号")
    private String hfApplyId;

    /**
     * 状态，可为空，默认 1。1 未提交进件信息；2 已经提交进件信息，审核中；3 已开商户；4 审核失败
     */
    @MpField(value = "status", columnType = "string", nullable = true, comment = "状态", defaultValue = "1")
    private String status = "1";

    /** 营业执照注册号图片，可为空 */
    @MpField(value = "business_code_img", columnType = "string", nullable = true, comment = "营业执照注册号图片")
    private String businessCodeImg;

    /** 本地营业执照注册号图片，可为空 */
    @MpField(value = "business_code_img_local", columnType = "string", nullable = true, comment = "本地营业执照注册号图片")
    private String businessCodeImgLocal;

    /** 组织机构代码图片，可为空 */
    @MpField(value = "institution_code_img", columnType = "string", nullable = true, comment = "组织机构代码图片")
    private String institutionCodeImg;

    /** 本地组织机构代码图片，可为空 */
    @MpField(value = "institution_code_img_local", columnType = "string", nullable = true, comment = "本地组织机构代码图片")
    private String institutionCodeImgLocal;

    /** 税务登记证号图片，可为空 */
    @MpField(value = "tax_code_img", columnType = "string", nullable = true, comment = "税务登记证号图片")
    private String taxCodeImg;

    /** 本地税务登记证号图片，可为空 */
    @MpField(value = "tax_code_img_local", columnType = "string", nullable = true, comment = "本地税务登记证号图片")
    private String taxCodeImgLocal;

    /** 统一社会信用代码图片，可为空 */
    @MpField(value = "social_credit_code_img", columnType = "string", nullable = true, comment = "统一社会信用代码图片")
    private String socialCreditCodeImg;

    /** 本地统一社会信用代码图片，可为空 */
    @MpField(value = "social_credit_code_img_local", columnType = "string", nullable = true, comment = "本地统一社会信用代码图片")
    private String socialCreditCodeImgLocal;

    /** 法人身份证正面照，可为空 */
    @MpField(value = "legal_card_imgz", columnType = "string", nullable = true, comment = "法人身份证正面照")
    private String legalCardImgz;

    /** 本地法人身份证正面照，可为空 */
    @MpField(value = "legal_card_imgz_local", columnType = "string", nullable = true, comment = "本地法人身份证正面照")
    private String legalCardImgzLocal;

    /** 法人身份证反面照，可为空 */
    @MpField(value = "legal_card_imgf", columnType = "string", nullable = true, comment = "法人身份证反面照")
    private String legalCardImgf;

    /** 本地法人身份证反面照，可为空 */
    @MpField(value = "legal_card_imgf_local", columnType = "string", nullable = true, comment = "本地法人身份证反面照")
    private String legalCardImgfLocal;

    /** 开户银行许可证图片，可为空 */
    @MpField(value = "bank_acct_img", columnType = "string", nullable = true, comment = "开户银行许可证图片")
    private String bankAcctImg;

    /** 本地开户银行许可证图片，可为空 */
    @MpField(value = "bank_acct_img_local", columnType = "string", nullable = true, comment = "本地开户银行许可证图片")
    private String bankAcctImgLocal;

    /** 汇付响应码，可为空 */
    @MpField(value = "resp_code", columnType = "string", nullable = true, comment = "汇付响应码")
    private String respCode;

    /** 汇付响应码描述，可为空 */
    @MpField(value = "resp_desc", columnType = "string", nullable = true, comment = "汇付响应码描述")
    private String respDesc;

    /** 创建时间 */
    @MpField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @MpField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
