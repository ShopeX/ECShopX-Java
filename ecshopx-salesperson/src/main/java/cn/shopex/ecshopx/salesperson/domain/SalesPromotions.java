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

/** 导购促销单 */
@Data
@MpTable(value = "companys_sales_promotions", comment = "导购促销单", indexes = {@MpIndex(name = "idx_salesperson_id", columns = {"salesperson_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_unique_key", columns = {"unique_key"})})
public class SalesPromotions {

    /** 促销单ID */
    @MpId(value = "sales_promotion_id", type = IdType.AUTO, columnType = "bigint", comment = "促销单ID")
    private Long salesPromotionId;

    /** 导购员id */
    @MpField(value = "salesperson_id", columnType = "bigint", comment = "导购员id")
    private Long salespersonId;

    /** 促销单唯一key */
    @MpField(value = "unique_key", columnType = "string", comment = "促销单唯一key")
    private String uniqueKey;

    /** 促销单内容 */
    @MpField(value = "promotion_items", columnType = "text", comment = "促销单内容")
    private String promotionItems;

    /** 企业id */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;
}
