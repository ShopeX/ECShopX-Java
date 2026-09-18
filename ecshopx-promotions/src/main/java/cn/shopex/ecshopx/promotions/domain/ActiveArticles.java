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

/** 活动文章表 */
@Data
@MpTable(value = "promotions_active_articles", comment = "活动文章表")
public class ActiveArticles {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 文章标题 */
    @MpField(value = "article_title", columnType = "string", comment = "文章标题")
    private String articleTitle;

    /** 文章副标题 */
    @MpField(value = "article_subtitle", columnType = "string", nullable = true, comment = "文章副标题")
    private String articleSubtitle;

    /** 文章内容 */
    @MpField(value = "article_content", columnType = "text", comment = "文章内容")
    private String articleContent;

    /** 封面 */
    @MpField(value = "article_cover", columnType = "text", comment = "封面")
    private String articleCover;

    /** 跳转地址,转json */
    @MpField(value = "directional_url", columnType = "text", comment = "跳转地址,转json")
    private String directionalUrl;

    /** 是否展示,1展示 0不展示 */
    @MpField(value = "is_show", columnType = "boolean", comment = "是否展示,1展示 0不展示", defaultValue = "1")
    private Boolean isShow = true;

    /** 是否已删除,1已删除 0未删除 */
    @MpField(value = "is_delete", columnType = "boolean", comment = "是否已删除,1已删除 0未删除", defaultValue = "0")
    private Boolean isDelete = false;

    /** 排序 */
    @MpField(value = "sort", columnType = "bigint", nullable = true, comment = "排序", defaultValue = "0")
    private Long sort = 0L;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
