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

/** 订单发货单明细 */
@Data
@MpTable(value = "orders_delivery_items", comment = "订单发货单商品表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_orders_delivery_id", columns = {"orders_delivery_id"})})
public class OrdersDeliveryItems {

    /** 发货明细主键 */
    @MpId(value = "orders_delivery_items_id", type = IdType.AUTO, columnType = "bigint", comment = "orders_delivery_items_id")
    private Long ordersDeliveryItemsId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 订单id */
    @MpField(value = "order_id", columnType = "bigint", comment = "订单id")
    private Long orderId;

    /** 订单商品行id */
    @MpField(value = "order_items_id", columnType = "bigint", comment = "订单items表id")
    private Long orderItemsId;

    /** 订单发货单id */
    @MpField(value = "orders_delivery_id", columnType = "bigint", comment = "订单发货单id")
    private Long ordersDeliveryId;

    /** 产品id */
    @MpField(value = "goods_id", columnType = "bigint", comment = "产品id")
    private Long goodsId;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 发货数量 */
    @MpField(value = "num", columnType = "integer", comment = "发货数量")
    private Integer num;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", comment = "商品名称")
    private String itemName;

    /** 商品图片 */
    @MpField(value = "pic", columnType = "string", comment = "商品图片")
    private String pic;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
