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

package cn.shopex.ecshopx.members.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 订阅通知 */
@Data
@MpTable(value = "members_subscribe_notice", comment = "订阅通知", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"})})
public class SubscribeNotice {

    /** 订阅id */
    @MpId(value = "sub_id", type = IdType.AUTO, columnType = "bigint", comment = "订阅id")
    private Long subId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 会员id */
    @MpField(value = "user_id", columnType = "bigint", comment = "会员id")
    private Long userId;

    /** open_id */
    @MpField(value = "open_id", columnType = "string", length = 40, comment = "open_id")
    private String openId;

    /** 订阅来源 wechat:微信 alipay:支付宝 */
    @MpField(value = "source", columnType = "string", length = 10, comment = "订阅来源 wechat:微信 alipay:支付宝", defaultValue = "wechat")
    private String source = "wechat";

    /** 关联id */
    @MpField(value = "rel_id", columnType = "bigint", nullable = true, comment = "关联id")
    private Long relId;

    /** 订阅类型。可选值有 goods:商品缺货通知 */
    @MpField(value = "sub_type", columnType = "string", comment = "订阅类型。可选值有 goods:商品缺货通知", defaultValue = "goods")
    private String subType = "goods";

    /** 订阅备注 */
    @MpField(value = "remarks", columnType = "string", nullable = true, comment = "订阅备注")
    private String remarks;

    /** 通知状态。可选值有 NO—未通知;SUCCESS-已通知;ERROR-通知失败 */
    @MpField(value = "sub_status", columnType = "string", comment = "通知状态。可选值有 NO—未通知;SUCCESS-已通知;ERROR-通知失败", defaultValue = "NO")
    private String subStatus = "NO";

    /** 通知失败原因 */
    @MpField(value = "err_reason", columnType = "string", nullable = true, comment = "通知失败原因")
    private String errReason;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺id", defaultValue = "0")
    private Integer distributorId = 0;
}
