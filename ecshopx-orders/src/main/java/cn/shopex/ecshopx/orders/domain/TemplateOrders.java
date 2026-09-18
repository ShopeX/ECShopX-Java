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

/** 小程序开通表 */
@Data
@MpTable(value = "template_orders", comment = "小程序开通表")
public class TemplateOrders {

    /** 小程序模版开通ID */
    @MpId(value = "template_orders_id", type = IdType.AUTO, columnType = "bigint", comment = "小程序模版开通ID")
    private Long templateOrdersId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** operator_id */
    @MpField(value = "operator_id", columnType = "bigint", comment = "operator_id")
    private Long operatorId;

    /** 模版名称 */
    @MpField(value = "template_name", columnType = "string", comment = "模版名称")
    private String templateName;

    /** 购买模版金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "string", comment = "购买模版金额，以分为单位")
    private String totalFee;

    /** 订单状态。可选值有 DONE—订单完成；NOTPAY—未支付；CANCEL—已取消 */
    @MpField(value = "order_status", columnType = "string", comment = "订单状态。可选值有 DONE—订单完成；NOTPAY—未支付；CANCEL—已取消")
    private String orderStatus;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;
}
