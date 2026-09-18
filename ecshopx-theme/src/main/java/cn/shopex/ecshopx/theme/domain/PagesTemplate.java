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

package cn.shopex.ecshopx.theme.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/** 页面模板表 */
@Data
@MpTable(value = "pages_template", comment = "页面模板表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class PagesTemplate {

    @MpId(value = "pages_template_id", type = IdType.AUTO, columnType = "bigint")
    private Long pagesTemplateId;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 区域id，默认 0 */
    @MpField(value = "regionauth_id", columnType = "bigint", nullable = true, comment = "区域id", defaultValue = "0")
    private Long regionauthId = 0L;

    /** 店铺id，默认 0 */
    @MpField(value = "distributor_id", columnType = "integer", nullable = true, comment = "店铺id", defaultValue = "0")
    private Integer distributorId = 0;

    /** 模板名称 */
    @MpField(value = "template_title", columnType = "string", length = 50, comment = "模板名称")
    private String templateTitle;

    /** 模板客户端名称 */
    @MpField(value = "template_name", columnType = "string", length = 50, comment = "模板客户端名称")
    private String templateName;

    /** 模板封面 */
    @MpField(value = "template_pic", columnType = "string", nullable = true, comment = "模板封面")
    private String templatePic;

    /** 模板类型 0总部  1同步模板 2门店自有模板，默认 0 */
    @MpField(value = "template_type", columnType = "integer", comment = "模板类型 0总部  1同步模板 2门店自有模板", defaultValue = "0")
    private Integer templateType = 0;

    /** 可编辑挂件状态  1启用 2未启用，默认 2 */
    @MpField(value = "element_edit_status", columnType = "integer", nullable = true, comment = "可编辑挂件状态  1启用 2未启用", defaultValue = "2")
    private Integer elementEditStatus = 2;

    /** 启用状态 1启用 2未启用，默认 2 */
    @MpField(value = "status", columnType = "integer", nullable = true, comment = "启用状态 1启用 2未启用", defaultValue = "2")
    private Integer status = 2;

    /** 定时启用状态 1启用 2未启用，默认 2 */
    @MpField(value = "timer_status", columnType = "integer", nullable = true, comment = "定时启用状态 1启用 2未启用", defaultValue = "2")
    private Integer timerStatus = 2;

    /** 定时模板切换时间 */
    @MpField(value = "timer_time", columnType = "integer", nullable = true, comment = "定时模板切换时间")
    private Integer timerTime;

    /** 模板状态变更时间 */
    @MpField(value = "template_status_modify_time", columnType = "integer", nullable = true, comment = "模板状态变更时间")
    private Integer templateStatusModifyTime;

    /** 模版页面:index-首页 distributor_index-门店首页，默认 index */
    @MpField(value = "weapp_pages", columnType = "string", nullable = true, comment = "模版页面:index-首页 distributor_index-门店首页", defaultValue = "index")
    private String weappPages = "index";

    /** 模板内容 */
    @MpField(value = "template_content", columnType = "text", nullable = true, comment = "模板内容")
    private String templateContent;

    /** 模版语言 */
    @MpField(value = "lang", columnType = "string", nullable = true, comment = "模版语言")
    private String lang;

    @MpField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @MpField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @MpField(value = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}
