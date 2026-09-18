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

/** 权益 */
@Data
@MpTable(value = "orders_rights", comment = "权益表")
public class Rights {

    /** 权益ID */
    @MpId(value = "rights_id", type = IdType.AUTO, columnType = "bigint", comment = "权益ID")
    private Long rightsId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 是否可预约 */
    @MpField(value = "can_reservation", columnType = "boolean", comment = "是否可预约", defaultValue = "False")
    private Boolean canReservation;

    /** 权益标题 */
    @MpField(value = "rights_name", columnType = "string", length = 255, comment = "权益标题")
    private String rightsName;

    /** 权益子标题 */
    @MpField(value = "rights_subname", columnType = "string", length = 255, nullable = true, comment = "权益子标题")
    private String rightsSubname;

    /** 权益来源 */
    @MpField(value = "rights_from", columnType = "string", length = 255, nullable = true, comment = "权益来源")
    private String rightsFrom;

    /** 操作员信息 */
    @MpField(value = "operator_desc", columnType = "string", length = 255, nullable = true, comment = "操作员信息")
    private String operatorDesc;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", length = 255, comment = "手机号")
    private String mobile;

    /** 服务商品原始总次数，0 表示无限制 */
    @MpField(value = "total_num", columnType = "bigint", comment = "服务商品原始总次数,0标示无限制", defaultValue = "0")
    private Long totalNum;

    /** 总消耗次数 */
    @MpField(value = "total_consum_num", columnType = "bigint", comment = "总消耗次数", defaultValue = "0")
    private Long totalConsumNum;

    /** 权益开始时间 */
    @MpField(value = "start_time", columnType = "integer", length = 11, comment = "权益开始时间")
    private Integer startTime;

    /** 权益结束时间 */
    @MpField(value = "end_time", columnType = "integer", length = 11, comment = "权益结束时间")
    private Integer endTime;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, nullable = true, comment = "订单号")
    private Long orderId;

    /** 权益的物料信息json结构 */
    @MpField(value = "label_infos", columnType = "text", nullable = true, comment = "权益的物料信息json结构")
    private String labelInfos;

    /** 权益状态：valid 有效的，expire 过期的，invalid 失效的 */
    @MpField(value = "status", columnType = "string", comment = "权益状态; valid:有效的, expire:过期的; invalid:失效的", defaultValue = "valid")
    private String status;

    /** 限制核销次数：1 不限制，2 限制 */
    @MpField(value = "is_not_limit_num", columnType = "integer", comment = "限制核销次数,1:不限制；2:限制", defaultValue = "2")
    private Integer isNotLimitNum;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
