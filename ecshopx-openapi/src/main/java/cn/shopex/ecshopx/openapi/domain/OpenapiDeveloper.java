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

package cn.shopex.ecshopx.openapi.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/** openapi开发配置表 */
@Data
@MpTable(value = "openapi_developer", comment = "openapi开发配置表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class OpenapiDeveloper {

    /** developer_id */
    @MpId(value = "developer_id", type = IdType.AUTO, columnType = "bigint", comment = "developer_id")
    private Long developerId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** app_key */
    @MpField(value = "app_key", columnType = "string", comment = "app_key")
    private String appKey;

    /** app_secret */
    @MpField(value = "app_secret", columnType = "string", comment = "app_secret")
    private String appSecret;

    /** 外部请求配置uri */
    @MpField(value = "external_base_uri", columnType = "string", comment = "外部请求配置uri")
    private String externalBaseUri;

    /** 外部请求配置app_key */
    @MpField(value = "external_app_key", columnType = "string", comment = "外部请求配置app_key")
    private String externalAppKey;

    /** 外部请求配置app_secret */
    @MpField(value = "external_app_secret", columnType = "string", comment = "外部请求配置app_secret")
    private String externalAppSecret;

    /** 创建时间 */
    @MpField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
