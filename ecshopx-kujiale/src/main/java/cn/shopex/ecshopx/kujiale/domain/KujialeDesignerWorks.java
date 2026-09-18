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

/** KujialeDesignerWorks */
@Data
@MpTable(value = "kujiale_designer_works", indexes = {@MpIndex(name = "idx_design_id", columns = {"design_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_plan_id", columns = {"plan_id"})})
public class KujialeDesignerWorks {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 方案名称 */
    @MpField(value = "design_name", columnType = "string", length = 255, comment = "方案名称")
    private String designName;

    /** 方案封面 */
    @MpField(value = "cover_pic", columnType = "text", length = 65535, nullable = true, comment = "方案封面")
    private String coverPic;

    /** 是否原创 */
    @MpField(value = "is_origin", columnType = "bigint", nullable = true, comment = "是否原创")
    private Long isOrigin;

    /** 是否优秀 */
    @MpField(value = "is_excellent", columnType = "bigint", nullable = true, comment = "是否优秀")
    private Long isExcellent;

    /** 是否优秀 */
    @MpField(value = "is_real_excellent", columnType = "bigint", nullable = true, comment = "是否优秀")
    private Long isRealExcellent;

    /** 是否置顶 */
    @MpField(value = "is_top", columnType = "bigint", nullable = true, comment = "是否置顶")
    private Long isTop;

    /** 方案ID */
    @MpField(value = "design_id", columnType = "string", length = 255, nullable = true, comment = "方案ID")
    private String designId;

    /** 户型ID */
    @MpField(value = "plan_id", columnType = "string", length = 255, nullable = true, comment = "户型ID")
    private String planId;

    /** 小区 */
    @MpField(value = "comm_name", columnType = "string", length = 255, nullable = true, comment = "小区")
    private String commName;

    /** 城市 */
    @MpField(value = "city", columnType = "string", length = 255, nullable = true, comment = "城市")
    private String city;

    /** 户型名称 */
    @MpField(value = "name", columnType = "string", length = 255, nullable = true, comment = "户型名称")
    private String name;

    /** 方案分类id */
    @MpField(value = "tag_id", columnType = "string", length = 255, nullable = true, comment = "方案分类id")
    private String tagId;

    /** 全景漫游url */
    @MpField(value = "design_pano_url", columnType = "string", length = 255, nullable = true, comment = "全景漫游url")
    private String designPanoUrl;

    /** 用户头像 */
    @MpField(value = "user_avatar", columnType = "string", length = 255, nullable = true, comment = "用户头像")
    private String userAvatar;

    /** 邮箱 */
    @MpField(value = "email", columnType = "string", length = 255, nullable = true, comment = "邮箱")
    private String email;

    /** 用户名 */
    @MpField(value = "user_name", columnType = "string", length = 255, nullable = true, comment = "用户名")
    private String userName;

    /** 用户id */
    @MpField(value = "user_id", columnType = "string", length = 255, nullable = true, comment = "用户id")
    private String userId;

    /** 组织id */
    @MpField(value = "organization_id", columnType = "string", length = 255, nullable = true, comment = "组织id")
    private String organizationId;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer", comment = "创建时间")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;

    /** 浏览量 */
    @MpField(value = "view_count", columnType = "integer", nullable = true, comment = "浏览量")
    private Integer viewCount = 0;

    /** 点赞数 */
    @MpField(value = "like_count", columnType = "integer", nullable = true, comment = "点赞数")
    private Integer likeCount = 0;

    /** 方案更新时间（可空，勿默认 0，否则掩盖 DB NULL） */
    @MpField(value = "ku_created", columnType = "integer", nullable = true, comment = "方案更新时间")
    private Integer kuCreated;
}
