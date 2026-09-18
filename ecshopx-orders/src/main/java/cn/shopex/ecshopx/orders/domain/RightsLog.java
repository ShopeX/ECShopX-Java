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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 权益核销日志 */
@Data
@MpTable(value = "orders_rights_log", comment = "权益日志表")
public class RightsLog {

    /** 权益日志ID */
    @MpId(value = "rights_log_id", type = IdType.AUTO, columnType = "bigint", comment = "权益日志ID")
    private Long rightsLogId;

    /** 权益ID */
    @MpField(value = "rights_id", columnType = "bigint", comment = "权益ID")
    private Long rightsId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 门店ID */
    @MpField(value = "shop_id", columnType = "string", comment = "门店ID")
    private String shopId;

    /** 权益标题 */
    @MpField(value = "rights_name", columnType = "string", length = 255, comment = "权益标题")
    private String rightsName;

    /** 权益子标题 */
    @MpField(value = "rights_subname", columnType = "string", length = 255, nullable = true, comment = "权益子标题")
    private String rightsSubname;

    /** 消耗次数 */
    @MpField(value = "consum_num", columnType = "bigint", comment = "消耗次数")
    private Long consumNum;

    /** 服务员 */
    @MpField(value = "attendant", columnType = "string", comment = "服务员")
    private String attendant;

    /** 核销员手机号 */
    @MpField(value = "salesperson_mobile", columnType = "string", length = 255, comment = "核销员手机号")
    private String salespersonMobile;

    /** 权益结束时间 */
    @MpField(value = "end_time", columnType = "string", comment = "权益结束时间")
    private String endTime;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
