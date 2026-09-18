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

package cn.shopex.ecshopx.wsugc.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** ugc通用设置 */
@Data
@MpTable(value = "wsugc_setting", comment = "ugc通用设置", indexes = {@MpIndex(name = "idx_type", columns = {"type"}), @MpIndex(name = "idx_keyname", columns = {"keyname"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class Setting {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 键名 */
    @MpField(value = "keyname", columnType = "string", nullable = true, comment = "键名")
    private String keyname;

    /** 值 */
    @MpField(value = "value", columnType = "text", nullable = true, comment = "值")
    private String value;

    /** 类型 */
    @MpField(value = "type", columnType = "string", nullable = true, comment = "类型")
    private String type;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 添加时间 */
    @MpField(value = "created", columnType = "integer", comment = "添加时间")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
