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

/** 预约配置表 */
@Data
@MpTable(value = "reservation_setting", comment = "预约配置表")
public class ReservationSetting {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 预约时间间隔 */
    @MpField(value = "time_interval", columnType = "integer", comment = "预约时间间隔")
    private Integer timeInterval;

    /** 资源位名称 */
    @MpField(value = "resource_name", columnType = "string", comment = "资源位名称")
    private String resourceName;

    /** 可提前预约天数 */
    @MpField(value = "max_limit_day", columnType = "integer", comment = "可提前预约天数")
    private Integer maxLimitDay;

    /** 可提前预约分钟数 */
    @MpField(value = "min_limit_hour", columnType = "integer", comment = "可提前预约分钟数")
    private Integer minLimitHour;

    /** 预约条件 */
    @MpField(value = "reservation_condition", columnType = "integer", comment = "预约条件")
    private Integer reservationCondition;

    /** 预约模式 */
    @MpField(value = "reservation_mode", columnType = "integer", comment = "预约模式")
    private Integer reservationMode;

    /** 取消预约最少提前分钟数 */
    @MpField(value = "cancel_minute", columnType = "integer", comment = "取消预约最少提前分钟数")
    private Integer cancelMinute;

    /** 预约限制 */
    @MpField(value = "reservation_num_limit", columnType = "text", nullable = true, comment = "预约限制")
    private String reservationNumLimit;

    /** 预约提醒通知 */
    @MpField(value = "sms_delay", columnType = "string", nullable = true, comment = "预约提醒通知")
    private String smsDelay;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
