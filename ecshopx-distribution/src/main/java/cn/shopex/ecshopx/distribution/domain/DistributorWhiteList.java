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

/** 店铺表 */
@Data
@MpTable(value = "distribution_distributor_white_list", comment = "店铺表", indexes = {@MpIndex(name = "ix_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "ix_mobile", columns = {"mobile"})})
public class DistributorWhiteList {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "True")
    private Long distributorId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺手机号 */
    @MpField(value = "mobile", columnType = "string", length = 50, comment = "店铺手机号")
    private String mobile;

    /** 名称 */
    @MpField(value = "username", columnType = "string", nullable = true, comment = "名称")
    private String username;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
