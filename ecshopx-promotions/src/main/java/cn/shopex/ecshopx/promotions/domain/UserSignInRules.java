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

/** 用户签到规则表 */
@Data
@MpTable(value = "user_signin_rules", comment = "用户签到规则表")
public class UserSignInRules {

    /** 记录id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "记录id")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 规则名称 */
    @MpField(value = "rule_name", columnType = "string", comment = "规则名称")
    private String ruleName;

    /** 1连续2累计 */
    @MpField(value = "type", columnType = "bigint", comment = "1连续2累计")
    private Long type = 1L;

    /** 需要的天数 */
    @MpField(value = "days_required ", columnType = "bigint", comment = "需要的天数")
    private Long daysRequired = 1L;

    /** 奖励类型 points积分，coupon券，coupons券包 */
    @MpField(value = "reward_text", columnType = "text", comment = "奖励类型 points积分，coupon券，coupons券包")
    private String rewardText = "";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
