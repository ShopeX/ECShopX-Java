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
 * adapay开户进件
 */
@Data
@MpTable(value = "adapay_merchant_entry", comment = "adapay开户进件", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_request_id", columns = {"request_id"})})
public class AdapayMerchantEntry {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 请求ID */
    @MpField(value = "request_id", columnType = "string", length = 100, comment = "请求ID")
    private String requestId;

    /** 注册手机号 */
    @MpField(value = "usr_phone", columnType = "string", length = 255, comment = "注册手机号")
    private String usrPhone;

    /** 联系人姓名 */
    @MpField(value = "cont_name", columnType = "string", length = 500, comment = "联系人姓名")
    private String contName;

    /** 联系人手机号码 */
    @MpField(value = "cont_phone", columnType = "string", length = 255, comment = "联系人手机号码")
    private String contPhone;

    /** 电子邮箱 */
    @MpField(value = "customer_email", columnType = "string", length = 100, comment = "电子邮箱")
    private String customerEmail;

    /** 商户名，小微商户填负责人姓名 */
    @MpField(value = "mer_name", columnType = "string", length = 50, comment = "商户名，小微商户填负责人姓名")
    private String merName;

    /** 商户名简称 */
    @MpField(value = "mer_short_name", columnType = "string", length = 50, comment = "商户名简称")
    private String merShortName;

    /** 营业执照编码，如三证合一传三证合一码，企业时必填 */
    @MpField(value = "license_code", columnType = "string", length = 50, nullable = true, comment = "营业执照编码，如三证合一传三证合一码，企业时必填")
    private String licenseCode;

    /** 注册地址 */
    @MpField(value = "reg_addr", columnType = "string", length = 100, comment = "注册地址")
    private String regAddr;

    /** 经营地址 */
    @MpField(value = "cust_addr", columnType = "string", length = 100, comment = "经营地址")
    private String custAddr;

    /** 商户电话 */
    @MpField(value = "cust_tel", columnType = "string", length = 255, comment = "商户电话")
    private String custTel;

    /** 商户有效日期（始），格式 YYYYMMDD （若开户企业类商户，必填） */
    @MpField(value = "mer_start_valid_date", columnType = "string", length = 30, nullable = true, comment = "商户有效日期（始），格式 YYYYMMDD （若开户企业类商户，必填）")
    private String merStartValidDate;

    /** 商户有效日期（至），格式 YYYYMMDD（若为长期有效，固定为“20991231”;若开户企业类商户，必填） */
    @MpField(value = "mer_valid_date", columnType = "string", length = 30, nullable = true, comment = "商户有效日期（至），格式 YYYYMMDD（若为长期有效，固定为“20991231”;若开户企业类商户，必填）")
    private String merValidDate;

    /** 法人/负责人 姓名 */
    @MpField(value = "legal_name", columnType = "string", length = 500, comment = "法人/负责人 姓名")
    private String legalName;

    /** 法人/负责人证件类型，0-身份证 */
    @MpField(value = "legal_type", columnType = "string", length = 10, comment = "法人/负责人证件类型，0-身份证")
    private String legalType;

    /** 法人/负责人证件号码 */
    @MpField(value = "legal_idno", columnType = "string", length = 255, comment = "法人/负责人证件号码")
    private String legalIdno;

    /** 法人/负责人手机号 */
    @MpField(value = "legal_mp", columnType = "string", length = 255, comment = "法人/负责人手机号")
    private String legalMp;

    /** 法人/负责人身份证有效期（始），格式 YYYYMMDD */
    @MpField(value = "legal_start_cert_id_expires", columnType = "string", length = 30, comment = "法人/负责人身份证有效期（始），格式 YYYYMMDD")
    private String legalStartCertIdExpires;

    /** 法人/负责人身份证有效期（至），格式 YYYYMMDD */
    @MpField(value = "legal_id_expires", columnType = "string", length = 30, comment = "法人/负责人身份证有效期（至），格式 YYYYMMDD")
    private String legalIdExpires;

    /** 结算银行卡号 */
    @MpField(value = "card_id_mask", columnType = "string", length = 255, comment = "结算银行卡号")
    private String cardIdMask;

    /** 结算银行卡所属银行code */
    @MpField(value = "bank_code", columnType = "string", length = 20, comment = "结算银行卡所属银行code")
    private String bankCode;

    /** 结算银行卡开户姓名 */
    @MpField(value = "card_name", columnType = "string", length = 500, comment = "结算银行卡开户姓名")
    private String cardName;

    /** 结算银行账户类型，1 : 对公， 2 : 对私。小微只能是对私 */
    @MpField(value = "bank_acct_type", columnType = "string", length = 5, comment = "结算银行账户类型，1 : 对公， 2 : 对私。小微只能是对私")
    private String bankAcctType;

    /** 结算银行卡省份编码 */
    @MpField(value = "prov_code", columnType = "string", length = 10, comment = "结算银行卡省份编码")
    private String provCode;

    /** 结算银行卡地区编码 */
    @MpField(value = "area_code", columnType = "string", length = 10, comment = "结算银行卡地区编码")
    private String areaCode;

    /** 商户rsa 公钥 */
    @MpField(value = "rsa_public_key", columnType = "text", nullable = true, comment = "商户rsa 公钥")
    private String rsaPublicKey;

    /** 商户类型：1-企业；2-小微 */
    @MpField(value = "entry_mer_type", columnType = "string", length = 10, comment = "商户类型：1-企业；2-小微")
    private String entryMerType;

    /** 测试API Key */
    @MpField(value = "test_api_key", columnType = "string", nullable = true, comment = "测试API Key")
    private String testApiKey;

    /** 生产API Key */
    @MpField(value = "live_api_key", columnType = "string", nullable = true, comment = "生产API Key")
    private String liveApiKey;

    /** 初始密码 */
    @MpField(value = "login_pwd", columnType = "string", length = 1024, nullable = true, comment = "初始密码")
    private String loginPwd;

    /** 应用ID列表 */
    @MpField(value = "app_id_list", columnType = "text", nullable = true, comment = "应用ID列表")
    private String appIdList;

    /** 合同查看地址 */
    @MpField(value = "sign_view_url", columnType = "string", length = 1024, nullable = true, comment = "合同查看地址")
    private String signViewUrl;

    /** 是否短信提醒: 1:是  0:否 */
    @MpField(value = "is_sms", columnType = "string", length = 20, nullable = true, comment = "是否短信提醒: 1:是  0:否")
    private String isSms;

    /** 接口调用状态，succeeded - 成功 failed - 失败 pending - 处理中 */
    @MpField(value = "status", columnType = "string", length = 20, nullable = true, comment = "接口调用状态，succeeded - 成功 failed - 失败 pending - 处理中")
    private String status;

    /** 错误描述 */
    @MpField(value = "error_msg", columnType = "string", length = 500, nullable = true, comment = "错误描述")
    private String errorMsg;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
