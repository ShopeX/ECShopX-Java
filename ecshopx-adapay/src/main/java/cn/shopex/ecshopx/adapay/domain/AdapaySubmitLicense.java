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

package cn.shopex.ecshopx.adapay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * adapay提交商户证照
 */
@Data
@MpTable(value = "adapay_submit_license", comment = "adapay提交商户证照", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class AdapaySubmitLicense {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 渠道商下商户的apiKey */
    @MpField(value = "sub_api_key", columnType = "string", comment = "渠道商下商户的apiKey")
    private String subApiKey;

    /** 统一社会信用代码的pic_id，企业商户必填，小微商户不填 */
    @MpField(value = "social_credit_code_id", columnType = "string", nullable = true, comment = "统一社会信用代码的pic_id，企业商户必填，小微商户不填")
    private String socialCreditCodeId;

    /** 法人身份证正面的pic_id */
    @MpField(value = "legal_certId_front_id", columnType = "string", comment = "法人身份证正面的pic_id")
    private String legalCertIdFrontId;

    /** 法人身份证反面的pic_id */
    @MpField(value = "legal_cert_id_back_id", columnType = "string", comment = "法人身份证反面的pic_id")
    private String legalCertIdBackId;

    /** 开户许可证图片的pic_id */
    @MpField(value = "account_opening_permit_id", columnType = "string", comment = "开户许可证图片的pic_id")
    private String accountOpeningPermitId;

    /** 若入驻的费率类型为线上时，该字段必填，请传入商户的业务网址或者商城地址 */
    @MpField(value = "business_add", columnType = "string", length = 500, nullable = true, comment = "若入驻的费率类型为线上时，该字段必填，请传入商户的业务网址或者商城地址")
    private String businessAdd;

    /** 门店的pic_id，若入驻的费率类型为线下时，该字段必填 */
    @MpField(value = "store_id", columnType = "string", nullable = true, comment = "门店的pic_id，若入驻的费率类型为线下时，该字段必填")
    private String storeId;

    /** 商户在业务网址或商城地址上测试的交易记录截图的pic_id */
    @MpField(value = "transaction_test_record_id", columnType = "string", nullable = true, comment = "商户在业务网址或商城地址上测试的交易记录截图的pic_id")
    private String transactionTestRecordId;

    /** 网站截图的pic_id */
    @MpField(value = "web_pic_id", columnType = "string", nullable = true, comment = "网站截图的pic_id")
    private String webPicId;

    /** 租赁合同的pic_id，如经营场所照片无法体现经营内容时上传 */
    @MpField(value = "lease_contract_id", columnType = "string", nullable = true, comment = "租赁合同的pic_id，如经营场所照片无法体现经营内容时上传")
    private String leaseContractId;

    /** 结算账号开户证明图片的pic_id */
    @MpField(value = "settle_account_certificate_id", columnType = "string", nullable = true, comment = "结算账号开户证明图片的pic_id")
    private String settleAccountCertificateId;

    /** 业务场景证明材料pic_id，如经营场所照片无法体现经营内容时上传 */
    @MpField(value = "buss_support_materials_id", columnType = "string", nullable = true, comment = "业务场景证明材料pic_id，如经营场所照片无法体现经营内容时上传")
    private String bussSupportMaterialsId;

    /** icp备案许可证明或者许可证编码的pic_id */
    @MpField(value = "icp_registration_license_id", columnType = "string", nullable = true, comment = "icp备案许可证明或者许可证编码的pic_id")
    private String icpRegistrationLicenseId;

    /** 行业资质文件类型：1游戏类，2直播类，3小说图书类，4其他 */
    @MpField(value = "industry_qualify_doc_type", columnType = "string", nullable = true, comment = "行业资质文件类型：1游戏类，2直播类，3小说图书类，4其他")
    private String industryQualifyDocType;

    /** 行业资质文件的pic_id */
    @MpField(value = "industry_qualify_doc_license_id", columnType = "string", nullable = true, comment = "行业资质文件的pic_id")
    private String industryQualifyDocLicenseId;

    /** 股东信息 */
    @MpField(value = "shareholder_info_list", columnType = "text", nullable = true, comment = "股东信息")
    private String shareholderInfoList;

    /** 是否短信提醒: 1:是  0:否 */
    @MpField(value = "is_sms", columnType = "string", length = 20, nullable = true, comment = "是否短信提醒: 1:是  0:否")
    private String isSms;

    /** W -> 待补充，I -> 初始，P -> 通过，R -> 拒绝 */
    @MpField(value = "audit_status", columnType = "string", nullable = true, comment = "W -> 待补充，I -> 初始，P -> 通过，R -> 拒绝")
    private String auditStatus;

    /** 审核拒绝原因 */
    @MpField(value = "audit_desc", columnType = "string", length = 500, nullable = true, comment = "审核拒绝原因")
    private String auditDesc;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
