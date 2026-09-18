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

package cn.shopex.ecshopx.supplier.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 新供应商商品属性表
 *
 * <p>索引：ix_item_id（item_id）
 */
@Data
@MpTable(value = "supplier_items_attr", comment = "新供应商商品属性表", indexes = {@MpIndex(name = "ix_item_id", columns = {"item_id"})})
public class SupplierItemsAttr {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 商品ID */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品ID")
    private Long itemId;

    /** 商品属性id */
    @MpField(value = "attribute_id", columnType = "bigint", comment = "商品属性id", defaultValue = "0")
    private Long attributeId = 0L;

    /** 是否需要删除 */
    @MpField(value = "is_del", columnType = "bigint", comment = "是否需要删除", defaultValue = "0")
    private Long isDel = 0L;

    /**
     * 商品属性类型 unit 单位，brand 品牌，item_params 商品参数, item_spec 规格, category 商品销售分类
     */
    @MpField(value = "attribute_type", columnType = "string", length = 15, comment = "商品属性类型 unit 单位，brand 品牌，item_params 商品参数, item_spec 规格, category 商品销售分类")
    private String attributeType;

    /** 属性值 */
    @MpField(value = "attr_data", columnType = "text", nullable = true, comment = "属性值")
    private String attrData = "";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
