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

/** 活动规则完成任务 */
@Data
@MpTable(value = "user_task_finish_rule", comment = "活动规则完成任务")
public class UserTaskFinishRule {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 活动id */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动id", defaultValue = "0")
    private Long activityId = 0L;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id", defaultValue = "0")
    private Long userId = 0L;

    /** 规则 */
    @MpField(value = "rule_id", columnType = "string", comment = "规则")
    private String ruleId = "0";

    /** 1完成，2未完成 */
    @MpField(value = "finish_status", columnType = "integer", comment = "1完成，2未完成")
    private Integer finishStatus = 0;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
