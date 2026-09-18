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

package cn.shopex.ecshopx.superadmin.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 店铺公告表 */
@Data
@MpTable(value = "shop_notice", comment = "店铺公告表")
public class ShopNotice {

    @MpId(value = "notice_id", type = IdType.AUTO, columnType = "bigint")
    private Long noticeId;

    /**
     * 公告类型。可选值有 notice-公告;helper-店主助手
     */
    @MpField(value = "type", columnType = "string", comment = "公告类型。可选值有 notice-公告;helper-店主助手")
    private String type;

    /** 公告标题 */
    @MpField(value = "title", columnType = "string", comment = "公告标题")
    private String title;

    /** 网页链接 */
    @MpField(value = "web_link", columnType = "string", comment = "网页链接")
    private String webLink;

    /** 是否发布 0:不发布 1:发布 */
    @MpField(value = "is_publish", columnType = "boolean", comment = "是否发布 0:不发布 1:发布")
    private Boolean isPublish = false;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
