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

package cn.shopex.ecshopx.salesperson.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 导购排名 */
@Data
@MpTable(value = "salesperson_leaderboard", comment = "导购排名", indexes = {@MpIndex(name = "idx_company_distributor", columns = {"company_id", "distributor_id"})})
public class Leaderboard {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id")
    private Long distributorId;

    /** 导购id */
    @MpField(value = "salesperson_id", columnType = "bigint", comment = "导购id")
    private Long salespersonId;

    /** 日期 */
    @MpField(value = "date", columnType = "integer", comment = "日期")
    private Integer date;

    /** 销售额 */
    @MpField(value = "sales", columnType = "bigint", comment = "销售额")
    private Long sales;

    /** 销售订单数量 */
    @MpField(value = "number", columnType = "bigint", comment = "销售订单数量")
    private Long number;
}
