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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 签到日志 */
@Data
@MpTable(value = "user_task_activity", comment = "签到日志")
public class UserTaskActivity {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 区域id */
    @MpField(value = "area_id", columnType = "bigint", comment = "区域id", defaultValue = "0")
    private Long areaId = 0L;

    /** 活动标题 */
    @MpField(value = "title", columnType = "string", comment = "活动标题")
    private String title;

    /** 开始时间 */
    @MpField(value = "begin_time", columnType = "bigint", comment = "开始时间", defaultValue = "0")
    private Long beginTime = 0L;

    /** 结束时间 */
    @MpField(value = "end_time", columnType = "bigint", comment = "结束时间", defaultValue = "0")
    private Long endTime = 0L;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
