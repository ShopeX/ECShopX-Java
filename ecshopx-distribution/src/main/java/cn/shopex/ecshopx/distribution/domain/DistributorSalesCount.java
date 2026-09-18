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

/** 店铺销量表 */
@Data
@MpTable(value = "distribution_distributor_sales_count", comment = "店铺销量表", indexes = {@MpIndex(name = "ix_company_id_distributor_id_time", columns = {"company_id", "distributor_id", "year_month_time"})})
public class DistributorSalesCount {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id")
    private Long distributorId;

    /** 已经关闭售后的订单的商品数量 */
    @MpField(value = "order_item_count", columnType = "bigint", comment = "已经关闭售后的订单的商品数量", defaultValue = "0")
    private Long orderItemCount = 0L;

    /** 已经关闭售后的订单的商品数量 */
    @MpField(value = "aftersales_item_count", columnType = "bigint", comment = "已经关闭售后的订单的商品数量", defaultValue = "0")
    private Long aftersalesItemCount = 0L;

    /** 统计的年月时间 */
    @MpField(value = "year_month_time", columnType = "bigint", comment = "统计的年月时间")
    private Long yearMonthTime;
}
