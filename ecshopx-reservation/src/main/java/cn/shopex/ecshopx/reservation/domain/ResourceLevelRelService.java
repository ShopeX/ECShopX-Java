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

package cn.shopex.ecshopx.reservation.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 资源位服务项目关联表 */
@Data
@MpTable(value = "reservation_level_rel_service", comment = "资源位服务项目关联表", uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"resource_level_id", "company_id", "shop_id", "material_id"})})
public class ResourceLevelRelService {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 资源位id */
    @MpField(value = "resource_level_id", columnType = "bigint", comment = "资源位id")
    private Long resourceLevelId;

    /** 公司 company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司 company id")
    private Long companyId;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id")
    private Long shopId;

    /** 服务项目id */
    @MpField(value = "material_id", columnType = "bigint", comment = "服务项目id")
    private Long materialId;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
