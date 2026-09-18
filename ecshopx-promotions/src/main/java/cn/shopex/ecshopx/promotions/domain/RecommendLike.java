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

/** 推荐喜欢商品(猜你喜欢) */
@Data
@MpTable(value = "promotions_recommend_like_items", comment = "推荐喜欢商品(猜你喜欢)", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class RecommendLike {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "自增id")
    private Long id;

    /** 企业id */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "店铺id")
    private Long distributorId;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 商品排序 */
    @MpField(value = "sort", columnType = "integer", comment = "商品排序", defaultValue = "0")
    private Integer sort = 0;
}
