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

package cn.shopex.ecshopx.salesperson.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 店铺管理员关联店铺表 */
@Data
@MpTable(value = "shop_rel_salesperson", comment = "店铺管理员关联店铺表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"shop_id", "salesperson_id"})})
public class ShopsRelSalesperson {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 店铺id */
    @MpField(value = "shop_id", columnType = "bigint", comment = "店铺id")
    private Long shopId;

    /** 店铺管理员id */
    @MpField(value = "salesperson_id", columnType = "bigint", comment = "店铺管理员id")
    private Long salespersonId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 店铺类型，shop：门店, distributor:店铺 */
    @MpField(value = "store_type", columnType = "string", comment = "店铺类型，shop：门店, distributor:店铺", defaultValue = "shop")
    private String storeType = "shop";
}
