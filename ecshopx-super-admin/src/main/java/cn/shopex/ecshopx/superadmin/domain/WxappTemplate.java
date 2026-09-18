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

package cn.shopex.ecshopx.superadmin.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 小程序模板表 */
@Data
@MpTable(value = "superadmin_wxapp_template", comment = "小程序模板表")
public class WxappTemplate {

    /** 主键 */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 小程序英文描述（唯一） */
    @MpField(value = "key_name", columnType = "string", comment = "小程序英文描述", unique = true)
    private String keyName;

    /** 小程序模板名称，可为空 */
    @MpField(value = "name", columnType = "string", nullable = true, comment = "小程序模板名称")
    private String name;

    /** 小程序标签，可为空 */
    @MpField(value = "tag", columnType = "string", nullable = true, comment = "小程序标签")
    private String tag;

    /** 模板id，默认 0，可为空 */
    @MpField(value = "template_id", columnType = "integer", nullable = true, comment = "模板id")
    private Integer templateId = 0;

    /** 模板id(直播版)，默认 0，可为空 */
    @MpField(value = "template_id_2", columnType = "integer", nullable = true, comment = "模板id(直播版)")
    private Integer templateId2 = 0;

    /** 版本号，可为空 */
    @MpField(value = "version", columnType = "string", nullable = true, comment = "版本号")
    private String version;

    /**
     * 是否为唯一属性，如果为唯一属性那么当前模版只能绑定一个小程序，默认 false
     */
    @MpField(value = "is_only", columnType = "boolean", comment = "是否为唯一属性，如果为唯一属性那么当前模版只能绑定一个小程序", defaultValue = "False")
    private Boolean isOnly = false;

    /** 模板详细描述，可为空 */
    @MpField(value = "description", columnType = "string", nullable = true, comment = "模板详细描述")
    private String description;

    /** 合法域名配置，可为空 */
    @MpField(value = "domain", columnType = "text", nullable = true, comment = "合法域名配置")
    private String domain;

    /** 是否禁用，默认 false */
    @MpField(value = "is_disabled", columnType = "boolean", comment = "是否禁用", defaultValue = "False")
    private Boolean isDisabled = false;

    /**
     * 使用平台。可选值有 development-开发环境;preissue-预发布环境;production-正式环境;，默认 development
     */
    @MpField(value = "platform", columnType = "string", comment = "使用平台。可选值有 development-开发环境;preissue-预发布环境;production-正式环境;", defaultValue = "development")
    private String platform = "development";

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
