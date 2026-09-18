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

/** 权益操作日志 */
@Data
@MpTable(value = "orders_rights_operate_logs", comment = "权益操作日志表")
public class RightsOperateLogs {

    /** 权益延期操作日志主键 */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "权益延期操作日志")
    private Long id;

    /** 权益ID */
    @MpField(value = "rights_id", columnType = "bigint", comment = "权益ID")
    private Long rightsId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 操作备注 */
    @MpField(value = "remark", columnType = "string", length = 255, comment = "操作备注")
    private String remark;

    /** 操作员Id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "操作员Id")
    private Long operatorId;

    /** 操作员 */
    @MpField(value = "operator", columnType = "string", nullable = true, comment = "操作员")
    private String operator;

    /** 延期之前日期 */
    @MpField(value = "original_date", columnType = "integer", nullable = true, comment = "延期之前日期")
    private Integer originalDate;

    /** 延期之后日期 */
    @MpField(value = "delay_date", columnType = "integer", nullable = true, comment = "延期之后日期")
    private Integer delayDate;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
