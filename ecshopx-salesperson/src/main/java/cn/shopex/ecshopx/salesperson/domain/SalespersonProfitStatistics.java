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

package cn.shopex.ecshopx.salesperson.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 导购员销售提成数据统计表 */
@Data
@MpTable(value = "salesperson_profit_statistics", comment = "导购员销售提成数据统计表", indexes = {@MpIndex(name = "ix_salesperson_date", columns = {"salesperson_id", "date"})})
public class SalespersonProfitStatistics {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id")
    private Long distributorId;

    /** 导购员id */
    @MpField(value = "salesperson_id", columnType = "bigint", comment = "导购员id")
    private Long salespersonId;

    /** 统计日期 Ymd */
    @MpField(value = "date", columnType = "bigint", comment = "统计日期 Ymd")
    private Long date;

    /** 未确认绑定会员提成 */
    @MpField(value = "unconfirmed_seller_fee", columnType = "bigint", comment = "未确认绑定会员提成")
    private Long unconfirmedSellerFee;

    /** 已确认绑定会员提成 */
    @MpField(value = "confirm_seller_fee", columnType = "bigint", comment = "已确认绑定会员提成")
    private Long confirmSellerFee;

    /** 未确认门店开单提成 */
    @MpField(value = "unconfirmed_offline_seller_fee", columnType = "bigint", comment = "未确认门店开单提成")
    private Long unconfirmedOfflineSellerFee;

    /** 已确认门店开单提成 */
    @MpField(value = "confirm_offline_seller_fee", columnType = "bigint", comment = "已确认门店开单提成")
    private Long confirmOfflineSellerFee;

    /** 未确认客户推广提成 */
    @MpField(value = "unconfirmed_popularize_seller_fee", columnType = "bigint", comment = "未确认客户推广提成")
    private Long unconfirmedPopularizeSellerFee;

    /** 已确认客户推广提成 */
    @MpField(value = "confirm_popularize_seller_fee", columnType = "bigint", comment = "已确认客户推广提成")
    private Long confirmPopularizeSellerFee;
}
