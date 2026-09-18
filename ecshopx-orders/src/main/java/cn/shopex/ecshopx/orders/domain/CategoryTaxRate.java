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

/** 分类税率表 */
@Data
@MpTable(value = "category_tax_rate", comment = "分类税率表")
public class CategoryTaxRate {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "自增id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 销售方ID */
    @MpField(value = "sales_party_id", columnType = "string", length = 64, comment = "销售方ID")
    private String salesPartyId;

    /** 税率分类：ALL/SPECIFIED */
    @MpField(value = "tax_rate_type", columnType = "string", length = 32, comment = "税率分类：ALL/SPECIFIED")
    private String taxRateType;

    /** 分类ID数组，json存储 */
    @MpField(value = "category_ids", columnType = "text", nullable = true, comment = "分类ID数组，json存储")
    private String categoryIds;

    /** 发票税率，如13% */
    @MpField(value = "invoice_tax_rate", columnType = "string", length = 16, comment = "发票税率，如13%")
    private String invoiceTaxRate;

    /** 创建时间 */
    @MpField(value = "created_at", columnType = "integer", comment = "创建时间")
    private Integer createdAt;

    /** 更新时间 */
    @MpField(value = "updated_at", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updatedAt;
}
