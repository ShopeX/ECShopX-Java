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
 * 场景实例表
 */
@Data
@MpTable(value = "aliyunsms_scene_item", comment = "场景实例表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class SceneItem {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 场景ID */
    @MpField(value = "scene_id", columnType = "integer", comment = "场景ID")
    private Integer sceneId;

    /** 签名ID */
    @MpField(value = "sign_id", columnType = "integer", comment = "签名ID")
    private Integer signId;

    /** 签名名称 */
    @MpField(value = "sign_name", columnType = "string", comment = "签名名称")
    private String signName;

    /** 模板ID */
    @MpField(value = "template_id", columnType = "integer", comment = "模板ID")
    private Integer templateId;

    /** 模板内容 */
    @MpField(value = "template_content", columnType = "text", comment = "模板内容")
    private String templateContent;

    /** 0-未启用;1-已启用 */
    @MpField(value = "status", columnType = "integer", comment = "0-未启用;1-已启用")
    private Integer status = 0;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间，可空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
