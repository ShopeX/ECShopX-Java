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

package cn.shopex.ecshopx.wechat.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 小程序自定义页面 */
@Data
@MpTable(value = "wechat_weapp_customize_page", comment = "小程序自定义页面")
public class WeappCustomizePage {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 小程序模板名称 */
    @MpField(value = "template_name", columnType = "string", nullable = true, comment = "小程序模板名称")
    private String templateName;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 区域id */
    @MpField(value = "regionauth_id", columnType = "bigint", comment = "区域id", defaultValue = "0")
    private Long regionauthId = 0L;

    /** 页面名称 */
    @MpField(value = "page_name", columnType = "string", comment = "页面名称")
    private String pageName;

    /** 页面描述 */
    @MpField(value = "page_description", columnType = "string", comment = "页面描述")
    private String pageDescription;

    /** 分享标题 */
    @MpField(value = "page_share_title", columnType = "string", nullable = true, comment = "分享标题")
    private String pageShareTitle;

    /** 分享描述 */
    @MpField(value = "page_share_desc", columnType = "string", nullable = true, comment = "分享描述")
    private String pageShareDesc;

    /** 分享图片 */
    @MpField(value = "page_share_imageUrl", columnType = "string", nullable = true, comment = "分享图片")
    private String pageShareImageUrl;

    /** 是否开启0否1是 */
    @MpField(value = "is_open", columnType = "integer", comment = "是否开启0否1是", defaultValue = "0")
    private Integer isOpen = 0;

    /** 页面类型 normal:普通页面 salesperson:导购首页 category:分类页 my:我的 */
    @MpField(value = "page_type", columnType = "string", comment = "页面类型 normal:普通页面 salesperson:导购首页 category:分类页 my:我的", defaultValue = "normal")
    private String pageType = "normal";
}
