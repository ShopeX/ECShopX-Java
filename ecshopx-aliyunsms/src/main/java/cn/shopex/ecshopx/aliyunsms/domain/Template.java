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

package cn.shopex.ecshopx.aliyunsms.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 模板表
 */
@Data
@MpTable(value = "aliyunsms_template", comment = "模板表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class Template {

    /** 模板ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "模板ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 短信类型: 0：验证码;1：短信通知;2：推广短信;3：国际/港澳台消息 */
    @MpField(value = "template_type", columnType = "string", comment = "短信类型: 0：验证码;1：短信通知;2：推广短信;3：国际/港澳台消息")
    private String templateType;

    /** 模板名称 */
    @MpField(value = "template_name", columnType = "string", comment = "模板名称")
    private String templateName;

    /** 模板申请说明 */
    @MpField(value = "remark", columnType = "string", comment = "模板申请说明")
    private String remark;

    /** 模板内容 */
    @MpField(value = "template_content", columnType = "text", comment = "模板内容")
    private String templateContent;

    /** 短信场景 */
    @MpField(value = "scene_id", columnType = "integer", comment = "短信场景")
    private Integer sceneId;

    /** 模板编码 */
    @MpField(value = "template_code", columnType = "string", nullable = true, comment = "模板编码")
    private String templateCode;

    /** 审核状态:0-审核中;1-审核通过;2-审核失败 */
    @MpField(value = "`status`", columnType = "string", comment = "审核状态:0-审核中;1-审核通过;2-审核失败")
    private String status = "0";

    /** 审核备注 */
    @MpField(value = "reason", columnType = "string", nullable = true, comment = "审核备注")
    private String reason = "";

    /** 关联签名名称，varchar(20) */
    @MpField(value = "related_sign_name", columnType = "string", length = 20, comment = "关联签名名称")
    private String relatedSignName;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间，可空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
