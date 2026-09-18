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

package cn.shopex.ecshopx.members.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 群发短信log */
@Data
@MpTable(value = "members_msmsend_log", comment = "群发短信log")
public class MemberSmsLog {

    /** 用户id */
    @MpId(value = "log_id", type = IdType.AUTO, columnType = "bigint", comment = "用户id")
    private Long logId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 收信手机号 */
    @MpField(value = "send_to_phones", columnType = "text", comment = "收信手机号")
    private String sendToPhones;

    /** 短信内容 */
    @MpField(value = "sms_content", columnType = "string", length = 256, comment = "短信内容")
    private String smsContent;

    /** 操作员信息 */
    @MpField(value = "operator", columnType = "string", length = 50, comment = "操作员信息")
    private String operator;

    /** 状态 */
    @MpField(value = "status", columnType = "integer", comment = "状态")
    private Integer status = 1;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
