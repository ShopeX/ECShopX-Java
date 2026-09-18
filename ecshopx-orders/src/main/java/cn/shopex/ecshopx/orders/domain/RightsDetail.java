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

/** 权益明细 */
@Data
@MpTable(value = "orders_rights_detail", comment = "权益详细ID", uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"rights_id", "item_id"})})
public class RightsDetail {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
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

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", length = 64, comment = "商品id")
    private Long itemId;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", length = 255, comment = "商品名称")
    private String itemName;

    /** 数值属性ID */
    @MpField(value = "label_id", columnType = "bigint", comment = "数值属性ID")
    private Long labelId;

    /** 数值属性名称 */
    @MpField(value = "label_name", columnType = "string", length = 255, comment = "数值属性名称")
    private String labelName;

    /** 服务商品原始总次数 */
    @MpField(value = "total_num", columnType = "bigint", comment = "服务商品原始总次数")
    private Long totalNum;

    /** 总消耗次数 */
    @MpField(value = "total_consum_num", columnType = "bigint", comment = "总消耗次数", defaultValue = "0")
    private Long totalConsumNum;

    /** 权益开始时间 */
    @MpField(value = "start_time", columnType = "string", comment = "权益开始时间")
    private String startTime;

    /** 权益结束时间 */
    @MpField(value = "end_time", columnType = "string", comment = "权益结束时间")
    private String endTime;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
