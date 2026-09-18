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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 小程序模板装修表 */
@Data
@MpTable(value = "wechat_weapp_setting", comment = "小程序模板装修表", indexes = {@MpIndex(name = "ix_page_nme", columns = {"page_name"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "ix_pages_template_id", columns = {"pages_template_id"}), @MpIndex(name = "idx_pagename_version_name_companyid_templatename", columns = {"page_name", "version", "name", "company_id", "template_name"})})
public class WeappSetting {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 小程序模板名称 */
    @MpField(value = "template_name", columnType = "string", length = 50, comment = "小程序模板名称")
    private String templateName;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 页面名称 */
    @MpField(value = "page_name", columnType = "string", length = 50, comment = "页面名称")
    private String pageName;

    /** 配置名称 */
    @MpField(value = "name", columnType = "string", length = 50, comment = "配置名称")
    private String name;

    /** 配置版本 */
    @MpField(value = "version", columnType = "string", length = 10, comment = "配置版本", defaultValue = "v1.0.0")
    private String version = "v1.0.0";

    /** 配置参数 */
    @MpField(value = "params", columnType = "text", comment = "配置参数")
    private String params;

    /** 页面模板id */
    @MpField(value = "pages_template_id", columnType = "integer", nullable = true, comment = "页面模板id", defaultValue = "0")
    private Integer pagesTemplateId = 0;

    /** 排序 */
    @MpField(value = "sort_by", columnType = "integer", nullable = true, comment = "排序", defaultValue = "0")
    private Integer sortBy = 0;
}
