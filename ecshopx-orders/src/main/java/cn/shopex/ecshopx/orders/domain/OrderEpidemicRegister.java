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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 订单疫情登记 */
@Data
@MpTable(value = "order_epidemic_register", comment = "订单疫情登记表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"})})
public class OrderEpidemicRegister {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 订单id */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单id")
    private Long orderId;

    /** 用户ID */
    @MpField(value = "user_id", columnType = "bigint", length = 64, comment = "用户ID")
    private Long userId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "integer", comment = "公司id")
    private Integer companyId;

    /** 店铺ID */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺ID")
    private Long distributorId;

    /** 登记姓名 */
    @MpField(value = "name", columnType = "text", comment = "登记姓名")
    private String name;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "text", comment = "手机号")
    private String mobile;

    /** 身份证号 */
    @MpField(value = "cert_id", columnType = "text", comment = "身份证号")
    private String certId;

    /** 体温 */
    @MpField(value = "temperature", columnType = "string", length = 30, comment = "体温")
    private String temperature;

    /** 职业 */
    @MpField(value = "job", columnType = "string", length = 100, comment = "职业")
    private String job;

    /** 症状 */
    @MpField(value = "symptom", columnType = "string", length = 50, comment = "症状")
    private String symptom;

    /** 症状描述 */
    @MpField(value = "symptom_des", columnType = "string", length = 500, nullable = true, comment = "症状描述")
    private String symptomDes;

    /** 是否去过中高风险地区：1 是，0 否 */
    @MpField(value = "is_risk_area", columnType = "integer", comment = "是否去过中高风险地区 1:是 0:否")
    private Integer isRiskArea;

    /** 是否使用这条登记信息：1 是，0 否 */
    @MpField(value = "is_use", columnType = "integer", comment = "是否使用这条登记信息 1:是 0:否", defaultValue = "1")
    private Integer isUse;

    /** 下单时间 */
    @MpField(value = "order_time", columnType = "integer", comment = "下单时间")
    private Integer orderTime;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
