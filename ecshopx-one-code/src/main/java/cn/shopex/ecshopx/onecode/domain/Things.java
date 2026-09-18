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

package cn.shopex.ecshopx.onecode.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 物品表，一物一码的物 */
@Data
@MpTable(value = "onecode_things", comment = "物品表，一物一码的物")
public class Things {

    /** 物品ID */
    @MpId(value = "thing_id", type = IdType.AUTO, columnType = "bigint", comment = "物品ID")
    private Long thingId;

    /** 物品名称 */
    @MpField(value = "thing_name", columnType = "string", length = 255, comment = "物品名称")
    private String thingName;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 官方建议售价,单位为‘分’ */
    @MpField(value = "price", columnType = "integer", comment = "官方建议售价,单位为‘分’")
    private Integer price;

    /** 图片 */
    @MpField(value = "pic", columnType = "string", comment = "图片")
    private String pic;

    /** 图文详情 */
    @MpField(value = "intro", columnType = "text", nullable = true, comment = "图文详情")
    private String intro;

    /** 总批次数 */
    @MpField(value = "batch_total_count", columnType = "integer", comment = "总批次数")
    private Integer batchTotalCount = 0;

    /** 总件数 */
    @MpField(value = "batch_total_quantity", columnType = "integer", comment = "总件数")
    private Integer batchTotalQuantity = 0;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
