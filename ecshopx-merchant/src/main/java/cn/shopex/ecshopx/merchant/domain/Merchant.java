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

/** 商户表 */
@Data
@MpTable(value = "merchant", comment = "商户表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_settlement_apply_id", columns = {"settlement_apply_id"}), @MpIndex(name = "idx_legal_mobile", columns = {"legal_mobile"}, lengths = {64})})
public class Merchant {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;

    /** 入驻申请id */
    @MpField(value = "settlement_apply_id", columnType = "bigint", nullable = true, comment = "入驻申请id")
    private Long settlementApplyId;

    /** 商户名称 */
    @MpField(value = "merchant_name", columnType = "string", nullable = true, comment = "商户名称")
    private String merchantName;

    /** 商户类型ID */
    @MpField(value = "merchant_type_id", columnType = "bigint", comment = "商户类型ID")
    private long merchantTypeId;

    /** 商户入驻类型。enterprise:企业;soletrader:个体户 */
    @MpField(value = "settled_type", columnType = "string", comment = "商户入驻类型。enterprise:企业;soletrader:个体户")
    private String settledType = "";

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

    /** 联系人 */
    @MpField(value = "legal_name", columnType = "string", length = 500, comment = "联系人")
    private String legalName;

    /** 法人身份证号码 */
    @MpField(value = "legal_cert_id", columnType = "string", length = 255, comment = "法人身份证号码")
    private String legalCertId = "";

    /** 法人手机号码 */
    @MpField(value = "legal_mobile", columnType = "string", nullable = true, comment = "法人手机号码")
    private String legalMobile;

    /** 联系邮箱 */
    @MpField(value = "email", columnType = "string", nullable = true, comment = "联系邮箱")
    private String email;

    /**
     * 银行账户类型：1-对公；2-对私。1 对公；2 对私
     */
    @MpField(value = "bank_acct_type", columnType = "string", length = 10, comment = "银行账户类型：1-对公；2-对私")
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

    /** 合同url */
    @MpField(value = "contract_url", columnType = "string", nullable = true, comment = "合同url")
    private String contractUrl;

    /** 入驻成功发送时间  1:立即 2:商家H5确认入驻协议后 */
    @MpField(value = "settled_succ_sendsms", columnType = "string", length = 10, comment = "入驻成功发送时间  1:立即 2:商家H5确认入驻协议后", defaultValue = "1")
    private String settledSuccSendsms = "1";

    /** 是否需要平台审核商品 0:不需要 1:需要 */
    @MpField(value = "audit_goods", columnType = "boolean", comment = "是否需要平台审核商品 0:不需要 1:需要", defaultValue = "1")
    private boolean auditGoods = true;

    /** 来源 admin:平台管理员;h5:h5入驻; */
    @MpField(value = "source", columnType = "string", length = 30, nullable = true, comment = "来源 admin:平台管理员;h5:h5入驻;", defaultValue = "h5")
    private String source = "h5";

    /** 禁用 */
    @MpField(value = "disabled", columnType = "boolean", comment = "禁用", defaultValue = "0")
    private boolean disabled = false;

    @MpField(value = "created", columnType = "integer")
    private int created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
