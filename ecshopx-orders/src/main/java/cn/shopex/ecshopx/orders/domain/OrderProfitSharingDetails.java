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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/** 订单资金分账详情 */
@Data
@MpTable(value = "order_profit_sharing_details", comment = "订单资金分账详情", indexes = {@MpIndex(name = "idx_company_id_order_id", columns = {"company_id", "order_id"}), @MpIndex(name = "idx_sharing_id", columns = {"sharing_id"})})
public class OrderProfitSharingDetails {

    /** ID */
    @MpId(value = "order_profit_sharing_detail_id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long orderProfitSharingDetailId;

    /** 分账ID */
    @MpField(value = "sharing_id", columnType = "bigint", comment = "分账ID")
    private Long sharingId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", comment = "订单号")
    private Long orderId;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 渠道分账账户id */
    @MpField(value = "channel_id", columnType = "string", nullable = true, comment = "渠道分账账户id")
    private String channelId = "";

    /** 渠道分账账户号 */
    @MpField(value = "channel_acct_id", columnType = "string", nullable = true, comment = "渠道分账账户号")
    private String channelAcctId = "";

    /** 分账金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "integer", nullable = true, comment = "分账金额，以分为单位", defaultValue = "0")
    private Integer totalFee = 0;

    /** 创建时间 */
    @MpField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
