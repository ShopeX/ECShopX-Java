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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员卡 */
@Data
@MpTable(value = "membercard", comment = "会员卡")
public class MemberCard {

    /** 公司id */
    @MpId(value = "company_id", type = IdType.INPUT, columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 商户名称 */
    @MpField(value = "brand_name", columnType = "string", length = 36, comment = "商户名称")
    private String brandName;

    /** 商户 logo */
    @MpField(value = "logo_url", columnType = "string", comment = "商户 logo")
    private String logoUrl;

    /** 卡券名,最大9个汉字 */
    @MpField(value = "title", columnType = "string", length = 27, comment = "卡券名,最大9个汉字")
    private String title;

    /** 券颜色值 */
    @MpField(value = "color", columnType = "string", length = 16, comment = "券颜色值")
    private String color;

    /** 卡券码类型(CODE_TYPE_TEXT CODE_TYPE_BARCODE CODE_TYPE_QRCODE CODE_TYPE_ONLY_QRCODE CODE_TYPE_ONLY_BARCODE CODE_TYPE_NONE) */
    @MpField(value = "code_type", columnType = "string", length = 48, comment = "卡券码类型(CODE_TYPE_TEXT CODE_TYPE_BARCODE CODE_TYPE_QRCODE CODE_TYPE_ONLY_QRCODE CODE_TYPE_ONLY_BARCODE CODE_TYPE_NONE)")
    private String codeType;

    /** 会员卡背景图 */
    @MpField(value = "background_pic_url", columnType = "string", comment = "会员卡背景图")
    private String backgroundPicUrl;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
