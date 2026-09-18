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

/** 商品促销和订单关联表 */
@Data
@MpTable(value = "orders_rel_promotions", comment = "商品促销和订单关联表", indexes = {@MpIndex(name = "idx_moid", columns = {"moid"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_activity_id", columns = {"activity_id"}), @MpIndex(name = "idx_shop_id", columns = {"shop_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class OrderPromotions {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 主订单id */
    @MpField(value = "moid", columnType = "bigint", length = 64, comment = "主订单id")
    private Long moid;

    /** 子订单id */
    @MpField(value = "coid", columnType = "bigint", length = 64, comment = "子订单id")
    private Long coid;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 订单类型 */
    @MpField(value = "order_type", columnType = "string", comment = "订单类型", defaultValue = "normal")
    private String orderType = "normal";

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", nullable = true, comment = "商品名称")
    private String itemName;

    /** 商品类型 */
    @MpField(value = "item_type", columnType = "string", nullable = true, comment = "商品类型", defaultValue = "normal")
    private String itemType = "normal";

    /** 活动id */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动id")
    private Long activityId;

    /** 活动名称 */
    @MpField(value = "activity_name", columnType = "string", nullable = true, comment = "活动名称")
    private String activityName;

    /** 活动类型 */
    @MpField(value = "activity_type", columnType = "string", comment = "活动类型")
    private String activityType;

    /** 活动标签 */
    @MpField(value = "activity_tag", columnType = "string", nullable = true, comment = "活动标签")
    private String activityTag;

    /** 活动描述 */
    @MpField(value = "activity_desc", columnType = "text", nullable = true, comment = "活动描述")
    private String activityDesc;

    /** 店铺id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "店铺id", defaultValue = "0")
    private Long shopId = 0L;

    /** 店铺类型 */
    @MpField(value = "shop_type", columnType = "string", nullable = true, comment = "店铺类型", defaultValue = "shop")
    private String shopType = "shop";

    /** 促销应用状态,valid:有效, invalid:失效 */
    @MpField(value = "status", columnType = "string", nullable = true, comment = "促销应用状态,valid:有效, invalid:失效", defaultValue = "valid")
    private String status = "valid";

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
