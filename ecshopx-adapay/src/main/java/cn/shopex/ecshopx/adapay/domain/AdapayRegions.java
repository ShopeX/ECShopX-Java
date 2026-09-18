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

package cn.shopex.ecshopx.adapay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 省市编码(四位码)
 */
@Data
@MpTable(value = "adapay_regions", comment = "省市编码(四位码)", indexes = {@MpIndex(name = "idx_area_name", columns = {"area_name"}), @MpIndex(name = "idx_area_code", columns = {"area_code"})})
public class AdapayRegions {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 名称 */
    @MpField(value = "area_name", columnType = "string", length = 50, comment = "名称")
    private String areaName;

    /** 父级ID */
    @MpField(value = "pid", columnType = "bigint", comment = "父级ID")
    private Long pid;

    /** 编码 */
    @MpField(value = "area_code", columnType = "string", length = 50, comment = "编码")
    private String areaCode;
}
