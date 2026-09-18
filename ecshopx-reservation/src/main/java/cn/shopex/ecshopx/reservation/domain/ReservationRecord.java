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

package cn.shopex.ecshopx.reservation.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 预约记录表 */
@Data
@MpTable(value = "reservation_record", comment = "预约记录表")
public class ReservationRecord {

    /** id */
    @MpId(value = "record_id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long recordId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", comment = "门店id")
    private Long shopId;

    /** 门店名称 */
    @MpField(value = "shop_name", columnType = "string", length = 100, nullable = true, comment = "门店名称")
    private String shopName;

    /** 约定日期 */
    @MpField(value = "agreement_date", columnType = "integer", comment = "约定日期")
    private Integer agreementDate;

    /** 到店时间(时间戳) */
    @MpField(value = "to_shop_time", columnType = "integer", nullable = true, comment = "到店时间(时间戳)")
    private Integer toShopTime;

    /** 到店时间(时刻字符串) */
    @MpField(value = "begin_time", columnType = "string", length = 5, comment = "到店时间(时刻字符串)")
    private String beginTime;

    /** 约定结束时刻 */
    @MpField(value = "end_time", columnType = "string", length = 5, nullable = true, comment = "约定结束时刻")
    private String endTime;

    /**
     * 预约状态。可选值有 cancel-取消;-to_the_shop-已到店;-not_to_shop-未到店;-success-预约成功;-system-系统占位;
     */
    @MpField(value = "status", columnType = "string", comment = "预约状态。可选值有 cancel-取消;-to_the_shop-已到店;-not_to_shop-未到店;-success-预约成功;-system-系统占位;")
    private String status;

    /** 预约数量 */
    @MpField(value = "num", columnType = "integer", comment = "预约数量", defaultValue = "1")
    private Integer num = 1;

    /** 用户user id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户user id")
    private Long userId;

    /** 用户名称 */
    @MpField(value = "user_name", columnType = "string", length = 100, nullable = true, comment = "用户名称")
    private String userName;

    /** 用户性别 */
    @MpField(value = "sex", columnType = "integer", nullable = true, comment = "用户性别")
    private Integer sex;

    /** 预约人手机号 */
    @MpField(value = "mobile", columnType = "string", nullable = true, comment = "预约人手机号")
    private String mobile;

    /** 资源位id */
    @MpField(value = "resource_level_id", columnType = "bigint", nullable = true, comment = "资源位id")
    private Long resourceLevelId;

    /** 资源位名称 */
    @MpField(value = "resource_level_name", columnType = "string", length = 100, nullable = true, comment = "资源位名称")
    private String resourceLevelName;

    /** 服务商品id */
    @MpField(value = "rights_id", columnType = "bigint", nullable = true, comment = "服务商品id")
    private Long rightsId;

    /** 服务商品名称 */
    @MpField(value = "rights_name", columnType = "string", length = 100, nullable = true, comment = "服务商品名称")
    private String rightsName;

    /** 服务商品id */
    @MpField(value = "label_id", columnType = "bigint", nullable = true, comment = "服务商品id")
    private Long labelId;

    /** 服务商品名称 */
    @MpField(value = "label_name", columnType = "string", length = 100, nullable = true, comment = "服务商品名称")
    private String labelName;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
