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

package cn.shopex.ecshopx.popularize.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 分销佣金表 */
@Data
@MpTable(value = "popularize_brokerage", comment = "分销佣金表")
public class Brokerage {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 佣金类型 */
    @MpField(value = "brokerage_type", columnType = "string", length = 15, comment = "佣金类型")
    private String brokerageType;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", length = 64, nullable = true, comment = "订单号")
    private String orderId;

    @MpField(value = "user_id", columnType = "bigint")
    private Long userId;

    @MpField(value = "buy_user_id", columnType = "bigint")
    private Long buyUserId;

    /** 订单类型 */
    @MpField(value = "order_type", columnType = "string", length = 15, comment = "订单类型")
    private String orderType;

    /** 佣金来源 订单,邀请等 */
    @MpField(value = "source", columnType = "string", length = 15, comment = "佣金来源 订单,邀请等")
    private String source;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 销售金额,单位为‘分’ */
    @MpField(value = "price", columnType = "integer", comment = "销售金额,单位为‘分’")
    private Integer price;

    /** 返佣类型 money 金额 point 积分 */
    @MpField(value = "commission_type", columnType = "string", length = 15, comment = "返佣类型 money 金额 point 积分")
    private String commissionType;

    /** 返佣金额,单位为‘分’ */
    @MpField(value = "rebate", columnType = "integer", comment = "返佣金额,单位为‘分’")
    private Integer rebate;

    /**
     * 返佣金额,单位为‘分’；默认 money
     */
    @MpField(value = "rebate_point", columnType = "string", length = 15, nullable = true, comment = "返佣金额,单位为‘分’", defaultValue = "money")
    private String rebatePoint = "money";

    /** 佣金计算详情 */
    @MpField(value = "detail", columnType = "text", comment = "佣金计算详情")
    private String detail;

    /** 是否已结算 */
    @MpField(value = "is_close", columnType = "boolean", comment = "是否已结算", defaultValue = "False")
    private Boolean isClose = false;

    /** 计划结算时间 */
    @MpField(value = "plan_close_time", columnType = "integer", comment = "计划结算时间")
    private Integer planCloseTime;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
