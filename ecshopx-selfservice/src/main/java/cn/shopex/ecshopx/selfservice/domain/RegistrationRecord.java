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

/** 报名记录 */
@Data
@MpTable(value = "selfservice_registration_record", comment = "报名记录", indexes = {@MpIndex(name = "idx_activity_id", columns = {"activity_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_true_name", columns = {"true_name"}), @MpIndex(name = "idx_created", columns = {"created"}), @MpIndex(name = "idx_status", columns = {"status"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class RegistrationRecord {

    @MpId(value = "record_id", type = IdType.AUTO, columnType = "bigint")
    private Long recordId;

    /** 活动id */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动id")
    private Long activityId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 报名编号 */
    @MpField(value = "record_no", columnType = "bigint", comment = "报名编号", defaultValue = "0")
    private Long recordNo = 0L;

    /** 活动分组编码 */
    @MpField(value = "group_no", columnType = "string", length = 15, nullable = true, comment = "活动分组编码")
    private String groupNo = "";

    /** 表单id */
    @MpField(value = "form_id", columnType = "bigint", comment = "表单id")
    private Long formId;

    /** 获取到积分 */
    @MpField(value = "get_points", columnType = "bigint", comment = "获取到积分")
    private Long getPoints;

    /** 核销码 */
    @MpField(value = "verify_code", columnType = "bigint", comment = "核销码")
    private Long verifyCode;

    /** 核销时间 */
    @MpField(value = "verify_time", columnType = "bigint", comment = "核销时间", defaultValue = "0")
    private Long verifyTime = 0L;

    /** 核销员 */
    @MpField(value = "verify_operator", columnType = "string", length = 50, nullable = true, comment = "核销员")
    private String verifyOperator = "";

    /** 真实姓名 */
    @MpField(value = "true_name", columnType = "string", length = 30, nullable = true, comment = "真实姓名")
    private String trueName = "";

    /** 是否加入白名单 */
    @MpField(value = "is_white_list", columnType = "bigint", comment = "是否加入白名单")
    private Long isWhiteList;

    /** 会员id */
    @MpField(value = "user_id", columnType = "bigint", comment = "会员id")
    private Long userId;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", comment = "手机号")
    private String mobile;

    /** 表单填写手机号 */
    @MpField(value = "form_mobile", columnType = "string", comment = "表单填写手机号")
    private String formMobile = "";

    /** 会员小程序appid */
    @MpField(value = "wxapp_appid", columnType = "string", length = 32, nullable = true, comment = "会员小程序appid")
    private String wxappAppid;

    /** 会员小程序openid */
    @MpField(value = "open_id", columnType = "string", length = 32, nullable = true, comment = "会员小程序openid")
    private String openId;

    /** 状态: pending 待审核，passed 已通过，rejected 已拒绝, canceled 已取消, verified 已核销 */
    @MpField(value = "status", columnType = "string", length = 32, comment = "状态: pending 待审核，passed 已通过，rejected 已拒绝, canceled 已取消, verified 已核销", defaultValue = "pending")
    private String status = "pending";

    /** 报名内容 */
    @MpField(value = "content", columnType = "text", comment = "报名内容")
    private String content;

    /** 拒绝原因 */
    @MpField(value = "reason", columnType = "text", nullable = true, comment = "拒绝原因")
    private String reason;

    /** 备注 */
    @MpField(value = "remark", columnType = "text", nullable = true, comment = "备注")
    private String remark;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 公司_ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司_ID")
    private Long companyId;
}
