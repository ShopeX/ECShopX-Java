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

package cn.shopex.ecshopx.espier.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 配置信息, 配置传递的请求字段
 *
 * <p>模块类型：1 会员注册；2 团长申请（与列注释一致：【1: 会员注册】【2: 团长申请】）
 */
@Data
@MpTable(value = "config_request_fields", comment = "配置信息, 配置传递的请求字段", indexes = {@MpIndex(name = "ix_company_module_open", columns = {"company_id", "module_type", "is_open"})})
public class ConfigRequestFields {

    /** 主键id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "integer", comment = "主键id")
    private Integer id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "integer", comment = "公司id")
    private Integer companyId;

    /** 店铺id,为0时表示该配置为平台创建 */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺id,为0时表示该配置为平台创建", defaultValue = "0")
    private Integer distributorId = 0;

    /** 模块类型, 【1: 会员注册】【2: 团长申请】 */
    @MpField(value = "module_type", columnType = "smallint", comment = "模块类型, 【1: 会员注册】【2: 团长申请】")
    private Integer moduleType;

    /** 该请求字段的标识, 比如是mobile则表示为手机号 */
    @MpField(value = "label", columnType = "string", length = 50, comment = "该请求字段的标识, 比如是mobile则表示为手机号")
    private String label;

    /** 前后端交互时需要被传递的key名，如果是手机号则是mobile */
    @MpField(value = "key_name", columnType = "string", length = 50, comment = "前后端交互时需要被传递的key名，如果是手机号则是mobile")
    private String keyName;

    /** 是否启用, 【0 关闭】【1 开启】 */
    @MpField(value = "is_open", columnType = "boolean", comment = "是否启用, 【0 关闭】【1 开启】")
    private Boolean isOpen;

    /** 是否必填, 【0 非必填】 【1 必填】 */
    @MpField(value = "is_required", columnType = "boolean", comment = "是否必填, 【0 非必填】 【1 必填】")
    private Boolean isRequired;

    /**
     * 是否是预设字段, 【0 非必填】 【1 必填】
     */
    @MpField(value = "is_preset", columnType = "boolean", comment = "是否是预设字段, 【0 非必填】 【1 必填】")
    private Boolean isPreset;

    /** 是否可修改, 【0 不可修改】 【1 可修改】 */
    @MpField(value = "is_edit", columnType = "boolean", comment = "是否可修改, 【0 不可修改】 【1 可修改】")
    private Boolean isEdit;

    /** 当前请求字段的类型, [1 文本] [2 数字] [3 日期] [4 单选项] */
    @MpField(value = "field_type", columnType = "smallint", comment = "当前请求字段的类型, [1 文本] [2 数字] [3 日期] [4 单选项]")
    private Integer fieldType;

    /** 验证条件, json存储 */
    @MpField(value = "validate_condition", columnType = "text", comment = "验证条件, json存储")
    private String validateCondition;

    /** 提示必填时的文案 */
    @MpField(value = "alert_required_message", columnType = "string", comment = "提示必填时的文案")
    private String alertRequiredMessage;

    /** 提示验证时时的文案 */
    @MpField(value = "alert_validate_message", columnType = "string", comment = "提示验证时时的文案")
    private String alertValidateMessage;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
