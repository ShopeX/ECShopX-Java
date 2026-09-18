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

/** 小程序通知消息模版 */
@Data
@MpTable(value = "promotions_notice_template", comment = "小程序通知消息模版")
public class WxaNoticeTemplate {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 小程序模板名称 yykweishop 微商城等 */
    @MpField(value = "template_name", columnType = "string", comment = "小程序模板名称 yykweishop 微商城等")
    private String templateName;

    /** 微信小程序通知模版库id */
    @MpField(value = "wxa_template_id", columnType = "string", comment = "微信小程序通知模版库id")
    private String wxaTemplateId;

    /** company_id */
    @MpField(value = "company_id", columnType = "bigint", comment = "company_id")
    private Long companyId;

    /** 通知类型 */
    @MpField(value = "notice_type", columnType = "string", comment = "通知类型", defaultValue = "wxa 小程序")
    private String noticeType = "wxa 小程序";

    /** 模板分类 */
    @MpField(value = "tmpl_type", columnType = "string", comment = "模板分类")
    private String tmplType;

    /** 模板id,发送小程序通知使用 */
    @MpField(value = "template_id", columnType = "string", comment = "模板id,发送小程序通知使用")
    private String templateId;

    /** 标题 */
    @MpField(value = "title", columnType = "string", comment = "标题")
    private String title;

    /** 发送场景 */
    @MpField(value = "scenes_name", columnType = "string", comment = "发送场景")
    private String scenesName;

    /** 模板内容 */
    @MpField(value = "content", columnType = "text", comment = "模板内容")
    private String content;

    /** 是否开启 */
    @MpField(value = "is_open", columnType = "boolean", comment = "是否开启")
    private Boolean isOpen;

    /** 短信发送触发时间描述,配置 */
    @MpField(value = "send_time_desc", columnType = "string", comment = "短信发送触发时间描述,配置")
    private String sendTimeDesc;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
