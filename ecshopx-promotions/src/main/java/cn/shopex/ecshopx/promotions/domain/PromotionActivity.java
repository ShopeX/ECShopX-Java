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

/** 自动化营销活动表 */
@Data
@MpTable(value = "promotions_activity", comment = "自动化营销活动表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class PromotionActivity {

    /** 活动ID */
    @MpId(value = "activity_id", type = IdType.AUTO, columnType = "bigint", comment = "活动ID")
    private Long activityId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 活动类型 */
    @MpField(value = "activity_type", columnType = "string", comment = "活动类型")
    private String activityType;

    /** 活动名称 */
    @MpField(value = "title", columnType = "string", comment = "活动名称")
    private String title;

    /** 触发条件 */
    @MpField(value = "trigger_condition", columnType = "text", comment = "触发条件")
    private String triggerCondition;

    /** 优惠配置 */
    @MpField(value = "discount_config", columnType = "text", comment = "优惠配置")
    private String discountConfig;

    /** 发送短信相关参数 */
    @MpField(value = "sms_params", columnType = "string", nullable = true, comment = "发送短信相关参数")
    private String smsParams;

    /** 是否开启发送短信 */
    @MpField(value = "sms_isopen", columnType = "string", comment = "是否开启发送短信")
    private String smsIsopen;

    /** 活动状态 */
    @MpField(value = "activity_status", columnType = "string", comment = "活动状态")
    private String activityStatus;

    /** 活动开始时间 */
    @MpField(value = "begin_time", columnType = "bigint", comment = "活动开始时间")
    private Long beginTime;

    /** 活动结束时间 */
    @MpField(value = "end_time", columnType = "bigint", nullable = true, comment = "活动结束时间")
    private Long endTime;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
