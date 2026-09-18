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

package cn.shopex.ecshopx.kujiale.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** KujialeDesignerTags */
@Data
@MpTable(value = "kujiale_designer_tags", indexes = {@MpIndex(name = "idx_category_id", columns = {"tag_category_id"}), @MpIndex(name = "idx_tag_id", columns = {"tag_id"})})
public class KujialeDesignerTags {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 标签类目id */
    @MpField(value = "tag_category_id", columnType = "string", length = 255, comment = "标签类目id")
    private String tagCategoryId;

    /** 标签类目名 */
    @MpField(value = "tag_category_name", columnType = "string", length = 255, nullable = true, comment = "标签类目名")
    private String tagCategoryName;

    /** 类型 */
    @MpField(value = "type", columnType = "integer", nullable = true, comment = "类型")
    private Integer type;

    /** 是否支持多选 */
    @MpField(value = "is_multiple_selected", columnType = "integer", nullable = true, comment = "是否支持多选")
    private Integer isMultipleSelected;

    /** 是否禁用 */
    @MpField(value = "is_disabled", columnType = "integer", nullable = true, comment = "是否禁用")
    private Integer isDisabled;

    /** 标签id */
    @MpField(value = "tag_id", columnType = "string", length = 255, nullable = true, comment = "标签id")
    private String tagId;

    /** 标签名称 */
    @MpField(value = "tag_name", columnType = "string", length = 255, nullable = true, comment = "标签名称")
    private String tagName;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer", comment = "创建时间")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
