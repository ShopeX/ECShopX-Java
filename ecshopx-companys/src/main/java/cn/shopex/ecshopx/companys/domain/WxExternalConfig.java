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

import java.time.LocalDateTime;

/**
 * 外部小程序配置表
 */
@Data
@MpTable(value = "wx_external_config", comment = "外部小程序配置表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class WxExternalConfig {

    /** 外部小程序配置表id */
    @MpId(value = "wx_external_config_id", type = IdType.AUTO, columnType = "bigint", comment = "外部小程序配置表id")
    private Long wxExternalConfigId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 小程序APPID */
    @MpField(value = "app_id", columnType = "string", comment = "小程序APPID")
    private String appId;

    /** 小程序名称 */
    @MpField(value = "app_name", columnType = "string", nullable = true, comment = "小程序名称")
    private String appName;

    /** 描述 */
    @MpField(value = "app_desc", columnType = "string", nullable = true, comment = "描述")
    private String appDesc;

    @MpField("created_at")
    private LocalDateTime createdAt;

    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
