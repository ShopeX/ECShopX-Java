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

package cn.shopex.ecshopx.workwechat.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 企业微信通知 */
@Data
@MpTable(value = "work_wechat_message", comment = "企业微信通知", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "idx_operator_id", columns = {"operator_id"})})
public class WorkWechatMessage {

    /** 主键 */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "主键")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id")
    private Long distributorId;

    /** 接受消息用户id */
    @MpField(value = "operator_id", columnType = "bigint", comment = "接受消息用户id")
    private Long operatorId;

    /** 消息类型:1售后订单，2待发货订单，3未妥投订单 */
    @MpField(value = "msg_type", columnType = "smallint", nullable = true, comment = "消息类型:1售后订单，2待发货订单，3未妥投订单")
    private Integer msgType;

    /** 内容 */
    @MpField(value = "content", columnType = "text", nullable = true, comment = "内容")
    private String content;

    /** 添加时间 */
    @MpField(value = "add_time", columnType = "integer", nullable = true, comment = "添加时间")
    private Integer addTime;

    /** 修改时间 */
    @MpField(value = "up_time", columnType = "integer", nullable = true, comment = "修改时间")
    private Integer upTime;

    /** 是否已读:0,未读;1,已读 */
    @MpField(value = "is_read", columnType = "smallint", nullable = true, comment = "是否已读:0,未读;1,已读", defaultValue = "0")
    private Integer isRead = 0;
}
