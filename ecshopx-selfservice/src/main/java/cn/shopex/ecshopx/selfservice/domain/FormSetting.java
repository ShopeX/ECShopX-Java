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

package cn.shopex.ecshopx.selfservice.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 自助表单配置 */
@Data
@MpTable(value = "selfservice_form_setting", comment = "自助表单配置")
public class FormSetting {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 表单项标题(中文描述) */
    @MpField(value = "field_title", columnType = "string", comment = "表单项标题(中文描述)")
    private String fieldTitle;

    /** 图片描述 */
    @MpField(value = "pic_name", columnType = "string", length = 50, nullable = true, comment = "图片描述")
    private String picName;

    /** 表单项英文名称(英文或拼音描述),唯一标示 */
    @MpField(value = "field_name", columnType = "string", comment = "表单项英文名称(英文或拼音描述),唯一标示")
    private String fieldName;

    /** 表单元素,text:文本,textarea:文本域,select:选择框,radio:单选,checkbox:多选框,date:日期选择,time:时间选择,area:地区地址选择, image:图片上传,number:纯数字 */
    @MpField(value = "form_element", columnType = "string", nullable = true, comment = "表单元素,text:文本,textarea:文本域,select:选择框,radio:单选,checkbox:多选框,date:日期选择,time:时间选择,area:地区地址选择, image:图片上传,number:纯数字")
    private String formElement = "text";

    /** 元素配图 */
    @MpField(value = "image_url", columnType = "string", nullable = true, comment = "元素配图")
    private String imageUrl = "";

    /** 状态;1:有效，2:弃用 */
    @MpField(value = "status", columnType = "integer", comment = "状态;1:有效，2:弃用", defaultValue = "1")
    private Integer status = 1;

    /** 排序，数字越大越靠前 */
    @MpField(value = "sort", columnType = "integer", comment = "排序，数字越大越靠前", defaultValue = "1")
    private Integer sort = 1;

    /** 是否必填 */
    @MpField(value = "is_required", columnType = "boolean", comment = "是否必填", defaultValue = "False")
    private Boolean isRequired = false;

    /** 表单元素为选择类时选择项（json）当form_element in (select,  radio, checkbox)时，此项必填。select 选择框 radio 单选框 checkbox 多项框 */
    @MpField(value = "options", columnType = "text", nullable = true, comment = "表单元素为选择类时选择项（json）当form_element in (select,  radio, checkbox)时，此项必填")
    private String options;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
