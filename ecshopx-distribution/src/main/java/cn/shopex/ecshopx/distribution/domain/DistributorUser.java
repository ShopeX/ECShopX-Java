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

/** 会员对应的导购员信息表 */
@Data
@MpTable(value = "distribution_distributor_user", comment = "会员对应的导购员信息表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"})})
public class DistributorUser {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    @MpField(value = "user_id", columnType = "bigint")
    private Long userId;

    /** 导购员ID */
    @MpField(value = "salesman_id", columnType = "bigint", nullable = true, comment = "导购员ID", defaultValue = "0")
    private Long salesmanId = 0L;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 分销商名称 */
    @MpField(value = "family", columnType = "text", nullable = true, comment = "分销商名称")
    private String family;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
