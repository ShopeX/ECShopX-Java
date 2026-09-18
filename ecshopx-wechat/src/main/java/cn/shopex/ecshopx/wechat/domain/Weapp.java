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

package cn.shopex.ecshopx.wechat.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/** 微信小程序提交审核表 */
@Data
@MpTable(value = "wechat_weapp", comment = "微信小程序提交审核表")
public class Weapp {

    /** 微信appid */
    @MpId(value = "authorizer_appid", type = IdType.INPUT, columnType = "string", length = 64, comment = "微信appid")
    private String authorizerAppid;

    /** 绑定操作者id */
    @MpField(value = "operator_id", columnType = "bigint", comment = "绑定操作者id")
    private Long operatorId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 审核失败原因 */
    @MpField(value = "reason", columnType = "text", nullable = true, comment = "审核失败原因")
    private String reason;

    /** 审核状态，其中0为审核成功，1为审核失败，2为审核中, 3为待提交审核 */
    @MpField(value = "audit_status", columnType = "integer", comment = "审核状态，其中0为审核成功，1为审核失败，2为审核中, 3为待提交审核")
    private Integer auditStatus;

    /** 发布状态，其中0为未发布，1为已发布 */
    @MpField(value = "release_status", columnType = "integer", comment = "发布状态，其中0为未发布，1为已发布")
    private Integer releaseStatus;

    /** 审核时间 */
    @MpField(value = "audit_time", columnType = "string", nullable = true, comment = "审核时间")
    private String auditTime;

    /** 小程序模板ID */
    @MpField(value = "template_id", columnType = "string", comment = "小程序模板ID")
    private String templateId;

    /** 小程序模板名称 */
    @MpField(value = "template_name", columnType = "string", comment = "小程序模板名称")
    private String templateName;

    /** 小程序已发布的版本 */
    @MpField(value = "release_ver", columnType = "string", nullable = true, comment = "小程序已发布的版本")
    private String releaseVer;

    /** 小程序模板版本 */
    @MpField(value = "template_ver", columnType = "string", nullable = true, comment = "小程序模板版本")
    private String templateVer;

    /** 小程序线上代码的可见状态 0不可见 1可见 */
    @MpField(value = "visitstatus", columnType = "integer", comment = "小程序线上代码的可见状态 0不可见 1可见")
    private Integer visitStatus;

    @MpField("created_at")
    private LocalDateTime createdAt;

    @MpField(value = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @MpField(value = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}
