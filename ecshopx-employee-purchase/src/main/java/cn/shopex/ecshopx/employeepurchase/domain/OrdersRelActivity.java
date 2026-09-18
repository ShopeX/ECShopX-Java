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

package cn.shopex.ecshopx.employeepurchase.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 内购订单关联活动企业表 */
@Data
@MpTable(value = "employee_purchase_orders_rel_activity", comment = "内购订单关联活动企业表", indexes = {@MpIndex(name = "idx_enterprise_id", columns = {"enterprise_id"}), @MpIndex(name = "idx_activity_id", columns = {"activity_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class OrdersRelActivity {

    /** 购物车ID */
    @MpId(value = "order_id", type = IdType.INPUT, columnType = "bigint", comment = "购物车ID")
    private Long orderId;

    /** 企业id */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业id")
    private Long companyId;

    /** 企业id */
    @MpField(value = "enterprise_id", columnType = "bigint", comment = "企业id")
    private Long enterpriseId;

    /** 活动ID */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动ID")
    private Long activityId;

    /** 用户ID */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户ID")
    private Long userId;

    /** 是否共享库存 */
    @MpField(value = "if_share_store", columnType = "boolean", comment = "是否共享库存", defaultValue = "False")
    private Boolean ifShareStore = false;

    /** 订单关闭修改时间 */
    @MpField(value = "close_modify_time", columnType = "integer", comment = "订单关闭修改时间")
    private Integer closeModifyTime;

    /** 本单是否扣过口令名额 */
    @MpField(value = "participate_quota_order_consumed", columnType = "boolean", comment = "本单是否扣过口令名额", defaultValue = "False")
    private Boolean participateQuotaOrderConsumed = false;

    /** 下单时购买方式快照 */
    @MpField(value = "purchase_mode", columnType = "string", length = 32, nullable = true, comment = "下单时购买方式快照")
    private String purchaseMode;

    /** 预充点本单扣减点数（分，=total_fee） */
    @MpField(value = "prepaid_payable_fee", columnType = "integer", comment = "预充点本单扣减点数（分）", defaultValue = "0")
    private Integer prepaidPayableFee = 0;

    /** 预充点本单已还点数累计（分） */
    @MpField(value = "restored_prepaid_fee", columnType = "integer", comment = "预充点本单已还点数累计（分）", defaultValue = "0")
    private Integer restoredPrepaidFee = 0;
}
