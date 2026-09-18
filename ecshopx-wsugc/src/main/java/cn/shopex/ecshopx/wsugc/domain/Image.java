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

package cn.shopex.ecshopx.wsugc.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 笔记的图片 */
@Data
@MpTable(value = "wsugc_post_image", comment = "笔记的图片", indexes = {@MpIndex(name = "idx_post_id", columns = {"post_id"})})
public class Image {

    @MpId(value = "post_image_id", type = IdType.AUTO, columnType = "bigint")
    private Long postImageId;

    /** 笔记id */
    @MpField(value = "post_id", columnType = "bigint", nullable = true, comment = "笔记id", defaultValue = "0")
    private Long postId = 0L;

    /** user_id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "user_id", defaultValue = "0")
    private Long userId = 0L;

    /** image_id */
    @MpField(value = "image_id", columnType = "bigint", nullable = true, comment = "image_id", defaultValue = "0")
    private Long imageId = 0L;

    /** 图片地址相对 */
    @MpField(value = "image_url", columnType = "string", comment = "图片地址相对")
    private String imageUrl;

    /** 排序 */
    @MpField(value = "p_order", columnType = "integer", nullable = true, comment = "排序", defaultValue = "50")
    private Integer pOrder = 50;

    /** 学员要求 */
    @MpField(value = "activity_joiner_condition", columnType = "text", nullable = true, comment = "学员要求")
    private String activityJoinerCondition;

    /** 活动简介 */
    @MpField(value = "activity_brief", columnType = "text", nullable = true, comment = "活动简介")
    private String activityBrief;

    /** 活动报名开始时间 */
    @MpField(value = "yuyue_begin_time", columnType = "integer", nullable = true, comment = "活动报名开始时间")
    private Integer yuyueBeginTime;

    /** 活动报名结束时间 */
    @MpField(value = "yuyue_end_time", columnType = "integer", nullable = true, comment = "活动报名结束时间")
    private Integer yuyueEndTime;

    /** 活动类型 */
    @MpField(value = "cat_id", columnType = "text", nullable = true, comment = "活动类型")
    private String catId;

    /** 活动开始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "活动开始时间")
    private Integer startTime;

    /** 活动结束时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "活动结束时间")
    private Integer endTime;

    /** 可参与次数 */
    @MpField(value = "join_limit", columnType = "integer", comment = "可参与次数", defaultValue = "0")
    private Integer joinLimit = 0;

    /** 限制人数 */
    @MpField(value = "limit_number", columnType = "integer", comment = "限制人数", defaultValue = "0")
    private Integer limitNumber = 0;

    /** 老师头衔 */
    @MpField(value = "teacher_title", columnType = "string", comment = "老师头衔")
    private String teacherTitle = "";

    /** 可参与次数 */
    @MpField(value = "teacher_name", columnType = "string", comment = "可参与次数")
    private String teacherName = "";

    /** 可参与次数 */
    @MpField(value = "teacher_brief", columnType = "text", comment = "可参与次数")
    private String teacherBrief = "";

    /** 可参与次数 */
    @MpField(value = "teacher_avatar", columnType = "string", comment = "可参与次数")
    private String teacherAvatar = "";

    /** 是否短信通知 */
    @MpField(value = "is_sms_notice", columnType = "boolean", comment = "是否短信通知", defaultValue = "True")
    private Boolean isSmsNotice = true;

    /** 是否小程序模板通知 */
    @MpField(value = "is_wxapp_notice", columnType = "boolean", comment = "是否小程序模板通知", defaultValue = "True")
    private Boolean isWxappNotice = true;

    /** 是否启用 */
    @MpField(value = "enabled", columnType = "integer", comment = "是否启用", defaultValue = "1")
    private Integer enabled = 1;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;
}
