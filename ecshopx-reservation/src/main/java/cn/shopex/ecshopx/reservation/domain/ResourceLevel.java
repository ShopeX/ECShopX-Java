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

package cn.shopex.ecshopx.reservation.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 资源位表 */
@Data
@MpTable(value = "reservation_resource_level", comment = "资源位表")
public class ResourceLevel {

    /** 自增id */
    @MpId(value = "resource_level_id", type = IdType.AUTO, columnType = "bigint", comment = "自增id")
    private Long resourceLevelId;

    /** 企业company id */
    @MpField(value = "company_id", columnType = "string", comment = "企业company id")
    private String companyId;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "string", comment = "门店id")
    private String shopId;

    /** 门店名称 */
    @MpField(value = "shop_name", columnType = "string", nullable = true, comment = "门店名称")
    private String shopName;

    /** 资源位昵称 */
    @MpField(value = "name", columnType = "string", comment = "资源位昵称")
    private String name;

    /** 简单介绍 */
    @MpField(value = "description", columnType = "string", length = 500, nullable = true, comment = "简单介绍")
    private String description;

    /** 状态 active:有效，invalid: 失效 */
    @MpField(value = "status", columnType = "string", nullable = true, comment = "状态,active:有效，invalid: 失效", defaultValue = "active")
    private String status = "active";

    /** 图片 */
    @MpField(value = "image_url", columnType = "string", nullable = true, comment = "图片")
    private String imageUrl;

    /** 数量 */
    @MpField(value = "quantity", columnType = "string", comment = "数量", defaultValue = "1")
    private String quantity = "1";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
