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

/** 会员收货地址表 */
@Data
@MpTable(value = "members_address", comment = "会员收货地址表", indexes = {@MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class MembersAddress {

    /** id */
    @MpId(value = "address_id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long addressId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 收货人 */
    @MpField(value = "username", columnType = "string", length = 50, comment = "收货人")
    private String username;

    /** 手机号码 */
    @MpField(value = "telephone", columnType = "string", length = 20, comment = "手机号码")
    private String telephone;

    /** 地区 */
    @MpField(value = "area", columnType = "string", nullable = true, comment = "地区")
    private String area;

    /** 地区：省 */
    @MpField(value = "province", columnType = "string", comment = "地区：省")
    private String province;

    /** 地区：市 */
    @MpField(value = "city", columnType = "string", comment = "地区：市")
    private String city;

    /** 地区：区 */
    @MpField(value = "county", columnType = "string", comment = "地区：区")
    private String county;

    /** 详细地址 */
    @MpField(value = "adrdetail", columnType = "string", comment = "详细地址")
    private String adrdetail;

    /** 邮编 */
    @MpField(value = "postalCode", columnType = "string", length = 20, nullable = true, comment = "邮编")
    private String postalCode;

    /** 是否默认地址 */
    @MpField(value = "is_def", columnType = "boolean", comment = "是否默认地址", defaultValue = "0")
    private Boolean isDef = false;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;

    /** 第三方数据 */
    @MpField(value = "third_data", columnType = "string", nullable = true, comment = "第三方数据")
    private String thirdData;

    /** 腾讯地图纬度 */
    @MpField(value = "lng", columnType = "string", comment = "腾讯地图纬度")
    private String lng = "";

    /** 腾讯地图经度 */
    @MpField(value = "lat", columnType = "string", comment = "腾讯地图经度")
    private String lat = "";
}
