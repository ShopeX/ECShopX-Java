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

/** 导购任务表 */
@Data
@MpTable(value = "salesperson_task", comment = "导购任务表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class SalespersonTask {

    /** ID */
    @MpId(value = "task_id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long taskId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 开始时间 */
    @MpField(value = "start_time", columnType = "bigint", comment = "开始时间")
    private Long startTime;

    /** 结束时间 */
    @MpField(value = "end_time", columnType = "bigint", comment = "结束时间")
    private Long endTime;

    /** 任务名称 */
    @MpField(value = "task_name", columnType = "string", comment = "任务名称")
    private String taskName;

    /** 任务类型 1 转发分享 2 获取新客 3 客户下单 4 会员福利 */
    @MpField(value = "task_type", columnType = "smallint", comment = "任务类型 1 转发分享 2 获取新客 3 客户下单 4 会员福利")
    private Integer taskType;

    /** 任务指标 */
    @MpField(value = "task_quota", columnType = "smallint", comment = "任务指标")
    private Integer taskQuota;

    /** 任务指标 */
    @MpField(value = "pics", columnType = "json_array", nullable = true, comment = "任务指标")
    private String pics;

    /** 任务内容 */
    @MpField(value = "task_content", columnType = "text", comment = "任务内容")
    private String taskContent;

    /** 是否是全部店铺 */
    @MpField(value = "use_all_distributor", columnType = "boolean", nullable = true, comment = "是否是全部店铺", defaultValue = "False")
    private Boolean useAllDistributor = false;

    /** 任务指标 */
    @MpField(value = "disabled", columnType = "string", length = 30, comment = "任务指标")
    private String disabled;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
