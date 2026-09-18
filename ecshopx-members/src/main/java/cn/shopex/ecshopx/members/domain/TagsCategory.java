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

package cn.shopex.ecshopx.members.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 标签分类 */
@Data
@MpTable(value = "members_tags_category", comment = "标签分类")
public class TagsCategory {

    /** 标签分类id */
    @MpId(value = "category_id", type = IdType.AUTO, columnType = "bigint", comment = "标签分类id")
    private Long categoryId;

    /** 标签分类名称 */
    @MpField(value = "category_name", columnType = "string", length = 50, comment = "标签分类名称")
    private String categoryName;

    /** 排序 */
    @MpField(value = "sort", columnType = "bigint", comment = "排序", defaultValue = "1")
    private Long sort = 1L;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
