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

/** 组合促销主商品表 */
@Data
@MpTable(value = "promotions_package_main_item", comment = "组合促销主商品表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_goods_id", columns = {"goods_id"}), @MpIndex(name = "idx_main_item_id", columns = {"main_item_id"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"package_id", "main_item_id"})})
public class PackageMainItemPromotions {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 组合促销规则id */
    @MpField(value = "package_id", columnType = "bigint", comment = "组合促销规则id")
    private Long packageId;

    /** 主商品id */
    @MpField(value = "main_item_id", columnType = "bigint", comment = "主商品id")
    private Long mainItemId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 商品id */
    @MpField(value = "goods_id", columnType = "bigint", comment = "商品id")
    private Long goodsId;

    /** 主商品价格 */
    @MpField(value = "main_item_price", columnType = "bigint", comment = "主商品价格")
    private Long mainItemPrice;
}
