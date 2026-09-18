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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 店铺导购员表 */
@Data
@MpTable(value = "distribution_distributor_salesman", comment = "店铺导购员表")
public class DistributorSalesman {

    @MpId(value = "salesman_id", type = IdType.AUTO, columnType = "bigint")
    private Long salesmanId;

    /** 上线店铺ID */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "上线店铺ID")
    private Long distributorId;

    /** 企业ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业ID")
    private Long companyId;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", length = 32, comment = "手机号")
    private String mobile;

    /** 姓名 */
    @MpField(value = "salesman_name", columnType = "string", length = 32, comment = "姓名")
    private String salesmanName;

    /** 导购注册人员数量 */
    @MpField(value = "child_count", columnType = "integer", nullable = true, comment = "导购注册人员数量", defaultValue = "0")
    private Integer childCount = 0;

    @MpField(value = "user_id", columnType = "string", nullable = true)
    private String userId;

    /** 是否有效 */
    @MpField(value = "is_valid", columnType = "string", comment = "是否有效")
    private String isValid;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
