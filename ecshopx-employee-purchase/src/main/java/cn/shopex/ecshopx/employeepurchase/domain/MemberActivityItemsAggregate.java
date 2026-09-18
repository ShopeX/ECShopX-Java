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

/** 内购活动商品会员累计使用额度 */
@Data
@MpTable(value = "employee_purchase_member_activity_items_aggregate", comment = "内购活动商品会员累计使用额度", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class MemberActivityItemsAggregate {

    /** 活动id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "活动id")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 企业id */
    @MpField(value = "enterprise_id", columnType = "bigint", comment = "企业id")
    private Long enterpriseId;

    /** 会员ID */
    @MpField(value = "user_id", columnType = "bigint", comment = "会员ID")
    private Long userId;

    /** 活动ID */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动ID")
    private Long activityId;

    /** 商品ID */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品ID")
    private Long itemId;

    /** 累计使用额度，以分为单位 */
    @MpField(value = "aggregate_fee", columnType = "integer", comment = "累计使用额度，以分为单位")
    private Integer aggregateFee;

    /** 累计购买数量 */
    @MpField(value = "aggregate_num", columnType = "integer", comment = "累计购买数量")
    private Integer aggregateNum;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
