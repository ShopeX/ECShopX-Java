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

/** 订单发票日志表 */
@Data
@MpTable(value = "orders_invoice_log", comment = "订单发票日志表", indexes = {@MpIndex(name = "idx_invoice_id", columns = {"invoice_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_operator_id", columns = {"operator_id"})})
public class OrderInvoiceLog {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", length = 64, comment = "自增id")
    private Long id;

    /** 关联发票表id */
    @MpField(value = "invoice_id", columnType = "bigint", comment = "关联发票表id")
    private Long invoiceId;

    /** 操作类型 */
    @MpField(value = "operator_type", columnType = "string", length = 20, comment = "操作类型")
    private String operatorType;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 操作人id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "操作人id")
    private Long operatorId;

    /** 操作内容 */
    @MpField(value = "operator_content", columnType = "json_array", nullable = true, comment = "操作内容")
    private String operatorContent;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
