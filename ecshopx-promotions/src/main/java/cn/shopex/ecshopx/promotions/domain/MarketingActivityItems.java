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

/** 各种促销活动商品表 */
@Data
@MpTable(value = "promotions_marketing_activity_items", comment = "各种促销活动商品表", indexes = {@MpIndex(name = "ix_marketing_type", columns = {"marketing_type"}), @MpIndex(name = "ix_marketing_item", columns = {"item_id", "marketing_id"}), @MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class MarketingActivityItems {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "自增id")
    private Long id;

    /** 关联营销id */
    @MpField(value = "marketing_id", columnType = "bigint", comment = "关联营销id")
    private Long marketingId;

    /** 关联id(商品ID，标签ID，品牌ID等) */
    @MpField(value = "item_id", columnType = "bigint", comment = "关联id(商品ID，标签ID，品牌ID等)")
    private Long itemId;

    /** 关联商品id */
    @MpField(value = "goods_id", columnType = "bigint", nullable = true, comment = "关联商品id")
    private Long goodsId;

    /** 列表页是否显示 */
    @MpField(value = "is_show", columnType = "boolean", comment = "列表页是否显示", defaultValue = "True")
    private Boolean isShow = true;

    /** 商品规格描述 */
    @MpField(value = "item_spec_desc", columnType = "string", nullable = true, comment = "商品规格描述")
    private String itemSpecDesc;

    /**
     * 营销类型: full_discount:满折,full_minus:满减,full_gift:满赠,self_select:任选优惠,plus_price_buy:加价购,member_preference:会员优先购
     */
    @MpField(value = "marketing_type", columnType = "string", comment = "营销类型: full_discount:满折,full_minus:满减,full_gift:满赠,self_select:任选优惠,plus_price_buy:加价购,member_preference:会员优先购")
    private String marketingType;

    /**
     * 活动商品类型: normal:实体类商品,service:服务类商品,tag:标签,category:商品主类目,brand:品牌
     */
    @MpField(value = "item_type", columnType = "string", comment = "活动商品类型: normal:实体类商品,service:服务类商品,tag:标签,category:商品主类目,brand:品牌", defaultValue = "normal")
    private String itemType = "normal";

    /** 商品标题 */
    @MpField(value = "item_name", columnType = "string", length = 150, comment = "商品标题")
    private String itemName;

    /** 商品价格 */
    @MpField(value = "price", columnType = "integer", comment = "商品价格", defaultValue = "0")
    private Integer price = 0;

    /** 商品简介 */
    @MpField(value = "item_brief", columnType = "string", length = 250, nullable = true, comment = "商品简介")
    private String itemBrief;

    /** 商品图片 */
    @MpField(value = "pics", columnType = "text", nullable = true, comment = "商品图片")
    private String pics;

    /** 促销标签 */
    @MpField(value = "promotion_tag", columnType = "string", length = 15, comment = "促销标签")
    private String promotionTag;

    /** 活动开始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "活动开始时间")
    private Integer startTime;

    /** 活动结束时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "活动结束时间")
    private Integer endTime;

    /** 是否生效中 */
    @MpField(value = "status", columnType = "boolean", comment = "是否生效中", defaultValue = "True")
    private Boolean status = true;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
