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
 * 结算账户
 */
@Data
@MpTable(value = "adapay_settle_account", comment = "结算账户", indexes = {@MpIndex(name = "idx_member_id", columns = {"member_id"}), @MpIndex(name = "idx_card_id", columns = {"card_id"}, lengths = {64}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_cert_id", columns = {"cert_id"}, lengths = {64}), @MpIndex(name = "idx_settle_account_id", columns = {"settle_account_id"})})
public class AdapaySettleAccount {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 应用app_id */
    @MpField(value = "app_id", columnType = "string", length = 100, nullable = true, comment = "应用app_id")
    private String appId = "";

    /** 由 Adapay 生成的结算账户对象 id */
    @MpField(value = "settle_account_id", columnType = "string", length = 100, nullable = true, comment = "由 Adapay 生成的结算账户对象 id")
    private String settleAccountId = "";

    /** member_id */
    @MpField(value = "member_id", columnType = "bigint", comment = "member_id")
    private Long memberId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 目前仅支持：bank_account（银行卡） */
    @MpField(value = "channel", columnType = "string", length = 50, nullable = true, comment = "目前仅支持：bank_account（银行卡）", defaultValue = "bank_account")
    private String channel = "bank_account";

    /** 银行卡号，如果需要自动开结算账户，本字段必填 */
    @MpField(value = "card_id", columnType = "string", length = 255, nullable = true, comment = "银行卡号，如果需要自动开结算账户，本字段必填")
    private String cardId = "";

    /** 银行卡对应的户名，如果需要自动开结算账户，本字段必填；若银行账户类型是对公，必须与企业名称一致 */
    @MpField(value = "card_name", columnType = "string", length = 500, nullable = true, comment = "银行卡对应的户名，如果需要自动开结算账户，本字段必填；若银行账户类型是对公，必须与企业名称一致")
    private String cardName = "";

    /** 证件号 */
    @MpField(value = "cert_id", columnType = "string", length = 255, nullable = true, comment = "证件号")
    private String certId = "";

    /** 证件类型：00-身份证（仅支持此值） */
    @MpField(value = "cert_type", columnType = "string", length = 10, nullable = true, comment = "证件类型，仅支持：00-身份证", defaultValue = "00")
    private String certType = "00";

    /** 用户手机号 */
    @MpField(value = "tel_no", columnType = "string", length = 255, nullable = true, comment = "用户手机号")
    private String telNo = "";

    /** 银行编码，详见附录 银行代码，银行账户类型对公时，必填 */
    @MpField(value = "bank_code", columnType = "string", length = 30, nullable = true, comment = "银行编码，详见附录 银行代码，银行账户类型对公时，必填")
    private String bankCode = "";

    /** 开户银行名称 */
    @MpField(value = "bank_name", columnType = "string", length = 30, nullable = true, comment = "开户银行名称")
    private String bankName = "";

    /** 银行账户类型：1-对公；2-对私。如需自动开结算账户则必填 */
    @MpField(value = "bank_acct_type", columnType = "string", length = 10, comment = "银行账户类型：1-对公；2-对私，如果需要自动开结算账户，本字段必填")
    private String bankAcctType = "";

    /** 省份编码, 银行账户类型为对公时，必填 */
    @MpField(value = "prov_code", columnType = "string", length = 10, nullable = true, comment = "省份编码, 银行账户类型为对公时，必填")
    private String provCode = "";

    /** 地区编码, 银行账户类型为对公时，必填 */
    @MpField(value = "area_code", columnType = "string", length = 10, nullable = true, comment = "地区编码, 银行账户类型为对公时，必填")
    private String areaCode = "";

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
