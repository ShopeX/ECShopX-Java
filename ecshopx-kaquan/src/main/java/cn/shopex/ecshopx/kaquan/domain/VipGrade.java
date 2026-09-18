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

/** 付费会员等级库表 */
@Data
@MpTable(value = "kaquan_vip_grade", comment = "付费会员等级库表", indexes = {@MpIndex(name = "idx_companyid_created", columns = {"company_id", "created"})})
public class VipGrade {

    /** ID */
    @MpId(value = "vip_grade_id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long vipGradeId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "integer", comment = "公司ID")
    private Integer companyId;

    /** 等级名称 */
    @MpField(value = "grade_name", columnType = "string", comment = "等级名称")
    private String gradeName;

    /** 等级类型,可选值有 vip:普通vip;svip:进阶vip */
    @MpField(value = "lv_type", columnType = "string", comment = "等级类型,可选值有 vip:普通vip;svip:进阶vip")
    private String lvType = "vip";

    /** 购买引导文本 */
    @MpField(value = "guide_title", columnType = "string", nullable = true, comment = "购买引导文本")
    private String guideTitle;

    /** 购买引导文本 */
    @MpField(value = "is_default", columnType = "boolean", comment = "购买引导文本", defaultValue = "False")
    private Boolean isDefault = Boolean.FALSE;

    /** 是否默认等级 */
    @MpField(value = "default_grade", columnType = "boolean", nullable = true, comment = "是否默认等级", defaultValue = "False")
    private Boolean defaultGrade = Boolean.FALSE;

    /** 是否禁用 */
    @MpField(value = "is_disabled", columnType = "boolean", nullable = true, comment = "是否禁用", defaultValue = "False")
    private Boolean isDisabled = Boolean.FALSE;

    /** 商家自定义会员卡背景图 */
    @MpField(value = "background_pic_url", columnType = "string", length = 1024, nullable = true, comment = "商家自定义会员卡背景图")
    private String backgroundPicUrl;

    /** 阶段价格表 */
    @MpField(value = "price_list", columnType = "text", nullable = true, comment = "阶段价格表")
    private String priceList;

    /** 会员权益 */
    @MpField(value = "privileges", columnType = "text", nullable = true, comment = "会员权益")
    private String privileges;

    /** 详细说明 */
    @MpField(value = "description", columnType = "text", nullable = true, comment = "详细说明")
    private String description;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;

    /** 外部唯一标识，外部调用方自定义的值 */
    @MpField(value = "external_id", columnType = "string", length = 50, comment = "外部唯一标识，外部调用方自定义的值")
    private String externalId = "";
}
