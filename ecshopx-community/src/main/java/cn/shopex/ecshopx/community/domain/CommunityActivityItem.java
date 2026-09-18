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

package cn.shopex.ecshopx.community.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 社区拼团活动商品表
 */
@Data
@MpTable(value = "community_activity_item", comment = "社区拼团活动商品表", indexes = {@MpIndex(name = "ix_activity_id", columns = {"activity_id"}), @MpIndex(name = "ix_item_id", columns = {"item_id"})})
public class CommunityActivityItem {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 活动ID */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动ID")
    private Long activityId;

    /** 商品 spu ID */
    @MpField(value = "goods_id", columnType = "bigint", comment = "商品spu ID")
    private Long goodsId;

    /** 商品 sku ID */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品sku ID")
    private Long itemId;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", comment = "商品名称")
    private String itemName;

    /** 商品规格描述，可为空 */
    @MpField(value = "item_spec_desc", columnType = "string", nullable = true, comment = "商品规格描述")
    private String itemSpecDesc;

    /** 商品描述，可为空，最长 255 */
    @MpField(value = "item_brief", columnType = "string", length = 255, nullable = true, comment = "商品描述")
    private String itemBrief;

    /** 商品图片，可为空 */
    @MpField(value = "item_pics", columnType = "text", nullable = true, comment = "商品图片")
    private String itemPics;

    /** 销售价，单位为分 */
    @MpField(value = "price", columnType = "integer", comment = "销售价,单位为‘分’")
    private Integer price;

    /** 成本价，单位为分，可为空，默认 0 */
    @MpField(value = "cost_price", columnType = "integer", nullable = true, comment = "成本价,单位为‘分’", defaultValue = "0")
    private Integer costPrice = 0;

    /** 原价，单位为分 */
    @MpField(value = "market_price", columnType = "integer", comment = "原价,单位为‘分’")
    private Integer marketPrice;

    /** 库存数量 */
    @MpField(value = "store", columnType = "integer", comment = "库存数量")
    private Integer store;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created_at", columnType = "integer")
    private Integer createdAt;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated_at", columnType = "integer", nullable = true)
    private Integer updatedAt;
}
