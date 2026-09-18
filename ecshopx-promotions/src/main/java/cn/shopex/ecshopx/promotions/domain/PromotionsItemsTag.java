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

package cn.shopex.ecshopx.promotions.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 商品的促销标签表 */
@Data
@MpTable(value = "promotions_items_tag", comment = "商品的促销标签表", indexes = {@MpIndex(name = "ix_tag_type", columns = {"tag_type"}), @MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class PromotionsItemsTag {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "自增id")
    private Long id;

    /** 关联营销id */
    @MpField(value = "promotion_id", columnType = "bigint", comment = "关联营销id")
    private Long promotionId;

    /** 关联对象id(商品ID,标签ID，品牌ID等) */
    @MpField(value = "item_id", columnType = "bigint", comment = "关联对象id(商品ID,标签ID，品牌ID等)", defaultValue = "0")
    private Long itemId = 0L;

    /** 关联商品id */
    @MpField(value = "goods_id", columnType = "bigint", nullable = true, comment = "关联商品id", defaultValue = "0")
    private Long goodsId = 0L;

    /** 标签类型: full_discount:满折,full_minus:满减,full_gift:满赠, seckill: 秒杀, */
    @MpField(value = "tag_type", columnType = "string", comment = "标签类型: full_discount:满折,full_minus:满减,full_gift:满赠, seckill: 秒杀,")
    private String tagType;

    /** 商品活动价 */
    @MpField(value = "activity_price", columnType = "bigint", comment = "商品活动价", defaultValue = "0")
    private Long activityPrice = 0L;

    /**
     * 活动对象类型: normal:实体类商品,service:服务类商品,tag:标签,category:商品主类目,brand:品牌
     */
    @MpField(value = "item_type", columnType = "string", comment = "活动对象类型: normal:实体类商品,service:服务类商品,tag:标签,category:商品主类目,brand:品牌", defaultValue = "normal")
    private String itemType = "normal";

    /** 活动开始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "活动开始时间")
    private Integer startTime;

    /** 活动结束时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "活动结束时间")
    private Integer endTime;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 是否全部商品,1:全部商品，2:非全部商品 */
    @MpField(value = "is_all_items", columnType = "bigint", comment = "是否全部商品,1:全部商品，2:非全部商品", defaultValue = "2")
    private Long isAllItems = 2L;
}
