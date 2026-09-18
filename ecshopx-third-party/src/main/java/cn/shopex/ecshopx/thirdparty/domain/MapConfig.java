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
@MpTable(value = "map_config", comment = "地图配置表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class MapConfig {

    /** 地区id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "地区id")
    private Long id;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 第三方类型【amap 高德地图】【tencent 腾讯地图】 */
    @MpField(value = "type", columnType = "string", length = 20, comment = "第三方类型【amap 高德地图】【tencent 腾讯地图】")
    private String type;

    /** 第三方控制台中生成的key */
    @MpField(value = "app_key", columnType = "string", comment = "第三方控制台中生成的key")
    private String appKey = "";

    /** 第三方控制台中生成的秘钥 */
    @MpField(value = "app_secret", columnType = "string", comment = "第三方控制台中生成的秘钥")
    private String appSecret = "";

    /** 是否是默认的地图配置项 */
    @MpField(value = "is_default", columnType = "boolean", comment = "是否是默认的地图配置项", defaultValue = "0")
    private Boolean isDefault = false;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
