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

package cn.shopex.ecshopx.hfpay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 汇付记账表
 */
@Data
@MpTable(value = "hfpay_trade_record", comment = "汇付记账表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class HfpayTradeRecord {

    /** 汇付记账表 */
    @MpId(value = "hfpay_trade_record_id", type = IdType.AUTO, columnType = "bigint", comment = "汇付记账表")
    private Long hfpayTradeRecordId;

    /** company_id，可为空 */
    @MpField(value = "company_id", columnType = "integer", nullable = true, comment = "company_id")
    private Integer companyId;

    /** 店铺id，可为空 */
    @MpField(value = "distributor_id", columnType = "string", nullable = true, comment = "店铺id")
    private String distributorId;

    /** 业务id，可为空 */
    @MpField(value = "trade_id", columnType = "string", nullable = true, comment = "业务id")
    private String tradeId;

    /** 订单id，可为空 */
    @MpField(value = "outer_order_id", columnType = "string", nullable = true, comment = "订单id")
    private String outerOrderId;

    /** 用户，可为空 */
    @MpField(value = "form_user_id", columnType = "string", nullable = true, comment = "用户")
    private String formUserId;

    /** 对象id，可为空 */
    @MpField(value = "target_user_id", columnType = "string", nullable = true, comment = "对象id")
    private String targetUserId;

    /** 时间戳，可为空 */
    @MpField(value = "trade_time", columnType = "string", nullable = true, comment = "时间戳")
    private String tradeTime;

    /** 交易类型，可为空 */
    @MpField(value = "trade_type", columnType = "integer", nullable = true, comment = "交易类型")
    private Integer tradeType;

    /** 财务科目，可为空 */
    @MpField(value = "fin_type", columnType = "string", nullable = true, comment = "财务科目")
    private String finType;

    /** 收入，可为空 */
    @MpField(value = "income", columnType = "integer", nullable = true, comment = "收入")
    private Integer income;

    /** 支出，可为空 */
    @MpField(value = "outcome", columnType = "integer", nullable = true, comment = "支出")
    private Integer outcome;

    /**
     * 结算状态 0未结算 1已结算，可为空，默认 0
     */
    @MpField(value = "is_clean", columnType = "integer", nullable = true, comment = "结算状态 0未结算 1已结算", defaultValue = "0")
    private Integer isClean = 0;

    /** 结算时间，可为空 */
    @MpField(value = "clean_time", columnType = "integer", nullable = true, comment = "结算时间")
    private Integer cleanTime;

    /** 描述，可为空 */
    @MpField(value = "message", columnType = "string", nullable = true, comment = "描述")
    private String message;

    /** 创建时间 */
    @MpField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
