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
 * 文章表
 */
@Data
@MpTable(value = "companys_article", comment = "文章表")
public class Article {

    /** 文章id */
    @MpId(value = "article_id", type = IdType.AUTO, columnType = "bigint", comment = "文章id")
    private Long articleId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 标题 */
    @MpField(value = "title", columnType = "string", comment = "标题")
    private String title;

    /** 摘要 */
    @MpField(value = "summary", columnType = "string", nullable = true, comment = "摘要")
    private String summary;

    /** 文章详细内容 */
    @MpField(value = "content", columnType = "text", comment = "文章详细内容")
    private String content;

    /** 文章排序 */
    @MpField(value = "sort", columnType = "integer", nullable = true, comment = "文章排序")
    private Integer sort;

    /** 文章封面 */
    @MpField(value = "image_url", columnType = "text", nullable = true, comment = "文章封面")
    private String imageUrl;

    /** 分享图片 */
    @MpField(value = "share_image_url", columnType = "text", nullable = true, comment = "分享图片")
    private String shareImageUrl;

    /** 文章发布时间 */
    @MpField(value = "release_time", columnType = "integer", nullable = true, comment = "文章发布时间")
    private Integer releaseTime;

    /** 文章发布状态 */
    @MpField(value = "release_status", columnType = "boolean", comment = "文章发布状态", defaultValue = "True")
    private Boolean releaseStatus = true;

    /** 作者 */
    @MpField(value = "author", columnType = "string", nullable = true, comment = "作者")
    private String author;

    /** 作者id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "作者id")
    private Long operatorId;

    /** 作者头像 */
    @MpField(value = "head_portrait", columnType = "text", nullable = true, comment = "作者头像")
    private String headPortrait;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 文章类型，general:普通文章; bring:带货文章 */
    @MpField(value = "article_type", columnType = "string", nullable = true, comment = "文章类型，general:普通文章; bring:带货文章", defaultValue = "general")
    private String articleType = "general";

    /** 文章类目id */
    @MpField(value = "category_id", columnType = "bigint", nullable = true, comment = "文章类目id")
    private Long categoryId;

    /** 省 */
    @MpField(value = "province", columnType = "string", nullable = true, comment = "省")
    private String province;

    /** 市 */
    @MpField(value = "city", columnType = "string", nullable = true, comment = "市")
    private String city;

    /** 区 */
    @MpField(value = "area", columnType = "string", nullable = true, comment = "区")
    private String area;

    /** 地区编号集合 */
    @MpField(value = "regions_id", columnType = "json_array", nullable = true, comment = "地区编号集合")
    private String regionsId;

    /** 地区名称集合 */
    @MpField(value = "regions", columnType = "json_array", nullable = true, comment = "地区名称集合")
    private String regions;

    /** 是否AI生成，0表示人工创建，1表示AI生成 */
    @MpField(value = "is_ai", columnType = "boolean", comment = "是否AI生成，0表示人工创建，1表示AI生成", defaultValue = "False")
    private Boolean isAi = false;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
