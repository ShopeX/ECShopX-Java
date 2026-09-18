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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 企业设置表
 */
@Data
@MpTable(value = "companys_setting", comment = "企业设置表")
public class Setting {

    /** 公司id */
    @MpId(value = "company_id", type = IdType.INPUT, columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 社区相关设置 */
    @MpField(value = "community_config", columnType = "text", nullable = true, comment = "社区相关设置")
    private String communityConfig;

    /** 提现支持银行类型 */
    @MpField(value = "withdraw_bank", columnType = "text", nullable = true, comment = "提现支持银行类型")
    private String withdrawBank;

    /** 客服电话 */
    @MpField(value = "consumer_hotline", columnType = "string", nullable = true, comment = "客服电话")
    private String consumerHotline;

    /** 客服开关 */
    @MpField(value = "customer_switch", columnType = "integer", comment = "客服开关", defaultValue = "0")
    private Integer customerSwitch = 0;

    /** 发票相关设置 */
    @MpField(value = "fapiao_config", columnType = "text", nullable = true, comment = "发票相关设置")
    private String fapiaoConfig;

    /** 发票开关 */
    @MpField(value = "fapiao_switch", columnType = "integer", comment = "发票开关", defaultValue = "0")
    private Integer fapiaoSwitch = 0;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
