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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 班次类型表 */
@Data
@MpTable(value = "reservation_work_shift", comment = "班次类型表")
public class WorkShift {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 公司门店 id */
    @MpField(value = "shop_id", columnType = "bigint", comment = "公司门店 id")
    private Long shopId;

    /** 资源位id */
    @MpField(value = "resource_level_id", columnType = "bigint", comment = "资源位id")
    private Long resourceLevelId;

    /** 工作日期 */
    @MpField(value = "work_date", columnType = "bigint", comment = "工作日期")
    private Long workDate;

    /** 工作班次类型id */
    @MpField(value = "shift_type_id", columnType = "bigint", comment = "工作班次类型id")
    private Long shiftTypeId;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
