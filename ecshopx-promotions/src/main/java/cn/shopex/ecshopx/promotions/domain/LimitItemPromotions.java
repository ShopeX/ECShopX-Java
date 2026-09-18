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

/** 限购活动商品表 */
@Data
@MpTable(value = "promotions_limit_item", comment = "限购活动商品表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "idx_item_id", columns = {"item_id"}), @MpIndex(name = "idx_unique_item", columns = {"company_id", "distributor_id", "item_id"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"limit_id", "distributor_id", "item_id"})})
public class LimitItemPromotions {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 限购活动规则id */
    @MpField(value = "limit_id", columnType = "bigint", comment = "限购活动规则id")
    private Long limitId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 商品 */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品")
    private Long itemId;

    /** 限购数量 */
    @MpField(value = "limit_num", columnType = "bigint", comment = "限购数量")
    private Long limitNum;

    /**
     * 活动商品类型: normal:实体类商品,service:服务类商品,tag:标签,category:商品主类目,brand:品牌
     */
    @MpField(value = "item_type", columnType = "string", comment = "活动商品类型: normal:实体类商品,service:服务类商品,tag:标签,category:商品主类目,brand:品牌", defaultValue = "normal")
    private String itemType = "normal";

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", comment = "商品名称")
    private String itemName;

    /** 商品图片 */
    @MpField(value = "pics", columnType = "text", comment = "商品图片")
    private String pics;

    /** 商品原价 */
    @MpField(value = "price", columnType = "integer", comment = "商品原价")
    private Integer price;

    /** 产品规格描述 */
    @MpField(value = "item_spec_desc", columnType = "string", nullable = true, comment = "产品规格描述")
    private String itemSpecDesc = "";

    /** 起始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "起始时间")
    private Integer startTime;

    /** 截止时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "截止时间")
    private Integer endTime;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
