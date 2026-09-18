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

package cn.shopex.ecshopx.community.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 社区拼团活动表
 */
@Data
@MpTable(value = "community_activity", comment = "社区拼团活动表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_chief_id", columns = {"chief_id"}), @MpIndex(name = "ix_activity_name", columns = {"activity_name"}), @MpIndex(name = "ix_start_time", columns = {"start_time"}), @MpIndex(name = "ix_end_time", columns = {"end_time"}), @MpIndex(name = "ix_activity_status", columns = {"activity_status"})})
public class CommunityActivity {

    /** 活动id */
    @MpId(value = "activity_id", type = IdType.AUTO, columnType = "bigint", comment = "活动id")
    private Long activityId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 店铺id，为 0 时表示该活动为平台活动 */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺id,为0时表示该活动为平台活动", defaultValue = "0")
    private Integer distributorId = 0;

    /** 团长ID */
    @MpField(value = "chief_id", columnType = "bigint", comment = "团长ID")
    private Long chiefId;

    /** 活动名称 */
    @MpField(value = "activity_name", columnType = "string", comment = "活动名称")
    private String activityName;

    /** 活动图片 */
    @MpField(value = "activity_pics", columnType = "text", comment = "活动图片")
    private String activityPics;

    /** 活动简介 */
    @MpField(value = "activity_desc", columnType = "string", comment = "活动简介")
    private String activityDesc;

    /** 活动详细介绍 */
    @MpField(value = "activity_intro", columnType = "text", comment = "活动详细介绍")
    private String activityIntro;

    /** 开始时间 */
    @MpField(value = "start_time", columnType = "integer", length = 11, comment = "开始时间")
    private Integer startTime;

    /** 结束时间 */
    @MpField(value = "end_time", columnType = "integer", length = 11, comment = "结束时间")
    private Integer endTime;

    /**
     * 活动状态。取值含义：private 私有；public 公开；protected 隐藏；success 确认成团；fail 成团失败。默认 public。
     */
    @MpField(value = "activity_status", columnType = "string", comment = "活动状态 private私有 public公开 protected隐藏 success确认成团 fail成团失败")
    private String activityStatus = "public";

    /**
     * 发货状态。可选值：DONE 已发货；PENDING 待发货；SUCCESS 已收货。默认 PENDING。
     */
    @MpField(value = "delivery_status", columnType = "string", comment = "发货状态。可选值有 DONE—已发货;PENDING—待发货;SUCCESS-已收货", defaultValue = "PENDING")
    private String deliveryStatus = "PENDING";

    /** 发货时间，可为空 */
    @MpField(value = "delivery_time", columnType = "integer", nullable = true, comment = "发货时间")
    private Integer deliveryTime;

    /** 售后配置，ban 表示禁止售后。默认 ban。 */
    @MpField(value = "aftersales_setting", columnType = "string", comment = "售后配置 ban禁止售后", defaultValue = "ban")
    private String aftersalesSetting = "ban";

    /** 创建时间（整型时间戳） */
    @MpField(value = "created_at", columnType = "integer")
    private Integer createdAt;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated_at", columnType = "integer", nullable = true)
    private Integer updatedAt;

    /** 分享图片，可为空 */
    @MpField(value = "share_image_url", columnType = "text", nullable = true, comment = "分享图片")
    private String shareImageUrl;
}
