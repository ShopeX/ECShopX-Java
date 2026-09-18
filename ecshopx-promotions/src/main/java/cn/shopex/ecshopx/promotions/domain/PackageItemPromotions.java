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

/** 组合促销商品表 */
@Data
@MpTable(value = "promotions_package_item", comment = "组合促销商品表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_item_id", columns = {"item_id"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"package_id", "item_id"})})
public class PackageItemPromotions {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 组合促销规则id */
    @MpField(value = "package_id", columnType = "bigint", comment = "组合促销规则id")
    private Long packageId;

    /** 商品 */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品")
    private Long itemId;

    /** 默认商品id */
    @MpField(value = "default_item_id", columnType = "bigint", comment = "默认商品id")
    private Long defaultItemId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 产品规格描述 */
    @MpField(value = "item_spec_desc", columnType = "string", nullable = true, comment = "产品规格描述")
    private String itemSpecDesc = "";

    /** 商品名称 */
    @MpField(value = "title", columnType = "string", comment = "商品名称")
    private String title;

    /** 商品图片 */
    @MpField(value = "image_default_id", columnType = "text", nullable = true, comment = "商品图片")
    private String imageDefaultId;

    /** 组合促销商品单价 */
    @MpField(value = "package_price", columnType = "integer", comment = "组合促销商品单价")
    private Integer packagePrice;

    /** 商品原价 */
    @MpField(value = "price", columnType = "integer", comment = "商品原价")
    private Integer price;

    /** 是否生效中 0 失效 | 1 生效 */
    @MpField(value = "status", columnType = "boolean", comment = "是否生效中 0 失效 | 1 生效")
    private Boolean status;

    /** 起始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "起始时间")
    private Integer startTime;

    /** 截止时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "截止时间")
    private Integer endTime;

    /** 列表页是否显示 */
    @MpField(value = "is_show", columnType = "boolean", nullable = true, comment = "列表页是否显示")
    private Boolean isShow;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
