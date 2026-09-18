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

package cn.shopex.ecshopx.theme.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/** 模板设置表 */
@Data
@MpTable(value = "pages_template_set", comment = "模板设置表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class PagesTemplateSet {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 区域id，默认 0 */
    @MpField(value = "regionauth_id", columnType = "bigint", nullable = true, comment = "区域id", defaultValue = "0")
    private Long regionauthId = 0L;

    /** 首页类型 1总部首页 2店铺首页，默认 1 */
    @MpField(value = "index_type", columnType = "integer", nullable = true, comment = "首页类型 1总部首页 2店铺首页", defaultValue = "1")
    private Integer indexType = 1;

    /** 关联模版ID，默认 0 */
    @MpField(value = "pages_template_id", columnType = "bigint", nullable = true, comment = "关联模版ID", defaultValue = "0")
    private Long pagesTemplateId = 0L;

    /** 店铺首页同步状态 1强制同步 2非强制同步，默认 2 */
    @MpField(value = "is_enforce_sync", columnType = "integer", nullable = true, comment = "店铺首页同步状态 1强制同步 2非强制同步", defaultValue = "2")
    private Integer isEnforceSync = 2;

    /** 开启猜你喜欢 1开启 2关闭，默认 2 */
    @MpField(value = "is_open_recommend", columnType = "integer", nullable = true, comment = "开启猜你喜欢 1开启 2关闭", defaultValue = "2")
    private Integer isOpenRecommend = 2;

    /** 开启小程序定位 1开启 2关闭，默认 1 */
    @MpField(value = "is_open_wechatapp_location", columnType = "integer", nullable = true, comment = "开启小程序定位 1开启 2关闭", defaultValue = "1")
    private Integer isOpenWechatappLocation = 1;

    /** 开启扫码功能 1开启 2关闭，默认 2 */
    @MpField(value = "is_open_scan_qrcode", columnType = "integer", nullable = true, comment = "开启扫码功能 1开启 2关闭", defaultValue = "2")
    private Integer isOpenScanQrcode = 2;

    /** 开启关注公众号组件 1开启 2关闭，默认 2 */
    @MpField(value = "is_open_official_account", columnType = "integer", nullable = true, comment = "开启关注公众号组件 1开启 2关闭", defaultValue = "2")
    private Integer isOpenOfficialAccount = 2;

    /** 小程序菜单设置 */
    @MpField(value = "tab_bar", columnType = "text", nullable = true, comment = "小程序菜单设置")
    private String tabBar;

    @MpField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @MpField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
