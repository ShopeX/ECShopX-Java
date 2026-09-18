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

package cn.shopex.ecshopx.members.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员发票信息表 */
@Data
@MpTable(value = "members_invoices", comment = "会员发票信息表")
public class MembersInvoices {

    /** id */
    @MpId(value = "invoices_id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long invoicesId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 类型 personal 个人 ；corporate 企业 */
    @MpField(value = "invoices_type", columnType = "string", length = 15, comment = "类型 personal 个人 ；corporate 企业 ")
    private String invoicesType;

    /** 名称 */
    @MpField(value = "name", columnType = "string", comment = "名称")
    private String name;

    /** 电话号码 */
    @MpField(value = "telephone", columnType = "string", length = 20, nullable = true, comment = "电话号码")
    private String telephone;

    /** 税号 */
    @MpField(value = "tax_number", columnType = "string", nullable = true, comment = "税号")
    private String taxNumber;

    /** 单位地址 */
    @MpField(value = "business_address", columnType = "string", nullable = true, comment = "单位地址")
    private String businessAddress;

    /** 开户银行 */
    @MpField(value = "bank", columnType = "string", nullable = true, comment = "开户银行")
    private String bank;

    /** 银行账号 */
    @MpField(value = "bank_account", columnType = "string", nullable = true, comment = "银行账号")
    private String bankAccount;

    /** 是否默认 */
    @MpField(value = "is_def", columnType = "boolean", comment = "是否默认", defaultValue = "0")
    private Boolean isDef = false;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
