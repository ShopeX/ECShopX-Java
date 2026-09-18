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

package cn.shopex.ecshopx.crossborder.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 跨境-税费策略
 */
@Data
@MpTable(value = "crossborder_taxstrategy", comment = "跨境-税费策略", indexes = {@MpIndex(name = "ix_id", columns = {"id"}), @MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class Taxstrategy {

    /** 设置id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "设置id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 规则名称 */
    @MpField(value = "taxstrategy_name", columnType = "string", nullable = true, comment = "规则名称")
    private String taxstrategyName;

    /** 策略内容 */
    @MpField(value = "taxstrategy_content", columnType = "text", nullable = true, comment = "策略内容")
    private String taxstrategyContent;

    /** 数据状态(1正常，-1删除) */
    @MpField(value = "state", columnType = "integer", comment = "数据状态(1正常，-1删除)")
    private Integer state;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
