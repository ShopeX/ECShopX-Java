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
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** pc页面装修组件内容 */
@Data
@MpTable(value = "theme_pc_template_content", comment = "pc页面装修组件内容", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class ThemePcTemplateContent {

    @MpId(value = "theme_pc_template_content_id", type = IdType.AUTO, columnType = "bigint")
    private Long themePcTemplateContentId;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    @MpField(value = "theme_pc_template_id", columnType = "bigint")
    private Long themePcTemplateId;

    /** 配置名称 */
    @MpField(value = "name", columnType = "string", length = 20, comment = "配置名称")
    private String name;

    /** 配置参数 */
    @MpField(value = "params", columnType = "text", comment = "配置参数")
    private String params;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 排序；与库列一致，可为 null */
    @MpField(value = "sort_by", columnType = "integer", nullable = true, comment = "排序", defaultValue = "0")
    private Integer sortBy;
}
