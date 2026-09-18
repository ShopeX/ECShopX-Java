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

/** 卡券包领取记录表 */
@Data
@MpTable(value = "card_package_receive", comment = "卡券包领取记录表", indexes = {@MpIndex(name = "idx_companyid", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_front_show", columns = {"front_show"})})
public class CardPackageReceive {

    /** 主键ID */
    @MpId(value = "receive_id", type = IdType.AUTO, columnType = "bigint", comment = "主键ID")
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

    /** 领取类型，vip_grade/grade/template vip等级/等级/卡券模版 */
    @MpField(value = "receive_type", columnType = "string", length = 20, comment = "领取类型，vip_grade/grade/template vip等级/等级/卡券模版")
    private String receiveType;

    /** 1/2/3 领取中/领取成功/领取失败 */
    @MpField(value = "receive_status", columnType = "integer", comment = "1/2/3 领取中/领取成功/领取失败")
    private Integer receiveStatus;

    /** 前端弹框是否展示过 */
    @MpField(value = "front_show", columnType = "integer", comment = "前端弹框是否展示过")
    private Integer frontShow;

    /** 领取时间 */
    @MpField(value = "receive_time", columnType = "integer", comment = "领取时间")
    private Integer receiveTime;

    /** 成功领取卡券数量 */
    @MpField(value = "success_count", columnType = "integer", comment = "成功领取卡券数量")
    private Integer successCount;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
