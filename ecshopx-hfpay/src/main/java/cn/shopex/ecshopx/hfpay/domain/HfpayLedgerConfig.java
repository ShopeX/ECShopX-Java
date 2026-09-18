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

package cn.shopex.ecshopx.hfpay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 分账配置表
 */
@Data
@MpTable(value = "hfpay_ledger_config", comment = "分账配置表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class HfpayLedgerConfig {

    /** 分账配置表id */
    @MpId(value = "hfpay_ledger_config_id", type = IdType.AUTO, columnType = "bigint", comment = "分账配置表id")
    private Long hfpayLedgerConfigId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 是否开启分账，默认 false */
    @MpField(value = "is_open", columnType = "string", comment = "是否开启分账", defaultValue = "false")
    private String isOpen = "false";

    /**
     * 分账业务模式，默认 1。1 平台/总部；2 店铺独立收款
     */
    @MpField(value = "business_type", columnType = "string", comment = "分账业务模式", defaultValue = "1")
    private String businessType = "1";

    /** 代理商商户号，可为空 */
    @MpField(value = "agent_number", columnType = "string", nullable = true, comment = "代理商商户号")
    private String agentNumber;

    /** 服务商渠道号，可为空 */
    @MpField(value = "provider_number", columnType = "string", nullable = true, comment = "服务商渠道号")
    private String providerNumber;

    /** 平台服务费率 */
    @MpField(value = "rate", columnType = "integer", comment = "平台服务费率")
    private Integer rate;

    /** 绑定的微信小程序appid，可为空 */
    @MpField(value = "app_id", columnType = "string", nullable = true, comment = "绑定的微信小程序appid")
    private String appId;

    /** 创建时间 */
    @MpField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
