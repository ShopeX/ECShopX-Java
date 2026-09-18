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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 地区权限
 */
@Data
@MpTable(value = "companys_regionauth", comment = "地区权限", indexes = {@MpIndex(name = "ix_regionauth_id", columns = {"regionauth_id"}), @MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class Regionauth {

    /** 地区id */
    @MpId(value = "regionauth_id", type = IdType.AUTO, columnType = "bigint", comment = "地区id")
    private Long regionauthId;

    /** 地区名称 */
    @MpField(value = "regionauth_name", columnType = "string", length = 50, nullable = true, comment = "地区名称")
    private String regionauthName;

    /** 数据状态(1正常，-1删除) */
    @MpField(value = "state", columnType = "integer", comment = "数据状态(1正常，-1删除)")
    private Integer state;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
