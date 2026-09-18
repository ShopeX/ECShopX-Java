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

/** 签到活动规则 */
@Data
@MpTable(value = "user_task_activity_rule", comment = "签到活动规则")
public class UserTaskActivityRule {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 活动id */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动id", defaultValue = "0")
    private Long activityId = 0L;

    /** 活动类型 */
    @MpField(value = "rule_type", columnType = "string", comment = "活动类型")
    private String ruleType = "";

    /** 活动详细类型，理解为二级类型 */
    @MpField(value = "rule_detail_type", columnType = "string", comment = "活动详细类型，理解为二级类型")
    private String ruleDetailType = "";

    /** 规则名称 */
    @MpField(value = "rule_name", columnType = "string", comment = "规则名称")
    private String ruleName = "";

    /** 1连续2累计 */
    @MpField(value = "sign_type", columnType = "bigint", comment = "1连续2累计")
    private Long signType = 1L;

    /** 规则名称 */
    @MpField(value = "rule_desc", columnType = "string", comment = "规则名称")
    private String ruleDesc = "";

    /** 用户标签 */
    @MpField(value = "hidde_tag", columnType = "string", comment = "用户标签")
    private String hiddeTag = "";

    /** 完成标签 */
    @MpField(value = "finish_tag", columnType = "string", comment = "完成标签")
    private String finishTag = "";

    /** 指定店铺 */
    @MpField(value = "binding_distributors", columnType = "string", comment = "指定店铺")
    private String bindingDistributors = "";

    /** 指定管理分类 */
    @MpField(value = "binding_category", columnType = "string", comment = "指定管理分类")
    private String bindingCategory = "";

    /** 分享图 */
    @MpField(value = "share_pic", columnType = "string", comment = "分享图")
    private String sharePic = "";

    /** 企微二维码 */
    @MpField(value = "qr_code", columnType = "string", comment = "企微二维码")
    private String qrCode = "";

    /** 频次，1每天2每周3每月0为活动周期内 */
    @MpField(value = "frequency", columnType = "bigint", comment = "频次，1每天2每周3每月0为活动周期内")
    private Long frequency = 0L;

    /** 常规的门槛，比如限制金额，限制页面，等等 */
    @MpField(value = "common_condition", columnType = "string", comment = "常规的门槛，比如限制金额，限制页面，等等")
    private String commonCondition = "";

    /** 奖励信息 */
    @MpField(value = "prize_text", columnType = "text", comment = "奖励信息")
    private String prizeText = "";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
