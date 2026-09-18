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

package cn.shopex.ecshopx.workwechat.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 企业微信通知模板 */
@Data
@MpTable(value = "work_wechat_message_template", comment = "企业微信通知模板")
public class WorkWechatMessageTemplate {

    /** 企业微信通知模板 */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "企业微信通知模板")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 企业微信通知模板id */
    @MpField(value = "template_id", columnType = "string", comment = "企业微信通知模板id")
    private String templateId;

    /** 模版是否开启 */
    @MpField(value = "disabled", columnType = "boolean", comment = "模版是否开启", defaultValue = "False")
    private Boolean disabled = false;

    /** 是否放大第一个 */
    @MpField(value = "emphasis_first_item", columnType = "boolean", comment = "是否放大第一个", defaultValue = "False")
    private Boolean emphasisFirstItem = false;

    /** 企业微信通知模板标题 */
    @MpField(value = "title", columnType = "string", nullable = true, comment = "企业微信通知模板标题")
    private String title = "";

    /** 企业微信通知模板内容 */
    @MpField(value = "description", columnType = "string", nullable = true, comment = "企业微信通知模板内容")
    private String description = "";

    /** 通知主体消息（DB JSON；持久化多为字符串，部分驱动/处理器可能为 Map/List） */
    @MpField(value = "content", columnType = "json_array", nullable = true, comment = "通知主体消息")
    private Object content;
}
