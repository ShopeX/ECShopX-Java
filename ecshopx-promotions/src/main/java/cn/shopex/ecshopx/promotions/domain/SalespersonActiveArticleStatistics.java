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

package cn.shopex.ecshopx.promotions.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 导购员活动转发数据统计表 */
@Data
@MpTable(value = "salesperson_active_article_statistics", comment = "导购员活动转发数据统计表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_salesperson_id", columns = {"salesperson_id"}), @MpIndex(name = "ix_add_date", columns = {"add_date"})})
public class SalespersonActiveArticleStatistics {

    /** 激活id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "激活id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 导购员id */
    @MpField(value = "salesperson_id", columnType = "bigint", comment = "导购员id")
    private Long salespersonId;

    /** 统计日期 Ymd */
    @MpField(value = "add_date", columnType = "integer", comment = "统计日期 Ymd")
    private Integer addDate;

    /** 统计数据 */
    @MpField(value = "data_value", columnType = "integer", comment = "统计数据", defaultValue = "0")
    private Integer dataValue = 0;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
