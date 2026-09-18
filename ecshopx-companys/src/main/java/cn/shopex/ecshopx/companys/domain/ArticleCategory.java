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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 文章类目表
 */
@Data
@MpTable(value = "companys_article_category", comment = "文章类目表")
public class ArticleCategory {

    /** 文章类目id */
    @MpId(value = "category_id", type = IdType.AUTO, columnType = "bigint", comment = "文章类目id")
    private Long categoryId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 类目名称 */
    @MpField(value = "category_name", columnType = "string", length = 50, comment = "类目名称")
    private String categoryName;

    /** 父级id, 0为顶级 */
    @MpField(value = "parent_id", columnType = "bigint", comment = "父级id, 0为顶级", defaultValue = "0")
    private Long parentId = 0L;

    /** 类目等级 */
    @MpField(value = "category_level", columnType = "integer", nullable = true, comment = "类目等级", defaultValue = "1")
    private Integer categoryLevel = 1;

    /** 路径 */
    @MpField(value = "path", columnType = "string", length = 255, nullable = true, comment = "路径", defaultValue = "0")
    private String path = "0";

    /** 排序 */
    @MpField(value = "sort", columnType = "bigint", nullable = true, comment = "排序", defaultValue = "0")
    private Long sort = 0L;

    /** 文章栏目类型，general:普通; bring:带货 */
    @MpField(value = "category_type", columnType = "string", nullable = true, comment = "文章栏目类型，general:普通; bring:带货", defaultValue = "bring")
    private String categoryType = "bring";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
