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

/** 开屏广告设置 */
@Data
@MpTable(value = "pages_open_screen_ad", comment = "开屏广告设置", indexes = {@MpIndex(name = "ix_id", columns = {"id"}), @MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class OpenScreenAd {

    /** 设置id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "设置id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 广告素材 */
    @MpField(value = "ad_material", columnType = "string", comment = "广告素材")
    private String adMaterial;

    /** 是否启用0否,1是，默认 0 */
    @MpField(value = "is_enable", columnType = "integer", comment = "是否启用0否,1是", defaultValue = "0")
    private Integer isEnable = 0;

    /** 曝光设置：first,always，默认 first */
    @MpField(value = "show_time", columnType = "string", comment = "曝光设置：first,always", defaultValue = "first")
    private String showTime = "first";

    /** 倒计时位置：right_top,right_bottom，默认 0 */
    @MpField(value = "position", columnType = "string", comment = "倒计时位置：right_top,right_bottom", defaultValue = "0")
    private String position = "0";

    /** 是否跳过0否,1是，默认 0 */
    @MpField(value = "is_jump", columnType = "integer", comment = "是否跳过0否,1是", defaultValue = "0")
    private Integer isJump = 0;

    /** 素材类型1图片,2视频，默认 1 */
    @MpField(value = "material_type", columnType = "integer", comment = "素材类型1图片,2视频", defaultValue = "1")
    private Integer materialType = 1;

    /** 等待时间，秒，默认 0 */
    @MpField(value = "waiting_time", columnType = "integer", comment = "等待时间，秒", defaultValue = "0")
    private Integer waitingTime = 0;

    /** 广告链接 */
    @MpField(value = "ad_url", columnType = "string", length = 1000, comment = "广告链接")
    private String adUrl;

    /** 设置应用, all 全部,app APP,wapp 小程序 ，默认 0 */
    @MpField(value = "app", columnType = "string", length = 100, comment = "设置应用, all 全部,app APP,wapp 小程序 ", defaultValue = "0")
    private String app = "0";

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 开始时间 */
    @MpField(value = "start_time", columnType = "bigint", comment = "开始时间")
    private Long startTime;

    /** 结束时间 */
    @MpField(value = "end_time", columnType = "bigint", comment = "结束时间")
    private Long endTime;
}
