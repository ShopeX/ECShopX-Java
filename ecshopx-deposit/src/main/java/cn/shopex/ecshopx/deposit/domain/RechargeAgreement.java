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

/** 充值协议表 */
@Data
@MpTable(value = "deposit_recharge_agreement", comment = "充值协议表")
public class RechargeAgreement {

    /** 企业ID */
    @MpId(value = "company_id", type = IdType.INPUT, columnType = "string", comment = "企业ID")
    private String companyId;

    /** 协议内容 */
    @MpField(value = "content", columnType = "text", comment = "协议内容")
    private String content;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "string", comment = "创建时间")
    private String createTime;
}
