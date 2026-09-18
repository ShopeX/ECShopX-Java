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
@MpTable(value = "reservation_shift_type", comment = "班次类型表")
public class WorkShiftType {

    /** type_id */
    @MpId(value = "type_id", type = IdType.AUTO, columnType = "bigint", comment = "type_id")
    private Long typeId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 排班类型名称 */
    @MpField(value = "type_name", columnType = "string", length = 30, comment = "排班类型名称")
    private String typeName;

    /** 排班类型名称 */
    @MpField(value = "begin_time", columnType = "string", length = 5, comment = "排班类型名称")
    private String beginTime;

    /** 排班类型名称 */
    @MpField(value = "end_time", columnType = "string", length = 5, comment = "排班类型名称")
    private String endTime;

    /** 类型状态 invalid/valid */
    @MpField(value = "status", columnType = "string", length = 10, comment = "类型状态invalid/valid", defaultValue = "valid")
    private String status = "valid";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
