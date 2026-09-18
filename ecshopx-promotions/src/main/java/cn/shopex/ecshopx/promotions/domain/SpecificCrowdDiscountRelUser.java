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

/** 定向促销会员日志 */
@Data
@MpTable(value = "promotions_scd_rel_user", comment = "定向促销会员日志", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_user_id", columns = {"user_id"})})
public class SpecificCrowdDiscountRelUser {

    /** 营销id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "营销id")
    private Long id;

    /** 企业id */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业id")
    private Long companyId;

    /** 企业id */
    @MpField(value = "user_id", columnType = "bigint", comment = "企业id")
    private Long userId;

    /** 订单号id */
    @MpField(value = "order_id", columnType = "string", comment = "订单号id")
    private String orderId;

    /** 优惠金额 */
    @MpField(value = "discount_fee", columnType = "bigint", comment = "优惠金额")
    private Long discountFee;

    /** 定向促销id */
    @MpField(value = "activity_id", columnType = "bigint", comment = "定向促销id")
    private Long activityId;

    /** 定向条件id */
    @MpField(value = "specific_id", columnType = "bigint", nullable = true, comment = "定向条件id")
    private Long specificId;

    /** 定向条件名称 */
    @MpField(value = "specific_name", columnType = "string", nullable = true, comment = "定向条件名称")
    private String specificName;

    /** 促销月份 */
    @MpField(value = "activity_month", columnType = "string", nullable = true, comment = "促销月份")
    private String activityMonth;

    /** 操作方式，plus:加，less:减 */
    @MpField(value = "action_type", columnType = "string", comment = "操作方式，plus:加，less:减", defaultValue = "plus")
    private String actionType = "plus";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
