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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 导购挂单
 */
@Data
@MpTable(value = "companys_operator_pending_order", comment = "导购挂单", indexes = {@MpIndex(name = "idx_operator_id", columns = {"operator_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class OperatorPendingOrder {

    /** 挂单ID */
    @MpId(value = "pending_id", type = IdType.AUTO, columnType = "bigint", comment = "挂单ID")
    private Long pendingId;

    /** 企业id */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 管理员id */
    @MpField(value = "operator_id", columnType = "bigint", comment = "管理员id")
    private Long operatorId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id", defaultValue = "0")
    private Long userId = 0L;

    /** 挂起类型 cart:收银台 order:订单 */
    @MpField(value = "pending_type", columnType = "string", comment = "挂起类型 cart:收银台 order:订单", defaultValue = "cart")
    private String pendingType = "cart";

    /** 暂存数据 */
    @MpField(value = "pending_data", columnType = "text", comment = "暂存数据")
    private String pendingData;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
