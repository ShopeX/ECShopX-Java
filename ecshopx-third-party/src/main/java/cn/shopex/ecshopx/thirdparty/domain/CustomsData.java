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

package cn.shopex.ecshopx.thirdparty.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 接收海关支付数据请求表 */
@Data
@MpTable(value = "customs_data", comment = "接收海关支付数据请求表")
public class CustomsData {

    /** 订单号 */
    @MpId(value = "order_id", type = IdType.INPUT, columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 海关发起请求时，平台接收的会话ID */
    @MpField(value = "session_id", columnType = "string", comment = "海关发起请求时，平台接收的会话ID")
    private String sessionId;

    /** 调用时的系统时间 */
    @MpField(value = "service_time", columnType = "bigint", length = 15, comment = "调用时的系统时间")
    private Long serviceTime;

    /** 是否上报 0否 1是 */
    @MpField(value = "status", columnType = "boolean", comment = "是否上报 0否 1是", defaultValue = "0")
    private Boolean status = false;
}
