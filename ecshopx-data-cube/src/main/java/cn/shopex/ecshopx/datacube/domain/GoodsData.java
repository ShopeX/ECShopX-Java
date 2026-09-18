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
 * 商品数据统计表。唯一约束：count_date、company_id、item_id、act_id（ix_date_goods_act）。
 */
@Data
@MpTable(value = "datacube_goods_data", comment = "商品数据统计表", uniqueIndexes = {@MpIndex(name = "ix_date_goods_act", columns = {"count_date", "company_id", "item_id", "act_id"})})
public class GoodsData {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 日期 */
    @MpField(value = "count_date", columnType = "date", comment = "日期")
    private LocalDate countDate;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 新增销量，默认 0 */
    @MpField(value = "sales_count", columnType = "integer", comment = "新增销量", defaultValue = "0")
    private Integer salesCount = 0;

    /** 新增成交额(优惠前)，默认 0 */
    @MpField(value = "fixed_amount_count", columnType = "bigint", comment = "新增成交额(优惠前)", defaultValue = "0")
    private Long fixedAmountCount = 0L;

    /** 新增成交额(实付价)，默认 0 */
    @MpField(value = "settle_amount_count", columnType = "bigint", comment = "新增成交额(实付价)", defaultValue = "0")
    private Long settleAmountCount = 0L;

    /** 商户id，默认 0 */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;

    /** 订单种类,可选值有 employee_purchase:内购订单，可为空 */
    @MpField(value = "order_class", columnType = "string", nullable = true, comment = "订单种类,可选值有 employee_purchase:内购订单")
    private String orderClass;

    /** 活动ID，默认 0 */
    @MpField(value = "act_id", columnType = "bigint", comment = "活动ID", defaultValue = "0")
    private Long actId = 0L;
}
