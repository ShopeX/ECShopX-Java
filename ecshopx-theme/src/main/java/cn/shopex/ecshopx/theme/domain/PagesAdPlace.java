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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 广告位设置 */
@Data
@MpTable(value = "pages_ad_place", comment = "广告位设置")
public class PagesAdPlace {

    /** 设置id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "设置id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 区域id，默认 0 */
    @MpField(value = "regionauth_id", columnType = "bigint", comment = "区域id", defaultValue = "0")
    private Long regionauthId = 0L;

    /** 适用范围: 0:全部,1:指定店铺，默认 0 */
    @MpField(value = "use_bound", columnType = "integer", nullable = true, comment = "适用范围: 0:全部,1:指定店铺", defaultValue = "0")
    private Integer useBound = 0;

    /** 广告类型：弹窗=>popup，轮播图=>carousel */
    @MpField(value = "ad_type", columnType = "string", length = 20)
    private String adType;

    /** 广告位名称 */
    @MpField(value = "name", columnType = "string", length = 100, comment = "广告位名称")
    private String name;

    /** 关联页面 */
    @MpField(value = "pages", columnType = "string", length = 30, comment = "关联页面")
    private String pages;

    /** 开始时间 */
    @MpField(value = "start_time", columnType = "bigint", comment = "开始时间")
    private Long startTime;

    /** 结束时间 */
    @MpField(value = "end_time", columnType = "bigint", comment = "结束时间")
    private Long endTime;

    /** 设置 */
    @MpField(value = "setting", columnType = "text", nullable = true, comment = "设置")
    private String setting;

    /** 自动播放，默认 0 */
    @MpField(value = "auto_play", columnType = "integer", comment = "自动播放", defaultValue = "0")
    private Integer autoPlay = 0;

    /** 播放间隔时间，默认 3 */
    @MpField(value = "play_interval", columnType = "integer", comment = "播放间隔时间", defaultValue = "3")
    private Integer playInterval = 3;

    /** 自动关闭，默认 0 */
    @MpField(value = "auto_close", columnType = "integer", comment = "自动关闭", defaultValue = "0")
    private Integer autoClose = 0;

    /** 关闭延迟时间，默认 10 */
    @MpField(value = "close_delay", columnType = "integer", comment = "关闭延迟时间", defaultValue = "10")
    private Integer closeDelay = 10;

    /** 添加者ID: 如店铺ID，默认 0 */
    @MpField(value = "source_id", columnType = "bigint", comment = "添加者ID: 如店铺ID", defaultValue = "0")
    private Long sourceId = 0L;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /**
     * 审核状态 submitting待提交 processing审核中 approved成功 rejected审核拒绝，默认 submitting
     */
    @MpField(value = "audit_status", columnType = "string", comment = "审核状态 submitting待提交 processing审核中 approved成功 rejected审核拒绝", defaultValue = "submitting")
    private String auditStatus = "submitting";

    /** 审核备注 */
    @MpField(value = "audit_remark", columnType = "string", nullable = true, comment = "审核备注")
    private String auditRemark;

    /** 排序，默认 0 */
    @MpField(value = "sort", columnType = "integer", comment = "排序", defaultValue = "0")
    private Integer sort = 0;

    /** 埋点上报参数 */
    @MpField(value = "tracking_code", columnType = "string", length = 100, nullable = true, comment = "埋点上报参数")
    private String trackingCode;
}
