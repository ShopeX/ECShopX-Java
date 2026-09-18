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

package cn.shopex.ecshopx.distribution.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 店铺导购员表 */
@Data
@MpTable(value = "distribution_distributor_items", comment = "店铺导购员表", indexes = {@MpIndex(name = "idx_defaultitemid_companyid", columns = {"default_item_id", "company_id"}), @MpIndex(name = "idx_companyid_istotalstore_goodscansale_defaultitemid", columns = {"company_id", "is_total_store", "goods_can_sale", "default_item_id"})}, uniqueIndexes = {@MpIndex(name = "distributor_items", columns = {"distributor_id", "item_id"})})
public class DistributorItems {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    @MpField(value = "distributor_id", columnType = "bigint")
    private Long distributorId;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 商品ID */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品ID")
    private Long itemId;

    /** 默认商品ID */
    @MpField(value = "default_item_id", columnType = "bigint", comment = "默认商品ID", defaultValue = "0")
    private Long defaultItemId = 0L;

    /** 商品集合ID */
    @MpField(value = "goods_id", columnType = "bigint", comment = "商品集合ID", defaultValue = "0")
    private Long goodsId = 0L;

    /** 是否为列表默认展示 */
    @MpField(value = "is_show", columnType = "boolean", comment = "是否为列表默认展示", defaultValue = "True")
    private Boolean isShow = true;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    /** 商品库存 */
    @MpField(value = "store", columnType = "bigint", nullable = true, comment = "商品库存", defaultValue = "0")
    private Long store = 0L;

    /** 商品价格 */
    @MpField(value = "price", columnType = "bigint", nullable = true, comment = "商品价格", defaultValue = "0")
    private Long price = 0L;

    /** 是否为总部库存 */
    @MpField(value = "is_total_store", columnType = "boolean", nullable = true, comment = "是否为总部库存", defaultValue = "True")
    private Boolean isTotalStore = true;

    /** 是否在本店可售 */
    @MpField(value = "is_can_sale", columnType = "boolean", nullable = true, comment = "是否在本店可售", defaultValue = "True")
    private Boolean isCanSale = true;

    /** 商品是否可售，有一个sku可售，那么商品就可售 */
    @MpField(value = "goods_can_sale", columnType = "boolean", nullable = true, comment = "商品是否可售，有一个sku可售，那么商品就可售", defaultValue = "True")
    private Boolean goodsCanSale = true;

    /** 是否开启自提配送 */
    @MpField(value = "is_self_delivery", columnType = "boolean", nullable = true, comment = "是否开启自提配送", defaultValue = "False")
    private Boolean isSelfDelivery = false;

    /** 是否开启快递配送 */
    @MpField(value = "is_express_delivery", columnType = "boolean", nullable = true, comment = "是否开启快递配送", defaultValue = "False")
    private Boolean isExpressDelivery = false;

    /** 商品销量 */
    @MpField(value = "sales", columnType = "bigint", comment = "商品销量", defaultValue = "0")
    private Long sales = 0L;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
