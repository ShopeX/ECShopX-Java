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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员标签库表 */
@Data
@MpTable(value = "members_tags", comment = "会员标签库表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_source", columns = {"source"}), @MpIndex(name = "idx_tag_name", columns = {"tag_name"})})
public class MemberTags {

    /** 标签id */
    @MpId(value = "tag_id", type = IdType.AUTO, columnType = "bigint", comment = "标签id")
    private Long tagId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 标签名称 */
    @MpField(value = "tag_name", columnType = "string", length = 50, comment = "标签名称")
    private String tagName;

    /** 标签描述 */
    @MpField(value = "description", columnType = "string", length = 255, nullable = true, comment = "标签描述")
    private String description;

    /** 标签icon */
    @MpField(value = "tag_icon", columnType = "text", nullable = true, comment = "标签icon")
    private String tagIcon;

    /** 自定义标签添加人员id */
    @MpField(value = "saleman_id", columnType = "integer", comment = "自定义标签添加人员id", defaultValue = "0")
    private Integer salemanId = 0;

    /** 标签类型，online：线上发布, self: 私有自定义 */
    @MpField(value = "tag_status", columnType = "string", length = 32, comment = "标签类型，online：线上发布, self: 私有自定义", defaultValue = "online")
    private String tagStatus = "online";

    /** 分类id */
    @MpField(value = "category_id", columnType = "integer", comment = "分类id", defaultValue = "0")
    private Integer categoryId = 0;

    /** 自定义标签下会员数量 */
    @MpField(value = "self_tag_count", columnType = "integer", comment = "自定义标签下会员数量", defaultValue = "0")
    private Integer selfTagCount = 0;

    /** 标签颜色 */
    @MpField(value = "tag_color", columnType = "string", length = 50, comment = "标签颜色")
    private String tagColor = "#ff1939";

    /** 字体颜色 */
    @MpField(value = "font_color", columnType = "string", length = 50, comment = "字体颜色")
    private String fontColor = "#ffffff";

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;

    /** 标签来源,self:商户自定义，staff:系统固定员工tag */
    @MpField(value = "source", columnType = "string", comment = "标签来源,self:商户自定义，staff:系统固定员工tag", defaultValue = "self")
    private String source = "self";

    /** 企业微信标签ID */
    @MpField(value = "wechat_tag_id", columnType = "string", length = 100, nullable = true, comment = "企业微信标签ID")
    private String wechatTagId;
}
