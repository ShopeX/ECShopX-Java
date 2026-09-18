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

package cn.shopex.ecshopx.theme.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 广告位与店铺关联表 */
@Data
@MpTable(value = "pages_ad_place_rel_distributors", comment = "广告位与店铺关联表", uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"company_id", "ad_place_id", "distributor_id"})})
public class PagesAdPlaceRelDistributors {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 广告位id */
    @MpField(value = "ad_place_id", columnType = "bigint", comment = "广告位id")
    private Long adPlaceId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id")
    private Long distributorId;
}
