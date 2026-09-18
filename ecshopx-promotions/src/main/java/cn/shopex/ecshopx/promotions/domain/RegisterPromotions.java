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

/** 注册营销表 */
@Data
@MpTable(value = "register_promotions", comment = "注册营销表")
public class RegisterPromotions {

    /** 注册促销活动ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "注册促销活动ID")
    private Long id;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 是否开启 */
    @MpField(value = "is_open", columnType = "string", comment = "是否开启")
    private String isOpen;

    /** 促销类型。可选值有 general-普通;distributor-分销商 */
    @MpField(value = "register_type", columnType = "string", comment = "促销类型。可选值有 general-普通;distributor-分销商", defaultValue = "general")
    private String registerType = "general";

    /** 注册引导广告标题 */
    @MpField(value = "ad_title", columnType = "string", nullable = true, comment = "注册引导广告标题")
    private String adTitle;

    /** 注册引导图片 */
    @MpField(value = "ad_pic", columnType = "string", comment = "注册引导图片")
    private String adPic;

    /** 注册引导跳转路径 */
    @MpField(value = "register_jump_path", columnType = "string", length = 500, nullable = true, comment = "注册引导跳转路径")
    private String registerJumpPath;

    /** 赠送的参数 */
    @MpField(value = "promotions_value", columnType = "text", comment = "赠送的参数")
    private String promotionsValue;
}
