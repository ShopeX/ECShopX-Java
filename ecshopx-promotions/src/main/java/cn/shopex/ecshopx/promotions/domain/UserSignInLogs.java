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

/** 用户签到奖励记录表 */
@Data
@MpTable(value = "user_signin_logs", comment = "用户签到奖励记录表")
public class UserSignInLogs {

    /** 记录id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "记录id")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 1日常签到，2，规则打标，3活动规则达标 */
    @MpField(value = "type", columnType = "integer", comment = "1日常签到，2，规则打标，3活动规则达标")
    private Integer type = 0;

    /** 获奖标题 */
    @MpField(value = "reward_title", columnType = "string", comment = "获奖标题")
    private String rewardTitle = "";

    /** 活动id */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动id")
    private Long activityId = 0L;

    /** 奖项记录多条 */
    @MpField(value = "reward_text", columnType = "text", comment = "奖项记录多条")
    private String rewardText = "";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
