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

package cn.shopex.ecshopx.selfservice.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 报名问卷活动 */
@Data
@MpTable(value = "selfservice_registration_activity", comment = "报名问卷活动", indexes = {@MpIndex(name = "idx_temp_id", columns = {"temp_id"}), @MpIndex(name = "idx_start_time", columns = {"start_time"}), @MpIndex(name = "idx_group_no", columns = {"group_no"})})
public class RegistrationActivity {

    @MpId(value = "activity_id", type = IdType.AUTO, columnType = "bigint")
    private Long activityId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 表单模板id */
    @MpField(value = "temp_id", columnType = "bigint", comment = "表单模板id")
    private Long tempId;

    /** 活动名称 */
    @MpField(value = "activity_name", columnType = "string", length = 200, comment = "活动名称")
    private String activityName;

    /** 活动城市(省市) */
    @MpField(value = "area", columnType = "string", length = 100, nullable = true, comment = "活动城市(省市)")
    private String area;

    /** 活动地点 */
    @MpField(value = "place", columnType = "string", length = 100, nullable = true, comment = "活动地点")
    private String place;

    /** 详情地址 */
    @MpField(value = "address", columnType = "string", length = 200, nullable = true, comment = "详情地址")
    private String address;

    /** 活动简介 */
    @MpField(value = "intro", columnType = "string", length = 500, nullable = true, comment = "活动简介")
    private String intro;

    /** 前端展示字段 */
    @MpField(value = "show_fields", columnType = "string", length = 100, nullable = true, comment = "前端展示字段")
    private String showFields;

    /** 活动轮播图 */
    @MpField(value = "pics", columnType = "text", nullable = true, comment = "活动轮播图")
    private String pics;

    /** 奖励积分 */
    @MpField(value = "gift_points", columnType = "integer", comment = "奖励积分")
    private Integer giftPoints;

    /** 是否允许重复报名(1或0) */
    @MpField(value = "is_allow_duplicate", columnType = "integer", comment = "是否允许重复报名(1或0)")
    private Integer isAllowDuplicate;

    /** 是否允许取消报名(1或0) */
    @MpField(value = "is_allow_cancel", columnType = "integer", comment = "是否允许取消报名(1或0)")
    private Integer isAllowCancel;

    /** 是否线下核销(1或0) */
    @MpField(value = "is_offline_verify", columnType = "integer", comment = "是否线下核销(1或0)")
    private Integer isOfflineVerify;

    /** 是否需要审核(1或0) */
    @MpField(value = "is_need_check", columnType = "integer", comment = "是否需要审核(1或0)")
    private Integer isNeedCheck;

    /** 是否自动加入内购白名单(1或0) */
    @MpField(value = "is_white_list", columnType = "integer", comment = "是否自动加入内购白名单(1或0)")
    private Integer isWhiteList;

    /** 内购白名单企业ID */
    @MpField(value = "enterprise_ids", columnType = "text", nullable = true, comment = "内购白名单企业ID")
    private String enterpriseIds = "";

    /** 活动分组编码 */
    @MpField(value = "group_no", columnType = "string", length = 15, nullable = true, comment = "活动分组编码")
    private String groupNo = "";

    /** 适用会员等级 */
    @MpField(value = "member_level", columnType = "string", length = 50, nullable = true, comment = "适用会员等级")
    private String memberLevel;

    /** 适用店铺 */
    @MpField(value = "distributor_ids", columnType = "string", length = 100, nullable = true, comment = "适用店铺")
    private String distributorIds = "";

    /** 活动参与提示信息 */
    @MpField(value = "join_tips", columnType = "string", length = 200, nullable = true, comment = "活动参与提示信息")
    private String joinTips;

    /** 表单填写提示信息 */
    @MpField(value = "submit_form_tips", columnType = "text", nullable = true, comment = "表单填写提示信息")
    private String submitFormTips;

    /** 图文详情 */
    @MpField(value = "content", columnType = "text", nullable = true, comment = "图文详情")
    private String content;

    /** 活动开始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "活动开始时间")
    private Integer startTime;

    /** 活动结束时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "活动结束时间")
    private Integer endTime;

    /** 可参与次数 */
    @MpField(value = "join_limit", columnType = "integer", comment = "可参与次数", defaultValue = "0")
    private Integer joinLimit = 0;

    /** 是否短信通知 */
    @MpField(value = "is_sms_notice", columnType = "boolean", comment = "是否短信通知", defaultValue = "True")
    private Boolean isSmsNotice = true;

    /** 是否小程序模板通知 */
    @MpField(value = "is_wxapp_notice", columnType = "boolean", comment = "是否小程序模板通知", defaultValue = "True")
    private Boolean isWxappNotice = true;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;
}
