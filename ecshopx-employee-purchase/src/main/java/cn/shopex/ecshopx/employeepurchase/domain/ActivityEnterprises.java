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

/** 活动参与企业表 */
@Data
@MpTable(value = "employee_purchase_activity_enterprises", comment = "活动参与企业表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_activity_id", columns = {"activity_id"}), @MpIndex(name = "idx_enterprise_id", columns = {"enterprise_id"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"activity_id", "enterprise_id"})})
public class ActivityEnterprises {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 活动ID */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动ID")
    private Long activityId;

    /** 企业ID */
    @MpField(value = "enterprise_id", columnType = "bigint", comment = "企业ID")
    private Long enterpriseId;

    /** 人均可购买额度/预充点数（分）；新活动必填 */
    @MpField(value = "per_capita_limitfee", columnType = "integer", nullable = true, comment = "人均可购买额度/预充点数（分）；新活动必填")
    private Integer perCapitaLimitfee;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;
}
