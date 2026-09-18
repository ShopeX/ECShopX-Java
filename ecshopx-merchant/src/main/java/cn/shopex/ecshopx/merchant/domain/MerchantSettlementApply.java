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

package cn.shopex.ecshopx.merchant.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 商户入驻申请表 */
@Data
@MpTable(value = "merchant_settlement_apply", comment = "商户入驻申请表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_audit_status", columns = {"audit_status"}), @MpIndex(name = "idx_settled_type", columns = {"settled_type"}), @MpIndex(name = "idx_source", columns = {"source"})})
public class MerchantSettlementApply {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", length = 255, comment = "手机号")
    private String mobile;

    /** 是否同意入驻协议 */
    @MpField(value = "is_agree_agreement", columnType = "boolean", comment = "是否同意入驻协议", defaultValue = "False")
    private boolean isAgreeAgreement = false;

    /** 商户类型ID */
    @MpField(value = "merchant_type_id", columnType = "bigint", comment = "商户类型ID", defaultValue = "0")
    private long merchantTypeId = 0L;

    /** 入驻类型。enterprise:企业;soletrader:个体户 */
    @MpField(value = "settled_type", columnType = "string", nullable = true, comment = "入驻类型。enterprise:企业;soletrader:个体户")
    private String settledType = "";

    /** 商户名称 */
    @MpField(value = "merchant_name", columnType = "string", nullable = true, comment = "商户名称")
    private String merchantName;

    /** 统一社会信用代码 */
    @MpField(value = "social_credit_code_id", columnType = "string", nullable = true, comment = "统一社会信用代码")
    private String socialCreditCodeId;

    @MpField(value = "province", columnType = "string", length = 50, nullable = true)
    private String province;

    @MpField(value = "city", columnType = "string", length = 50, nullable = true)
    private String city;

    @MpField(value = "area", columnType = "string", length = 50, nullable = true)
    private String area;

    /** 国家行政区划编码组合，逗号隔开 */
    @MpField(value = "regions_id", columnType = "text", nullable = true, comment = "国家行政区划编码组合，逗号隔开")
    private String regionsId;

    /** 详细地址 */
    @MpField(value = "address", columnType = "string", nullable = true, comment = "详细地址")
    private String address;

    /** 法人姓名 */
    @MpField(value = "legal_name", columnType = "string", length = 50, nullable = true, comment = "法人姓名")
    private String legalName;

    /** 法人身份证号码 */
    @MpField(value = "legal_cert_id", columnType = "string", length = 255, nullable = true, comment = "法人身份证号码")
    private String legalCertId = "";

    /** 法人手机号码 */
    @MpField(value = "legal_mobile", columnType = "string", nullable = true, comment = "法人手机号码")
    private String legalMobile;

    /**
     * 银行账户类型：1-对公；2-对私。1 对公；2 对私
     */
    @MpField(value = "bank_acct_type", columnType = "string", length = 10, nullable = true, comment = "银行账户类型：1-对公；2-对私")
    private String bankAcctType = "";

    /** 结算银行卡号 */
    @MpField(value = "card_id_mask", columnType = "string", nullable = true, comment = "结算银行卡号")
    private String cardIdMask;

    /** 结算银行卡所属银行名称 */
    @MpField(value = "bank_name", columnType = "string", length = 100, nullable = true, comment = "结算银行卡所属银行名称")
    private String bankName;

    /** 银行预留手机号 */
    @MpField(value = "bank_mobile", columnType = "string", nullable = true, comment = "银行预留手机号")
    private String bankMobile;

    /** 营业执照图片url */
    @MpField(value = "license_url", columnType = "string", nullable = true, comment = "营业执照图片url")
    private String licenseUrl;

    /** 法人手持身份证正面url */
    @MpField(value = "legal_certid_front_url", columnType = "string", nullable = true, comment = "法人手持身份证正面url")
    private String legalCertidFrontUrl;

    /** 法人手持身份证反面url */
    @MpField(value = "legal_cert_id_back_url", columnType = "string", nullable = true, comment = "法人手持身份证反面url")
    private String legalCertIdBackUrl;

    /** 结算银行卡正面url */
    @MpField(value = "bank_card_front_url", columnType = "string", nullable = true, comment = "结算银行卡正面url")
    private String bankCardFrontUrl;

    /** 审核状态：1:审核中 2:审核成功 3:审核驳回 */
    @MpField(value = "audit_status", columnType = "string", length = 10, nullable = true, comment = "审核状态：1:审核中 2:审核成功 3:审核驳回", defaultValue = "1")
    private String auditStatus = "1";

    /** 审核备注 */
    @MpField(value = "audit_memo", columnType = "string", length = 500, nullable = true, comment = "审核备注")
    private String auditMemo;

    /** 来源 admin:平台管理员;h5:h5入驻; */
    @MpField(value = "source", columnType = "string", length = 30, nullable = true, comment = "来源 admin:平台管理员;h5:h5入驻;", defaultValue = "h5")
    private String source = "h5";

    /** 是否需要平台审核商品 0:不需要 1:需要 */
    @MpField(value = "audit_goods", columnType = "boolean", comment = "是否需要平台审核商品 0:不需要 1:需要", defaultValue = "1")
    private boolean auditGoods = true;

    /** 禁用 */
    @MpField(value = "disabled", columnType = "boolean", comment = "禁用", defaultValue = "0")
    private boolean disabled = false;

    @MpField(value = "created", columnType = "integer")
    private int created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
