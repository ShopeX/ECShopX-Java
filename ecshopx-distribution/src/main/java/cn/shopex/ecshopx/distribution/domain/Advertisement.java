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

package cn.shopex.ecshopx.distribution.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 广告表 */
@Data
@MpTable(value = "distribution_shopscreen_advertisement", comment = "广告表")
public class Advertisement {

    /** 广告id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "广告id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 标题 */
    @MpField(value = "title", columnType = "string", comment = "标题")
    private String title;

    /** 缩略图 */
    @MpField(value = "thumb_img", columnType = "text", comment = "缩略图")
    private String thumbImg;

    /** 排序 */
    @MpField(value = "sort", columnType = "integer", nullable = true, comment = "排序")
    private Integer sort;

    /** (图片/视频)地址 */
    @MpField(value = "media_url", columnType = "text", comment = "(图片/视频)地址")
    private String mediaUrl;

    /** 类型 */
    @MpField(value = "media_type", columnType = "string", comment = "类型", defaultValue = "image")
    private String mediaType = "image";

    /** 发布时间 */
    @MpField(value = "release_time", columnType = "integer", nullable = true, comment = "发布时间")
    private Integer releaseTime;

    /** 发布状态 */
    @MpField(value = "release_status", columnType = "boolean", comment = "发布状态", defaultValue = "False")
    private Boolean releaseStatus = false;

    /** 作者id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "作者id")
    private Long operatorId;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
