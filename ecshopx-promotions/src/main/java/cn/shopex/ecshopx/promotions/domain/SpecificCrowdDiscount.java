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

/** 特定人群促销 */
@Data
@MpTable(value = "promotions_specific_crowd_discount", comment = "特定人群促销", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_status", columns = {"status"}), @MpIndex(name = "ix_specific_id", columns = {"specific_id"})})
public class SpecificCrowdDiscount {

    /** 营销id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "营销id")
    private Long id;

    /** 企业id */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业id")
    private Long companyId;

    /**
     * 特定人群类型。member_tag：会员标签
     */
    @MpField(value = "specific_type", columnType = "string", comment = "特定人群类型", defaultValue = "member_tag")
    private String specificType = "member_tag";

    /** 营销id */
    @MpField(value = "specific_id", columnType = "bigint", nullable = true, comment = "营销id")
    private Long specificId;

    /** 周期类型,1:自然月;2:指定时段 */
    @MpField(value = "cycle_type", columnType = "integer", comment = "周期类型,1:自然月;2:指定时段", defaultValue = "1")
    private Integer cycleType = 1;

    /** 开始时间 */
    @MpField(value = "start_time", columnType = "bigint", nullable = true, comment = "开始时间")
    private Long startTime;

    /** 结束时间 */
    @MpField(value = "end_time", columnType = "bigint", nullable = true, comment = "结束时间")
    private Long endTime;

    /** 折扣值 */
    @MpField(value = "discount", columnType = "bigint", nullable = true, comment = "折扣值")
    private Long discount;

    /** 每人累计限额 */
    @MpField(value = "limit_total_money", columnType = "integer", nullable = true, comment = "每人累计限额")
    private Integer limitTotalMoney;

    /** 状态，1:暂存，2:已发布, 3:停用, 4:已过期 */
    @MpField(value = "status", columnType = "bigint", comment = "状态，1:暂存，2:已发布, 3:停用, 4:已过期")
    private Long status = 1L;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
