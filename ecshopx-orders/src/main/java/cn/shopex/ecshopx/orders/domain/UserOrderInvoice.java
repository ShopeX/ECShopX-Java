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

/** 用户订单发票 */
@Data
@MpTable(value = "orders_user_invoice", comment = "订单发票", indexes = {@MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"})})
public class UserOrderInvoice {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", length = 64, comment = "自增id")
    private Long id;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", length = 64, comment = "订单号")
    private String orderId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 是否开票 */
    @MpField(value = "status", columnType = "integer", comment = "是否开票", defaultValue = "0")
    private Integer status;

    /** 发票信息 */
    @MpField(value = "invoice", columnType = "text", nullable = true, comment = "发票信息")
    private String invoice;
}
