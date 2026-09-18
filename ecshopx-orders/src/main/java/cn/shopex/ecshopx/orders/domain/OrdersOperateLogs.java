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

/** 订单操作日志表 */
@Data
@MpTable(value = "orders_operate_logs", comment = "订单操作日志表", indexes = {@MpIndex(name = "order_id", columns = {"order_id"}), @MpIndex(name = "operator_id", columns = {"operator_id"}), @MpIndex(name = "company_id", columns = {"company_id"})})
public class OrdersOperateLogs {

    /** 订单操作日志主键 */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "订单操作日志")
    private Long id;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 公司Id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司Id")
    private Long companyId;

    /** 操作员Id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "操作员Id")
    private Long operatorId;

    /** 供应商ID */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商ID", defaultValue = "0")
    private Long supplierId = 0L;

    /** 操作员 */
    @MpField(value = "operator", columnType = "string", nullable = true, comment = "操作员")
    private String operator;

    /**
     * 操作角色：buyer 购买者，seller 卖家，shopadmin 平台操作员，system 系统
     */
    @MpField(value = "operator_role", columnType = "string", length = 100, nullable = true, comment = "操作角色,buyer:购买者;seller:卖家;shopadmin:平台操作员;system:系统")
    private String operatorRole;

    /**
     * 操作行为：create 创建，update 修改，payed 支付，delivery 发货，confirm 收货，cancel 取消，refund
     * 退款，reship 退货，exchange 换货，mark 修改备注，finish 完成
     */
    @MpField(value = "behavior", columnType = "string", length = 100, nullable = true, comment = "操作行为,create:创建;update:修改;payed:支付;delivery:发货；confirm:收货；cancel:取消；refund:退款；reship:退货；exchange:换货；mark:修改备注；finish:完成；")
    private String behavior;

    /** 操作内容 */
    @MpField(value = "log_text", columnType = "text", nullable = true, comment = "操作内容")
    private String logText;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
