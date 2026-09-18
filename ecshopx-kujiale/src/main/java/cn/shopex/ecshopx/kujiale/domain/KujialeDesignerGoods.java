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

package cn.shopex.ecshopx.kujiale.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** KujialeDesignerGoods */
@Data
@MpTable(value = "kujiale_designer_goods", indexes = {@MpIndex(name = "idx_good_id", columns = {"good_id"})})
public class KujialeDesignerGoods {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 渲染图ID */
    @MpField(value = "good_id", columnType = "string", length = 128, comment = "渲染图ID")
    private String goodId = "";

    /** 尺寸 */
    @MpField(value = "dimensions", columnType = "string", length = 32, comment = "尺寸")
    private String dimensions = "";

    /** 描述 */
    @MpField(value = "description", columnType = "string", length = 255, comment = "描述")
    private String description = "";

    /** 商品编码 */
    @MpField(value = "brand_good_code", columnType = "string", length = 128, comment = "商品编码")
    private String brandGoodCode = "";

    /** 商品名称 */
    @MpField(value = "brand_good_name", columnType = "string", length = 128, comment = "商品名称")
    private String brandGoodName = "";

    /** 品牌id */
    @MpField(value = "brand_id", columnType = "string", length = 32, comment = "品牌id")
    private String brandId = "";

    /** 品牌名称 */
    @MpField(value = "brand_name", columnType = "string", length = 128, comment = "品牌名称")
    private String brandName = "";

    /** 系列id */
    @MpField(value = "series_tag_id", columnType = "string", length = 32, comment = "系列id")
    private String seriesTagId = "";

    /** 系列名称 */
    @MpField(value = "series_tag_name", columnType = "string", length = 128, comment = "系列名称")
    private String seriesTagName = "";

    /** 型号 */
    @MpField(value = "product_number", columnType = "string", length = 32, comment = "型号")
    private String productNumber = "";

    /** 材质 */
    @MpField(value = "customer_texture", columnType = "string", length = 64, comment = "材质")
    private String customerTexture = "";

    /** 购买链接 */
    @MpField(value = "buy_link", columnType = "string", length = 255, comment = "购买链接")
    private String buyLink = "";

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer", comment = "创建时间")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
