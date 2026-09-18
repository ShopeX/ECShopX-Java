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

/** 实体订单关联自提信息 */
@Data
@MpTable(value = "orders_rel_ziti", comment = "实体订单关联自提信息", indexes = {@MpIndex(name = "idx_order_id", columns = {"order_id"})})
public class NormalOrdersRelZiti {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 自提点名称 */
    @MpField(value = "name", columnType = "string", comment = "自提点名称")
    private String name;

    /** 纬度 */
    @MpField(value = "lng", columnType = "string", nullable = true, comment = "纬度")
    private String lng;

    /** 经度 */
    @MpField(value = "lat", columnType = "string", nullable = true, comment = "经度")
    private String lat;

    /** 省 */
    @MpField(value = "province", columnType = "string", nullable = true, comment = "省")
    private String province;

    /** 市 */
    @MpField(value = "city", columnType = "string", nullable = true, comment = "市")
    private String city;

    /** 区 */
    @MpField(value = "area", columnType = "string", nullable = true, comment = "区")
    private String area;

    /** 地址 */
    @MpField(value = "address", columnType = "string", nullable = true, comment = "地址")
    private String address;

    /** 联系电话 */
    @MpField(value = "contract_phone", columnType = "string", length = 20, comment = "联系电话")
    private String contractPhone;

    /** 自提日期 */
    @MpField(value = "pickup_date", columnType = "string", length = 20, comment = "自提日期")
    private String pickupDate;

    /** 自提时间 */
    @MpField(value = "pickup_time", columnType = "string", length = 20, comment = "自提时间")
    private String pickupTime;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
