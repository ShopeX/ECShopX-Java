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

package cn.shopex.ecshopx.salesperson.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 导购通知 */
@Data
@MpTable(value = "salesperson_notice", comment = "导购通知", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_title", columns = {"title"})})
public class SalespersonNotice {

    /** 通知id */
    @MpId(value = "notice_id", type = IdType.AUTO, columnType = "bigint", comment = "通知id")
    private Long noticeId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 通知标题 */
    @MpField(value = "title", columnType = "string", comment = "通知标题")
    private String title;

    /** 通知内容 */
    @MpField(value = "content", columnType = "text", comment = "通知内容")
    private String content;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "text", nullable = true, comment = "店铺id")
    private String distributorId = "";

    /** 店铺id */
    @MpField(value = "all_distributor", columnType = "text", nullable = true, comment = "店铺id", defaultValue = "0")
    private String allDistributor = "0";

    /** 通知类型，1系统通知，2总部通知，3其他通知 */
    @MpField(value = "notice_type", columnType = "integer", comment = "通知类型，1系统通知，2总部通知，3其他通知", defaultValue = "1")
    private Integer noticeType = 1;

    /** 发送次数 */
    @MpField(value = "sent_times", columnType = "integer", nullable = true, comment = "发送次数", defaultValue = "0")
    private Integer sentTimes = 0;

    /** 是否已删除 */
    @MpField(value = "is_delete", columnType = "integer", nullable = true, comment = "是否已删除", defaultValue = "0")
    private Integer isDelete = 0;

    /** 是否撤回 */
    @MpField(value = "withdraw", columnType = "integer", nullable = true, comment = "是否撤回", defaultValue = "0")
    private Integer withdraw = 0;

    /** 最后发送时间 */
    @MpField(value = "last_sent_time", columnType = "integer", nullable = true, comment = "最后发送时间", defaultValue = "0")
    private Integer lastSentTime = 0;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;

    /** 状态，1未发送，2已发送，3已撤回 */
    @MpField(value = "status", columnType = "integer", nullable = true, comment = "状态，1未发送，2已发送，3已撤回", defaultValue = "1")
    private Integer status = 1;
}
