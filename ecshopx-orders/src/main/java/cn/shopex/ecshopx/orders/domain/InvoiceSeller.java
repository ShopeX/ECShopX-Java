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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 发票销售方表 */
@Data
@MpTable(value = "invoice_seller", comment = "发票销售方表")
public class InvoiceSeller {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "自增id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 开票人 */
    @MpField(value = "seller_name", columnType = "string", length = 64, comment = "开票人")
    private String sellerName;

    /** 收款人 */
    @MpField(value = "payee", columnType = "string", length = 64, comment = "收款人")
    private String payee;

    /** 复核人 */
    @MpField(value = "reviewer", columnType = "string", length = 64, comment = "复核人")
    private String reviewer;

    /** 销售方名称 */
    @MpField(value = "seller_company_name", columnType = "string", length = 128, comment = "销售方名称")
    private String sellerCompanyName;

    /** 销售方税号 */
    @MpField(value = "seller_tax_no", columnType = "string", length = 32, comment = "销售方税号")
    private String sellerTaxNo;

    /** 销售方开户行 */
    @MpField(value = "seller_bank_name", columnType = "string", length = 128, comment = "销售方开户行")
    private String sellerBankName;

    /** 销售方银行账号 */
    @MpField(value = "seller_bank_account", columnType = "string", length = 64, comment = "销售方银行账号")
    private String sellerBankAccount;

    /** 销售方电话 */
    @MpField(value = "seller_phone", columnType = "string", length = 32, comment = "销售方电话")
    private String sellerPhone;

    /** 销售方地址 */
    @MpField(value = "seller_address", columnType = "string", length = 255, comment = "销售方地址")
    private String sellerAddress;

    /** 创建时间 */
    @MpField(value = "created_at", columnType = "integer", comment = "创建时间")
    private Integer createdAt;

    /** 更新时间 */
    @MpField(value = "updated_at", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updatedAt;
}
