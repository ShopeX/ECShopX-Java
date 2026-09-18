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

/** 订单发票商品表 */
@Data
@MpTable(value = "orders_invoice_item", comment = "订单发票商品表", indexes = {@MpIndex(name = "idx_invoice_id", columns = {"invoice_id"}), @MpIndex(name = "idx_invoice_apply_bn", columns = {"invoice_apply_bn"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_order_id", columns = {"order_id"})})
public class OrderInvoiceItem {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", length = 64, comment = "自增id")
    private Long id;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 关联发票表id */
    @MpField(value = "invoice_id", columnType = "bigint", comment = "关联发票表id")
    private Long invoiceId;

    /** 发票申请单号 */
    @MpField(value = "invoice_apply_bn", columnType = "string", length = 64, comment = "发票申请单号")
    private String invoiceApplyBn;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", length = 64, comment = "订单号")
    private String orderId;

    /** 运营侧订单ID */
    @MpField(value = "oid", columnType = "string", length = 64, nullable = true, comment = "OMS系统订单ID")
    private String oid;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", length = 255, comment = "商品名称")
    private String itemName;

    /** 商品编码 */
    @MpField(value = "item_bn", columnType = "string", length = 64, nullable = true, comment = "商品编码")
    private String itemBn;

    /** 商品主图 */
    @MpField(value = "main_img", columnType = "string", length = 255, nullable = true, comment = "商品主图")
    private String mainImg;

    /** 商品规格 */
    @MpField(value = "spec_info", columnType = "string", length = 255, nullable = true, comment = "商品规格")
    private String specInfo;

    /** 商品规格描述 */
    @MpField(value = "item_spec_desc", columnType = "text", nullable = true, comment = "商品规格描述")
    private String itemSpecDesc;

    /** 数量 */
    @MpField(value = "num", columnType = "integer", comment = "数量")
    private Integer num;

    /** 金额，以分为单位 */
    @MpField(value = "amount", columnType = "integer", comment = "金额，以分为单位")
    private Integer amount;

    /** 发票税率，如13% */
    @MpField(value = "invoice_tax_rate", columnType = "string", length = 16, nullable = true, comment = "发票税率，如13%")
    private String invoiceTaxRate;

    /** 原始数量 */
    @MpField(value = "original_num", columnType = "integer", nullable = true, comment = "原始数量")
    private Integer originalNum;

    /** 原始金额，以分为单位 */
    @MpField(value = "original_amount", columnType = "integer", nullable = true, comment = "原始金额，以分为单位")
    private Integer originalAmount;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
