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

/** 实体订单关联达达同城配表 */
@Data
@MpTable(value = "orders_rel_dada", comment = "实体订单关联达达同城配表", indexes = {@MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_dada_status", columns = {"dada_status"})})
public class NormalOrdersRelDada {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /**
     * 达达状态：0 待处理，1 待接单，2 待取货，3 配送中，4 已完成，5 已取消，9 妥投异常之物品返回中，10
     * 妥投异常之物品返回完成，100 骑士到店，1000 创建达达运单失败
     */
    @MpField(value = "dada_status", columnType = "integer", comment = "达达状态 0:待处理,1:待接单,2:待取货,3:配送中,4:已完成,5:已取消,9:妥投异常之物品返回中,10:妥投异常之物品返回完成,100: 骑士到店,1000:创建达达运单失败", defaultValue = "0")
    private Integer dadaStatus;

    /** 达达平台订单号 */
    @MpField(value = "dada_delivery_no", columnType = "string", length = 50, nullable = true, comment = "达达平台订单号")
    private String dadaDeliveryNo;

    /**
     * 订单取消原因来源：1 达达回调配送员取消；2 达达回调商家主动取消；3 达达回调系统或客服取消；11
     * 商城系统取消；12 商城商家主动取消；13 商城消费者主动取消
     */
    @MpField(value = "dada_cancel_from", columnType = "integer", comment = "订单取消原因来源 1:达达回调配送员取消；2:达达回调商家主动取消；3:达达回调系统或客服取消；11:商城系统取消；12:商城商家主动取消；13:商城消费者主动取消；")
    private Integer dadaCancelFrom;

    /** 达达配送员id */
    @MpField(value = "dm_id", columnType = "integer", nullable = true, comment = "达达配送员id")
    private Integer dmId;

    /** 配送员姓名 */
    @MpField(value = "dm_name", columnType = "string", length = 20, nullable = true, comment = "配送员姓名")
    private String dmName;

    /** 配送员手机号 */
    @MpField(value = "dm_mobile", columnType = "string", nullable = true, comment = "配送员手机号")
    private String dmMobile;

    /** 取货时间 */
    @MpField(value = "pickup_time", columnType = "integer", comment = "取货时间")
    private Integer pickupTime;

    /** 商家接单时间 */
    @MpField(value = "accept_time", columnType = "integer", comment = "商家接单时间")
    private Integer acceptTime;

    /** 送达时间 */
    @MpField(value = "delivered_time", columnType = "integer", comment = "送达时间")
    private Integer deliveredTime;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
