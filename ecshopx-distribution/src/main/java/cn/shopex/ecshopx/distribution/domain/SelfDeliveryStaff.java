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

package cn.shopex.ecshopx.distribution.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 自配送员工信息表 */
@Data
@MpTable(value = "self_delivery_staff", comment = "自配送员工信息表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_operator_id", columns = {"operator_id"})})
public class SelfDeliveryStaff {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    @MpField(value = "operator_id", columnType = "bigint")
    private Long operatorId;

    /** 配送员所属店铺id */
    @MpField(value = "distributor_id", columnType = "integer", nullable = true, comment = "配送员所属店铺id", defaultValue = "0")
    private Integer distributorId = 0;

    /** 配送员所属门店id */
    @MpField(value = "shop_id", columnType = "integer", nullable = true, comment = "配送员所属门店id", defaultValue = "0")
    private Integer shopId = 0;

    /**
     * 配送员属性。full_time:全职;part_time:兼职;
     */
    @MpField(value = "staff_attribute", columnType = "string", comment = "配送员属性。full_time:全职;part_time:兼职;", defaultValue = "full_time")
    private String staffAttribute = "full_time";

    /** 配送员编号。 */
    @MpField(value = "staff_no", columnType = "string", nullable = true, comment = "配送员编号。")
    private String staffNo;

    /**
     * 配送员类型。platform:平台;distributor:店铺;shop:商家;
     */
    @MpField(value = "staff_type", columnType = "string", comment = "配送员类型。platform:平台;distributor:店铺;shop:商家;", defaultValue = "distributor")
    private String staffType = "distributor";

    /**
     * 结算方式。order:订单;amount:订单金额;
     */
    @MpField(value = "payment_method", columnType = "string", comment = "结算方式。order:订单;amount:订单金额;", defaultValue = "order")
    private String paymentMethod = "order";

    /** 结合payment_method计算使用，结算费用 分 或 百分比 */
    @MpField(value = "payment_fee", columnType = "integer", comment = "结合payment_method计算使用，结算费用 分 或 百分比", defaultValue = "0")
    private Integer paymentFee = 0;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
