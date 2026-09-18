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
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

/** 物品批次表 */
@Data
@MpTable(value = "onecode_batchs", autoResultMap = true, comment = "物品批次表")
public class Batchs {

    /** 批次ID */
    @MpId(value = "batch_id", type = IdType.AUTO, columnType = "bigint", comment = "批次ID")
    private Long batchId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 物品ID */
    @MpField(value = "thing_id", columnType = "bigint", comment = "物品ID")
    private Long thingId;

    /** 批次编号 */
    @MpField(value = "batch_number", columnType = "string", length = 255, comment = "批次编号")
    private String batchNumber;

    /** 批次名称 */
    @MpField(value = "batch_name", columnType = "string", comment = "批次名称")
    private String batchName;

    /** 批次件数 */
    @MpField(value = "batch_quantity", columnType = "integer", comment = "批次件数")
    private Integer batchQuantity;

    /** 前台是否可以查看流通信息 */
    @MpField(value = "show_trace", columnType = "boolean", comment = "前台是否可以查看流通信息", defaultValue = "True")
    private Boolean showTrace = true;

    /** 流通信息 */
    @MpField(value = "trace_info", typeHandler = JacksonTypeHandler.class, columnType = "json_array", nullable = true, comment = "流通信息")
    private Object traceInfo;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
