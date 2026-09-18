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
 * 企业用户对象
 */
@Data
@MpTable(value = "adapay_corp_member", comment = "企业用户对象", indexes = {@MpIndex(name = "idx_order_no", columns = {"order_no"}), @MpIndex(name = "idx_name", columns = {"name"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_card_no", columns = {"card_no"}, lengths = {64}), @MpIndex(name = "idx_member_id", columns = {"member_id"})})
public class AdapayCorpMember {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 应用app_id */
    @MpField(value = "app_id", columnType = "string", length = 100, comment = "应用app_id")
    private String appId = "";

    /** 请求订单号 */
    @MpField(value = "order_no", columnType = "string", length = 64, comment = "请求订单号")
    private String orderNo = "";

    /** member_id */
    @MpField(value = "member_id", columnType = "bigint", comment = "member_id")
    private Long memberId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 经销商id */
    @MpField(value = "dealer_id", columnType = "bigint", nullable = true, comment = "经销商id", defaultValue = "0")
    private Long dealerId = 0L;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 操作者id */
    @MpField(value = "operator_id", columnType = "integer", nullable = true, comment = "操作者id", defaultValue = "0")
    private Integer operatorId = 0;

    /** 企业名称 */
    @MpField(value = "name", columnType = "string", length = 100, comment = "企业名称")
    private String name = "";

    /** 省份编码 */
    @MpField(value = "prov_code", columnType = "string", length = 10, nullable = true, comment = "省份编码")
    private String provCode = "";

    /** 地区编码 */
    @MpField(value = "area_code", columnType = "string", length = 10, comment = "地区编码")
    private String areaCode = "";

    /** 统一社会信用码 */
    @MpField(value = "social_credit_code", columnType = "string", length = 32, comment = "统一社会信用码")
    private String socialCreditCode = "";

    /** 统一社会信用证有效期 */
    @MpField(value = "social_credit_code_expires", columnType = "string", length = 32, comment = "统一社会信用证有效期")
    private String socialCreditCodeExpires = "";

    /** 经营范围 */
    @MpField(value = "business_scope", columnType = "string", length = 800, comment = "经营范围")
    private String businessScope = "";

    /** 法人姓名 */
    @MpField(value = "legal_person", columnType = "string", length = 500, comment = "法人姓名")
    private String legalPerson = "";

    /** 法人身份证号码 */
    @MpField(value = "legal_cert_id", columnType = "string", length = 255, comment = "法人身份证号码")
    private String legalCertId = "";

    /** 法人身份证有效期 */
    @MpField(value = "legal_cert_id_expires", columnType = "string", length = 16, comment = "法人身份证有效期")
    private String legalCertIdExpires = "";

    /** 法人手机号 */
    @MpField(value = "legal_mp", columnType = "string", length = 255, comment = "法人手机号")
    private String legalMp = "";

    /** 企业地址 */
    @MpField(value = "address", columnType = "string", length = 300, comment = "企业地址")
    private String address = "";

    /** 邮编 */
    @MpField(value = "zip_code", columnType = "string", length = 10, nullable = true, comment = "邮编")
    private String zipCode = "";

    /** 企业电话 */
    @MpField(value = "telphone", columnType = "string", length = 30, nullable = true, comment = "企业电话")
    private String telphone = "";

    /** 企业邮箱 */
    @MpField(value = "email", columnType = "string", length = 100, nullable = true, comment = "企业邮箱")
    private String email = "";

    /** 上传附件 */
    @MpField(value = "attach_file", columnType = "string", length = 300, nullable = true, comment = "上传附件")
    private String attachFile = "";

    /** 附件文件名 */
    @MpField(value = "attach_file_name", columnType = "string", length = 300, nullable = true, comment = "附件文件名")
    private String attachFileName = "";

    /** 经销商确认函附件 */
    @MpField(value = "confirm_letter_file", columnType = "string", length = 300, nullable = true, comment = "经销商确认函附件")
    private String confirmLetterFile = "";

    /** 经销商确认函附件文件名 */
    @MpField(value = "confirm_letter_file_name", columnType = "string", length = 300, nullable = true, comment = "经销商确认函附件文件名")
    private String confirmLetterFileName = "";

    /** 银行代码，如果需要自动开结算账户，本字段必填 */
    @MpField(value = "bank_code", columnType = "string", length = 30, nullable = true, comment = "银行代码，如果需要自动开结算账户，本字段必填")
    private String bankCode = "";

    /** 银行账户类型：1-对公；2-对私。如需自动开结算账户则必填 */
    @MpField(value = "bank_acct_type", columnType = "string", length = 10, comment = "银行账户类型：1-对公；2-对私，如果需要自动开结算账户，本字段必填")
    private String bankAcctType = "";

    /** 银行卡号，如果需要自动开结算账户，本字段必填 */
    @MpField(value = "card_no", columnType = "string", length = 255, comment = "银行卡号，如果需要自动开结算账户，本字段必填")
    private String cardNo = "";

    /** 银行卡对应的户名，如果需要自动开结算账户，本字段必填；若银行账户类型是对公，必须与企业名称一致 */
    @MpField(value = "card_name", columnType = "string", length = 100, comment = "银行卡对应的户名，如果需要自动开结算账户，本字段必填；若银行账户类型是对公，必须与企业名称一致")
    private String cardName = "";

    /** 审核状态：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功 */
    @MpField(value = "audit_state", columnType = "string", length = 50, nullable = true, comment = "审核状态，状态包括：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功")
    private String auditState = "";

    /** 审核结果描述 */
    @MpField(value = "audit_desc", columnType = "string", length = 200, nullable = true, comment = "审核结果描述")
    private String auditDesc = "";

    /** 当前交易状态：pending-处理中；succeeded-成功；failed-失败 */
    @MpField(value = "status", columnType = "string", length = 50, nullable = true, comment = "当前交易状态")
    private String status = "";

    /** 错误描述 */
    @MpField(value = "error_info", columnType = "string", length = 500, nullable = true, comment = "错误描述")
    private String errorInfo = "";

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
