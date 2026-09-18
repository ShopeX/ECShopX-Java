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

package cn.shopex.ecshopx.deposit.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 储值规则，充值固定金额送钱或送礼品 */
@Data
@MpTable(value = "deposit_recharge_rule", comment = "储值规则，充值固定金额送钱或送礼品")
public class RechargeRule {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "integer", comment = "ID")
    private Integer id;

    /** 企业ID */
    @MpField(value = "company_id", columnType = "string", comment = "企业ID")
    private String companyId;

    /** 充值固定金额 */
    @MpField(value = "money", columnType = "string", comment = "充值固定金额")
    private String money;

    /** 充值规则类型 */
    @MpField(value = "rule_type", columnType = "string", comment = "充值规则类型")
    private String ruleType;

    /** 充值规则数据 */
    @MpField(value = "rule_data", columnType = "string", comment = "充值规则数据")
    private String ruleData;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "string", comment = "创建时间")
    private String createTime;
}
