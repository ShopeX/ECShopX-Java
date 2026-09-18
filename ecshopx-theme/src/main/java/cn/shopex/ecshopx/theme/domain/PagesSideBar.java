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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 侧边栏设置 */
@Data
@MpTable(value = "pages_side_bar", comment = "侧边栏设置")
public class PagesSideBar {

    /** 设置id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "设置id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 区域id，默认 0 */
    @MpField(value = "regionauth_id", columnType = "bigint", comment = "区域id", defaultValue = "0")
    private Long regionauthId = 0L;

    /** 名称 */
    @MpField(value = "name", columnType = "string", length = 100, comment = "名称")
    private String name;

    /** 关联页面 */
    @MpField(value = "pages", columnType = "string", length = 100, comment = "关联页面")
    private String pages;

    /** 是否禁用 */
    @MpField(value = "disabled", insertStrategy = FieldStrategy.NOT_NULL, columnType = "boolean", comment = "是否禁用", defaultValue = "False")
    private Boolean disabled;

    /** 设置 */
    @MpField(value = "setting", insertStrategy = FieldStrategy.NOT_NULL, columnType = "text", nullable = true, comment = "设置")
    private String setting;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
