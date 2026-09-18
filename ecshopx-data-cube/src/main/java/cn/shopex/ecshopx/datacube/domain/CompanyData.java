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

package cn.shopex.ecshopx.datacube.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.time.LocalDate;

/**
 * 商城数据统计表。唯一约束：count_date、company_id、act_id（ix_date_company_act）。
 */
@Data
@MpTable(value = "datacube_company_data", comment = "商城数据统计表", uniqueIndexes = {@MpIndex(name = "ix_date_company_act", columns = {"count_date", "company_id", "act_id"})})
public class CompanyData {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 日期 */
    @MpField(value = "count_date", columnType = "date", comment = "日期")
    private LocalDate countDate;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 新增会员数，默认 0 */
    @MpField(value = "member_count", columnType = "integer", comment = "新增会员数", defaultValue = "0")
    private Integer memberCount = 0;

    /** 新增售后单数，默认 0 */
    @MpField(value = "aftersales_count", columnType = "integer", comment = "新增售后单数", defaultValue = "0")
    private Integer aftersalesCount = 0;

    /** 新增退款额，默认 0 */
    @MpField(value = "refunded_count", columnType = "integer", comment = "新增退款额", defaultValue = "0")
    private Integer refundedCount = 0;

    /** 新增交易额，默认 0 */
    @MpField(value = "amount_payed_count", columnType = "bigint", comment = "新增交易额", defaultValue = "0")
    private Long amountPayedCount = 0L;

    /** 新增订单数，默认 0 */
    @MpField(value = "order_count", columnType = "integer", comment = "新增订单数", defaultValue = "0")
    private Integer orderCount = 0;

    /** 新增已付款订单数，默认 0 */
    @MpField(value = "order_payed_count", columnType = "integer", comment = "新增已付款订单数", defaultValue = "0")
    private Integer orderPayedCount = 0;

    /** 新增 gmv，默认 0 */
    @MpField(value = "gmv_count", columnType = "bigint", comment = "新增gmv", defaultValue = "0")
    private Long gmvCount = 0L;

    /** 订单种类,可选值有 employee_purchase:内购订单，可为空 */
    @MpField(value = "order_class", columnType = "string", nullable = true, comment = "订单种类,可选值有 employee_purchase:内购订单")
    private String orderClass;

    /** 活动ID，默认 0 */
    @MpField(value = "act_id", columnType = "bigint", comment = "活动ID", defaultValue = "0")
    private Long actId = 0L;
}
