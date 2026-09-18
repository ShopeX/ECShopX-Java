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

package cn.shopex.ecshopx.kaquan.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员卡等级表 */
@Data
@MpTable(value = "membercard_grade", comment = "会员卡等级表", indexes = {@MpIndex(name = "idx_companyid", columns = {"company_id"})})
public class MemberCardGrade {

    /** 等级ID */
    @MpId(value = "grade_id", type = IdType.AUTO, columnType = "bigint", comment = "等级ID")
    private Long gradeId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "string", comment = "公司ID")
    private String companyId;

    /** 等级名称 */
    @MpField(value = "grade_name", columnType = "string", comment = "等级名称")
    private String gradeName;

    /** 是否默认等级 */
    @MpField(value = "default_grade", columnType = "boolean", comment = "是否默认等级", defaultValue = "False")
    private Boolean defaultGrade = Boolean.FALSE;

    /** 商家自定义会员卡背景图 */
    @MpField(value = "background_pic_url", columnType = "string", length = 1024, nullable = true, comment = "商家自定义会员卡背景图")
    private String backgroundPicUrl;

    /** 等级背景 */
    @MpField(value = "grade_background", columnType = "string", length = 1024, nullable = true, comment = "等级背景")
    private String gradeBackground;

    /** 升级条件（JSON） */
    @MpField(value = "promotion_condition", columnType = "json_array", nullable = true, comment = "升级条件")
    private String promotionCondition;

    /** 会员权益（JSON） */
    @MpField(value = "privileges", columnType = "json_array", nullable = true, comment = "会员权益")
    private String privileges;

    /** 达摩CRM等级编码 */
    @MpField(value = "dm_grade_code", columnType = "string", length = 50, nullable = true, comment = "达摩CRM等级编码")
    private String dmGradeCode = "";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;

    /** 第三方数据 */
    @MpField(value = "third_data", columnType = "string", nullable = true, comment = "第三方数据")
    private String thirdData;

    /** 外部唯一标识，外部调用方自定义的值 */
    @MpField(value = "external_id", columnType = "string", length = 50, comment = "外部唯一标识，外部调用方自定义的值")
    private String externalId = "";

    /** 详细说明 */
    @MpField(value = "description", columnType = "text", nullable = true, comment = "详细说明")
    private String description;
}
