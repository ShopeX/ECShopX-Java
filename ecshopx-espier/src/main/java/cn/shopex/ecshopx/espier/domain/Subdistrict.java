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

package cn.shopex.ecshopx.espier.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 街道社区表 */
@Data
@MpTable(value = "espier_subdistrict", comment = "街道社区表", indexes = {@MpIndex(name = "ix_parent_id", columns = {"parent_id"}), @MpIndex(name = "ix_label", columns = {"label"})})
public class Subdistrict {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 地区名称 */
    @MpField(value = "label", columnType = "string", comment = "地区名称")
    private String label;

    /** 父级id */
    @MpField(value = "parent_id", columnType = "bigint", comment = "父级id")
    private Long parentId;

    /** 所属店铺id列表 */
    @MpField(value = "distributor_id", columnType = "string", comment = "所属店铺id列表", defaultValue = ",")
    private String distributorId = ",";

    @MpField(value = "province", columnType = "string", nullable = true)
    private String province;

    @MpField(value = "city", columnType = "string", nullable = true)
    private String city;

    @MpField(value = "area", columnType = "string", nullable = true)
    private String area;

    /** 国家行政区划编码组合，逗号隔开 */
    @MpField(value = "regions_id", columnType = "text", nullable = true, comment = "国家行政区划编码组合，逗号隔开")
    private String regionsId;
}
