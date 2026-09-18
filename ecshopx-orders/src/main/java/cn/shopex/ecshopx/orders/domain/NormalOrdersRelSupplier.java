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

/** 实体订单关联供应商 */
@Data
@MpTable(value = "orders_rel_supplier", comment = "实体订单关联供应商", indexes = {@MpIndex(name = "idx_supplier_order_id", columns = {"supplier_id", "order_id"})})
public class NormalOrdersRelSupplier {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "integer", comment = "供应商id")
    private Integer supplierId;

    /** 运费价格，以分为单位 */
    @MpField(value = "freight_fee", columnType = "integer", nullable = true, comment = "运费价格，以分为单位", defaultValue = "0")
    private Integer freightFee = 0;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;
}
