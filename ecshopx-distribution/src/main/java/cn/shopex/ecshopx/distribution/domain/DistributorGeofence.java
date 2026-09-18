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

/** 店铺围栏信息 */
@Data
@MpTable(value = "distribution_distributor_geofence", comment = "店铺围栏信息", indexes = {@MpIndex(name = "ix_company_distributor_service", columns = {"company_id", "distributor_id", "config_service_local_id", "status"})})
public class DistributorGeofence {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "自增id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id")
    private Long distributorId;

    /** 地图配置的本地主键ID */
    @MpField(value = "config_service_local_id", columnType = "bigint", comment = "地图配置的本地主键ID")
    private Long configServiceLocalId;

    /** 第三方服务的围栏ID */
    @MpField(value = "geofence_id", columnType = "bigint", comment = "第三方服务的围栏ID")
    private String geofenceId;

    /** json数据，第三方服务的围栏信息 */
    @MpField(value = "geofence_data", columnType = "text", comment = "json数据，第三方服务的围栏信息")
    private String geofenceData;

    /** 状态【1 启用】【0 禁用】 */
    @MpField(value = "status", columnType = "smallint", comment = "状态【1 启用】【0 禁用】", defaultValue = "1")
    private Integer status = 1;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
