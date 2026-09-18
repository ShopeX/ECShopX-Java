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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 发票表 */
@Data
@MpTable(value = "orders_invoices", comment = "发票表", indexes = {@MpIndex(name = "idx_invoice_id", columns = {"invoice_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class OrderInvoices {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", length = 64, comment = "自增id")
    private Long id;

    /** 关联发票表id */
    @MpField(value = "invoice_id", columnType = "bigint", comment = "关联发票表id")
    private Long invoiceId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 开票类型：红 red，蓝 blue */
    @MpField(value = "invoice_type", columnType = "string", length = 20, comment = "开票类型，红red,和蓝blue")
    private String invoiceType;

    /** 发票号码 */
    @MpField(value = "invoice_no", columnType = "string", nullable = true, comment = "发票号码")
    private String invoiceNo;

    /** 发票代码 */
    @MpField(value = "invoice_code", columnType = "string", nullable = true, comment = "发票代码")
    private String invoiceCode;

    /** 开票时间 */
    @MpField(value = "invoice_time", columnType = "string", nullable = true, comment = "开票时间")
    private String invoiceTime;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
