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

/** 导购任务表 */
@Data
@MpTable(value = "salesperson_task_rel_distributor", comment = "导购任务表", uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"task_id", "company_id", "distributor_id"})})
public class SalespersonTaskRelDistributor {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** ID */
    @MpField(value = "task_id", columnType = "bigint", comment = "ID")
    private Long taskId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 门店id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "门店id")
    private Long distributorId;
}
