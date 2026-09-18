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

package cn.shopex.ecshopx.distribution.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 店铺自提点 */
@Data
@MpTable(value = "distribution_pickup_location", comment = "店铺自提点", indexes = {@MpIndex(name = "ix_distributor_id", columns = {"distributor_id"})})
public class PickupLocation {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 所属店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "所属店铺id")
    private Long distributorId;

    /** 绑定店铺id */
    @MpField(value = "rel_distributor_id", columnType = "bigint", comment = "绑定店铺id", defaultValue = "0")
    private Long relDistributorId = 0L;

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

    /** 营业时间 */
    @MpField(value = "hours", columnType = "string", nullable = true, comment = "营业时间")
    private String hours;

    /** 工作日：周一至周日->1-7，逗号分隔 */
    @MpField(value = "workdays", columnType = "string", nullable = true, comment = "工作日：周一至周日->1-7，逗号分隔", defaultValue = ",")
    private String workdays = ",";

    /** 最长预约时间，天 */
    @MpField(value = "wait_pickup_days", columnType = "string", nullable = true, comment = "最长预约时间，天", defaultValue = "0")
    private String waitPickupDays = "0";

    /** 当前最晚提货时间 */
    @MpField(value = "latest_pickup_time", columnType = "string", nullable = true, comment = "当前最晚提货时间")
    private String latestPickupTime;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
