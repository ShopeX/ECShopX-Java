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
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 跨境-设置
 */
@Data
@MpTable(value = "crossborder_set", comment = "跨境-设置", indexes = {@MpIndex(name = "ix_id", columns = {"id"}), @MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class CrossBorderSet {

    /** 设置id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "设置id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 税率 */
    @MpField(value = "tax_rate", columnType = "string", length = 10, comment = "税率")
    private String taxRate;

    /** 额度提醒 */
    @MpField(value = "quota_tip", columnType = "text", length = 1000, nullable = true, comment = "额度提醒")
    private String quotaTip;

    /** 跨境物流 */
    @MpField(value = "logistics", insertStrategy = FieldStrategy.NOT_NULL, columnType = "string", length = 10, comment = "跨境物流")
    private String logistics;

    /**
     * 跨境显示,0不显示，1显示
     */
    @MpField(value = "crossborder_show", columnType = "integer", length = 4, comment = "跨境显示,0不显示，1显示", defaultValue = "0")
    private Integer crossborderShow = 0;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
