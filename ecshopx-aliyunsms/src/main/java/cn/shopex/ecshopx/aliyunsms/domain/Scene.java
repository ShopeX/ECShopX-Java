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
 * 场景表
 */
@Data
@MpTable(value = "aliyunsms_scene", comment = "场景表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class Scene {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 场景名称 */
    @MpField(value = "scene_name", columnType = "string", comment = "场景名称")
    private String sceneName;

    /** 场景 title，可空 */
    @MpField(value = "scene_title", columnType = "string", nullable = true, comment = "场景title")
    private String sceneTitle;

    /** 短信类型: 0：验证码;1：短信通知;2：推广短信;3：国际/港澳台消息 */
    @MpField(value = "template_type", columnType = "string", comment = "短信类型: 0：验证码;1：短信通知;2：推广短信;3：国际/港澳台消息")
    private String templateType;

    /** 状态：enabled 启用，disabled 禁用 */
    @MpField(value = "status", columnType = "string", comment = "状态")
    private String status = "disabled";

    /** 默认模板 */
    @MpField(value = "default_template", columnType = "string", nullable = true, comment = "默认模板")
    private String defaultTemplate;

    /** 模板变量，varchar(500) */
    @MpField(value = "variables", columnType = "string", length = 500, nullable = true, comment = "模板变量")
    private String variables;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间，可空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
