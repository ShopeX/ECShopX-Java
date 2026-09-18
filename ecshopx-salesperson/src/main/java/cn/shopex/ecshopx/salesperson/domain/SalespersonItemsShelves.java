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

/** 导购活动商品上架 */
@Data
@MpTable(value = "salesperson_items_shelves", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_activity_type", columns = {"activity_type"}), @MpIndex(name = "ix_item_id", columns = {"item_id"})})
public class SalespersonItemsShelves {

    /** 活动ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "活动ID")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 活动id */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动id")
    private Long activityId;

    /** 活动类型 full_discount:满折,full_minus:满减,full_gift:满赠,self_select:任选优惠,plus_price_buy:加价购,group拼团,seckill秒杀,package打包,limited_time_sale限时特惠 */
    @MpField(value = "activity_type", columnType = "string", comment = "活动类型 full_discount:满折,full_minus:满减,full_gift:满赠,self_select:任选优惠,plus_price_buy:加价购,group拼团,seckill秒杀,package打包,limited_time_sale限时特惠")
    private String activityType;

    /** 门店id, 0是所有门店 */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "门店id, 0是所有门店", defaultValue = "0")
    private Long distributorId = 0L;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 活动开始时间 */
    @MpField(value = "start_time", columnType = "bigint", comment = "活动开始时间")
    private Long startTime;

    /** 活动结束时间 */
    @MpField(value = "end_time", columnType = "bigint", comment = "活动结束时间")
    private Long endTime;
}
