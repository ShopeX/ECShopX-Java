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

package cn.shopex.ecshopx.thirdparty.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 地图配置表 */
@Data
@MpTable(value = "map_config_service", comment = "地图配置表", indexes = {@MpIndex(name = "ix_company_config_type", columns = {"company_id", "config_id", "type", "status"})})
public class MapConfigService {

    /** 本地的唯一id，如果外部表要与该表做关联，需要使用该id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "本地的唯一id，如果外部表要与该表做关联，需要使用该id")
    private Long id;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 地图配置的id */
    @MpField(value = "config_id", columnType = "bigint", comment = "地图配置的id")
    private Long configId;

    /** 服务的类型 */
    @MpField(value = "type", columnType = "smallint", comment = "服务的类型")
    private Integer type;

    /** 第三方平台提供的服务id */
    @MpField(value = "service_id", columnType = "string", length = 20, comment = "第三方平台提供的服务id")
    private String serviceId;

    /** json内容，第三方平台中的服务数据 */
    @MpField(value = "service_data", columnType = "text", comment = "json内容，第三方平台中的服务数据")
    private String serviceData;

    /** 状态【1 生效】【0 失效】 */
    @MpField(value = "status", columnType = "smallint", comment = "状态【1 生效】【0 失效】", defaultValue = "1")
    private Integer status = 1;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
