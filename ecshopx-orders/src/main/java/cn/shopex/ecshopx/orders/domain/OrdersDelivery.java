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

/** 订单发货单表 */
@Data
@MpTable(value = "orders_delivery", comment = "订单发货单表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_supplier_id", columns = {"supplier_id"})})
public class OrdersDelivery {

    /** 发货单主键 */
    @MpId(value = "orders_delivery_id", type = IdType.AUTO, columnType = "bigint", comment = "orders_delivery_id")
    private Long ordersDeliveryId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "integer", comment = "供应商id", defaultValue = "0")
    private Integer supplierId = 0;

    /** 订单id */
    @MpField(value = "order_id", columnType = "bigint", comment = "订单id")
    private Long orderId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 快递公司 */
    @MpField(value = "delivery_corp", columnType = "string", comment = "快递公司")
    private String deliveryCorp;

    /** 快递公司名称 */
    @MpField(value = "delivery_corp_name", columnType = "string", comment = "快递公司名称")
    private String deliveryCorpName;

    /** 快递单号 */
    @MpField(value = "delivery_code", columnType = "string", comment = "快递单号")
    private String deliveryCode;

    /** 发货时间 */
    @MpField(value = "delivery_time", columnType = "integer", comment = "发货时间")
    private Integer deliveryTime;

    /** 快递代码来源 */
    @MpField(value = "delivery_corp_source", columnType = "string", nullable = true, comment = "快递代码来源")
    private String deliveryCorpSource;

    /** 收货人手机号 */
    @MpField(value = "receiver_mobile", columnType = "string", nullable = true, comment = "收货人手机号")
    private String receiverMobile;

    /** 订单包裹类型：batch 整单发货，sep 拆单发货 */
    @MpField(value = "package_type", columnType = "string", nullable = true, comment = "订单包裹类型 batch 整单发货  sep拆单发货")
    private String packageType;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 自配送员id */
    @MpField(value = "self_delivery_operator_id", columnType = "bigint", nullable = true, comment = "自配送员id", defaultValue = "0")
    private Long selfDeliveryOperatorId = 0L;

    /** 配送备注 */
    @MpField(value = "delivery_remark", columnType = "string", nullable = true, comment = "配送备注")
    private String deliveryRemark;

    /** 配送图片 */
    @MpField(value = "delivery_pics", columnType = "json_array", nullable = true, comment = "配送图片")
    private String deliveryPics;
}
