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
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员中心分享信息表 */
@Data
@MpTable(value = "theme_member_center_share", comment = "会员中心分享信息表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class ThemeMemberCenterShare {

    @MpId(value = "theme_member_center_share_id", type = IdType.AUTO, columnType = "bigint")
    private Long themeMemberCenterShareId;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 页面名称 */
    @MpField(value = "share_title", columnType = "string", length = 50, comment = "页面名称")
    private String shareTitle;

    /** 页面描述 */
    @MpField(value = "share_description", columnType = "string", length = 150, comment = "页面描述")
    private String shareDescription;

    /** 分享图片小程序，默认空字符串 */
    @MpField(value = "share_pic_wechatapp", columnType = "string", length = 150, nullable = true, comment = "分享图片小程序")
    private String sharePicWechatapp = "";

    /** 分享图片h5，默认空字符串 */
    @MpField(value = "share_pic_h5", columnType = "string", length = 150, nullable = true, comment = "分享图片h5")
    private String sharePicH5 = "";

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
