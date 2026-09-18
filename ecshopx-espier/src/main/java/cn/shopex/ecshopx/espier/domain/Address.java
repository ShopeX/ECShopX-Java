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

/** 地区表 */
@Data
@MpTable(value = "espier_address", comment = "地区表", indexes = {@MpIndex(name = "ix_parent_id", columns = {"parent_id"}), @MpIndex(name = "ix_label", columns = {"label"})})
public class Address {

    /** 地区id */
    @MpId(value = "id", type = IdType.INPUT, columnType = "bigint", comment = "地区id")
    private Long id;

    /** 地区名称 */
    @MpField(value = "label", columnType = "string", comment = "地区名称")
    private String label;

    /** 父级id */
    @MpField(value = "parent_id", columnType = "bigint", comment = "父级id")
    private Long parentId;

    /** 路径 */
    @MpField(value = "path", columnType = "string", comment = "路径")
    private String path;
}
