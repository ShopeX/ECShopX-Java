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

/** 订单流程记录表 */
@Data
@MpTable(value = "orders_process_log", comment = "订单流程记录表", indexes = {@MpIndex(name = "idx_order_id", columns = {"order_id"})})
public class OrderProcessLog {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 订单id */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单id")
    private Long orderId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "integer", comment = "供应商id", defaultValue = "0")
    private Integer supplierId;

    /**
     * 操作类型：用户 user，导购 salesperson，管理员 admin，系统 system
     */
    @MpField(value = "operator_type", columnType = "string", length = 20, comment = "操作类型 用户:user 导购:salesperon 管理员:admin 系统:system")
    private String operatorType;

    /** 操作员id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "操作员id", defaultValue = "0")
    private Long operatorId;

    /** 操作员名字 */
    @MpField(value = "operator_name", columnType = "string", nullable = true, comment = "操作员名字")
    private String operatorName;

    /** 订单操作备注 */
    @MpField(value = "remarks", columnType = "string", length = 30, comment = "订单操作备注")
    private String remarks;

    /** 订单操作详情 */
    @MpField(value = "detail", columnType = "text", comment = "订单操作detail")
    private String detail;

    /** 提交参数 */
    @MpField(value = "params", columnType = "json_array", nullable = true, comment = "提交参数")
    private String params;

    /** C端是否可见 */
    @MpField(value = "is_show", columnType = "boolean", nullable = true, comment = "C端是否可见", defaultValue = "False")
    private Boolean isShow;

    /** 图片记录 */
    @MpField(value = "pics", columnType = "json_array", nullable = true, comment = "图片记录")
    private String pics;

    /** 订单发货备注 */
    @MpField(value = "delivery_remark", columnType = "string", nullable = true, comment = "订单发货备注")
    private String deliveryRemark;

    /** 订单操作时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单操作时间")
    private Integer createTime;

    /** 订单操作 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单操作")
    private Integer updateTime;
}
