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

package cn.shopex.ecshopx.community.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 社区拼团团长自提点管理表
 */
@Data
@MpTable(value = "community_chief_ziti", comment = "社区拼团团长自提点管理表", indexes = {@MpIndex(name = "ix_chief_id", columns = {"chief_id"}), @MpIndex(name = "ix_is_default", columns = {"is_default"}), @MpIndex(name = "ix_ziti_status", columns = {"ziti_status"})})
public class CommunityChiefZiti {

    /** 自提点id */
    @MpId(value = "ziti_id", type = IdType.AUTO, columnType = "bigint", comment = "自提点id")
    private Long zitiId;

    /** 团长ID */
    @MpField(value = "chief_id", columnType = "bigint", comment = "团长ID")
    private Long chiefId;

    /** 自提点名称 */
    @MpField(value = "ziti_name", columnType = "string", comment = "自提点名称")
    private String zitiName;

    /** 省，可为空 */
    @MpField(value = "province", columnType = "string", nullable = true, comment = "省")
    private String province;

    /** 市，可为空 */
    @MpField(value = "city", columnType = "string", nullable = true, comment = "市")
    private String city;

    /** 区，可为空 */
    @MpField(value = "area", columnType = "string", nullable = true, comment = "区")
    private String area;

    /** 地区编号集合（库内 JSON），可为空 */
    @MpField(value = "regions_id", columnType = "json_array", nullable = true, comment = "地区编号集合")
    private String regionsId;

    /** 地区名称集合（库内 JSON），可为空 */
    @MpField(value = "regions", columnType = "json_array", nullable = true, comment = "地区名称集合")
    private String regions;

    /** 具体地址，可为空，最长 500 */
    @MpField(value = "address", columnType = "string", length = 500, nullable = true, comment = "具体地址")
    private String address;

    /** 地图纬度，可为空 */
    @MpField(value = "lng", columnType = "string", nullable = true, comment = "地图纬度")
    private String lng;

    /** 地图经度，可为空 */
    @MpField(value = "lat", columnType = "string", nullable = true, comment = "地图经度")
    private String lat;

    /** 自提点联系人，可为空 */
    @MpField(value = "ziti_contact_user", columnType = "string", nullable = true, comment = "自提点联系人")
    private String zitiContactUser;

    /** 自提点联系电话，可为空 */
    @MpField(value = "ziti_contact_mobile", columnType = "string", nullable = true, comment = "自提点联系电话")
    private String zitiContactMobile;

    /** 自提点图片，可为空 */
    @MpField(value = "ziti_pics", columnType = "string", nullable = true, comment = "自提点图片")
    private String zitiPics;

    /** 是否默认，可为空，默认 false */
    @MpField(value = "is_default", columnType = "boolean", nullable = true, comment = "是否默认", defaultValue = "False")
    private Boolean isDefault = false;

    /**
     * 自提点状态：success 正常；fail 作废。默认 success。
     */
    @MpField(value = "ziti_status", columnType = "string", nullable = true, comment = "自提点状态 success正常 fail作废", defaultValue = "success")
    private String zitiStatus = "success";

    /** 创建时间（整型时间戳） */
    @MpField(value = "created_at", columnType = "integer")
    private Integer createdAt;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated_at", columnType = "integer", nullable = true)
    private Integer updatedAt;
}
