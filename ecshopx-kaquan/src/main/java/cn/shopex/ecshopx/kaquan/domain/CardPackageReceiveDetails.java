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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 卡券包领取记录详情表 */
@Data
@MpTable(value = "card_package_receive_details", comment = "卡券包领取记录详情表", indexes = {@MpIndex(name = "idx_companyid", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_receive_id", columns = {"receive_id"})})
public class CardPackageReceiveDetails {

    /** 主键ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "主键ID")
    private Long id;

    /** 用户领取表主键ID */
    @MpField(value = "receive_id", columnType = "bigint", comment = "用户领取表主键ID")
    private Long receiveId;

    /** 卡券包ID */
    @MpField(value = "package_id", columnType = "bigint", comment = "卡券包ID")
    private Long packageId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 卡券ID */
    @MpField(value = "card_id", columnType = "bigint", comment = "卡券ID")
    private Long cardId;

    /** 领取描述 */
    @MpField(value = "message", columnType = "string", length = 100, comment = "领取描述")
    private String message;

    /** 1/2/3 领取中/领取成功/领取失败 */
    @MpField(value = "receive_status", columnType = "integer", comment = "1/2/3 领取中/领取成功/领取失败")
    private Integer receiveStatus;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
