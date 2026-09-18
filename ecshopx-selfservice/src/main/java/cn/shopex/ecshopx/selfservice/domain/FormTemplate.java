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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 自助表单模板 */
@Data
@MpTable(value = "selfservice_form_template", comment = "自助表单模板", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class FormTemplate {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 表单模板名称 */
    @MpField(value = "tem_name", columnType = "string", comment = "表单模板名称")
    private String temName;

    /** 表单模板类型；ask_answer_paper：问答考卷，basic_entry：基础录入 */
    @MpField(value = "tem_type", columnType = "string", comment = "表单模板类型；ask_answer_paper：问答考卷，basic_entry：基础录入")
    private String temType;

    /** 表单模板内容 */
    @MpField(value = "content", columnType = "text", comment = "表单模板内容")
    private String content;

    /** 状态; 1:有效，2:弃用 */
    @MpField(value = "status", columnType = "integer", comment = "状态; 1:有效，2:弃用", defaultValue = "1")
    private Integer status = 1;

    /** 表单关键指数 */
    @MpField(value = "key_index", columnType = "text", nullable = true, comment = "表单关键指数")
    private String keyIndex;

    /** 表单关键指数, single:单页问卷, multiple:多页问卷 */
    @MpField(value = "form_style", columnType = "string", nullable = true, comment = "表单关键指数, single:单页问卷, multiple:多页问卷")
    private String formStyle;

    /** 头部文字 */
    @MpField(value = "header_link_title", columnType = "string", length = 500, nullable = true, comment = "头部文字")
    private String headerLinkTitle;

    /** 头部背景图片 */
    @MpField(value = "header_bg_pic", columnType = "string", length = 300, nullable = true, comment = "头部背景图片")
    private String headerBgPic;

    /** 头部留白高度(px) */
    @MpField(value = "header_height", columnType = "string", length = 20, nullable = true, comment = "头部留白高度(px)")
    private String headerHeight;

    /** 头部文字内容 */
    @MpField(value = "header_title", columnType = "string", length = 500, nullable = true, comment = "头部文字内容")
    private String headerTitle;

    /** 表单关键指数 */
    @MpField(value = "bottom_title", columnType = "string", length = 500, nullable = true, comment = "表单关键指数")
    private String bottomTitle;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
