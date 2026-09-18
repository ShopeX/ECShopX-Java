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

package cn.shopex.ecshopx.promotions.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 短信模板表 */
@Data
@MpTable(value = "sms_template", comment = "短信模板表")
public class SmsTemplate {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** company_id */
    @MpField(value = "company_id", columnType = "bigint", comment = "company_id")
    private Long companyId;

    /** 短信类型 */
    @MpField(value = "sms_type", columnType = "string", comment = "短信类型", defaultValue = "notice")
    private String smsType = "notice";

    /** 模板分类 */
    @MpField(value = "tmpl_type", columnType = "string", comment = "模板分类")
    private String tmplType;

    /** 模板内容 */
    @MpField(value = "content", columnType = "text", comment = "模板内容")
    private String content;

    /** 是否开启 */
    @MpField(value = "is_open", columnType = "string", comment = "是否开启")
    private String isOpen;

    /** 模板名称 */
    @MpField(value = "tmpl_name", columnType = "string", comment = "模板名称")
    private String tmplName;

    /** 短信发送触发时间描述 */
    @MpField(value = "send_time_desc", columnType = "string", comment = "短信发送触发时间描述")
    private String sendTimeDesc;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
