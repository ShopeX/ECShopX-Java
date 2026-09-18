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

/** 限购活动用户表 */
@Data
@MpTable(value = "promotions_limit_person", comment = "限购活动用户表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_distributor_user", columns = {"distributor_id", "user_id"})})
public class LimitPersonPromotions {

    /** 限购活动用户记录表id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "限购活动用户记录表id")
    private Long id;

    /** 限购活动规则id */
    @MpField(value = "limit_id", columnType = "bigint", comment = "限购活动规则id")
    private Long limitId;

    /** 限购活动用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "限购活动用户id")
    private Long userId;

    /** 限购活动商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "限购活动商品id")
    private Long itemId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 数量 */
    @MpField(value = "number", columnType = "bigint", comment = "数量")
    private Long number;

    /** 起始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "起始时间")
    private Integer startTime;

    /** 截止时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "截止时间")
    private Integer endTime;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
