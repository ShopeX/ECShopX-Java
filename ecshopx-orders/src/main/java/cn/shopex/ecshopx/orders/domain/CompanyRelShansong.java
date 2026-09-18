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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 商城关联闪送应用配置信息表 */
@Data
@MpTable(value = "company_rel_shansong", comment = "商城关联闪送应用配置信息表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class CompanyRelShansong {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 商户ID */
    @MpField(value = "shop_id", columnType = "string", comment = "商户ID")
    private String shopId;

    /** App-key */
    @MpField(value = "client_id", columnType = "string", comment = "App-key")
    private String clientId;

    /** App-密钥 */
    @MpField(value = "app_secret", columnType = "string", comment = "App-密钥")
    private String appSecret;

    /** 是否上线:0:未开启，1:已开启 */
    @MpField(value = "online", columnType = "boolean", comment = "是否上线:0:未开启，1:已开启", defaultValue = "0")
    private Boolean online;

    /** 运费承担方:0:商家承担，1:买家承担 */
    @MpField(value = "freight_type", columnType = "boolean", comment = "运费承担方:0:商家承担，1:买家承担", defaultValue = "0")
    private Boolean freightType;

    /** 是否开启:0:未开启，1:已开启 */
    @MpField(value = "is_open", columnType = "boolean", comment = "是否开启:0:未开启，1:已开启", defaultValue = "0")
    private Boolean isOpen;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer", nullable = true, comment = "创建时间")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
