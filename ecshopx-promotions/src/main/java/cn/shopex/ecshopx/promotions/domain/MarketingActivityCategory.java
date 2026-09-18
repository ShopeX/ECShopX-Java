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

/** 各种促销活动分类表 */
@Data
@MpTable(value = "promotions_marketing_activity_category", comment = "各种促销活动分类表", indexes = {@MpIndex(name = "ix_marketing_type", columns = {"marketing_type"}), @MpIndex(name = "ix_marketing_category", columns = {"category_id", "marketing_id"}), @MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class MarketingActivityCategory {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "自增id")
    private Long id;

    /** 关联营销id */
    @MpField(value = "marketing_id", columnType = "bigint", comment = "关联营销id")
    private Long marketingId;

    /** 关联分类id */
    @MpField(value = "category_id", columnType = "bigint", comment = "关联分类id")
    private Long categoryId;

    /** 营销类型: full_discount:满折,full_minus:满减,full_gift:满赠 */
    @MpField(value = "marketing_type", columnType = "string", comment = "营销类型: full_discount:满折,full_minus:满减,full_gift:满赠")
    private String marketingType;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 分类等级 */
    @MpField(value = "category_level", columnType = "integer", comment = "分类等级", defaultValue = "0")
    private Integer categoryLevel = 0;
}
