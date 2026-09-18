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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 店铺售后地址 */
@Data
@MpTable(value = "distributor_aftersales_address", comment = "店铺售后地址")
public class DistributorAftersalesAddress {

    @MpId(value = "address_id", type = IdType.AUTO, columnType = "bigint")
    private Long addressId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id")
    private Long distributorId;

    /** 企业id */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业id")
    private Long companyId;

    /** 省 */
    @MpField(value = "province", columnType = "string", comment = "省")
    private String province;

    /** 市 */
    @MpField(value = "city", columnType = "string", comment = "市")
    private String city;

    /** 区/县 */
    @MpField(value = "area", columnType = "string", comment = "区/县")
    private String area;

    @MpField(value = "regions_id", columnType = "text")
    private String regionsId;

    @MpField(value = "regions", columnType = "text")
    private String regions;

    /** 地址 */
    @MpField(value = "address", columnType = "string", comment = "地址")
    private String address;

    /** 纬度 */
    @MpField(value = "lng", columnType = "string", nullable = true, comment = "纬度")
    private String lng;

    /** 经度 */
    @MpField(value = "lat", columnType = "string", nullable = true, comment = "经度")
    private String lat;

    /** 联系人 */
    @MpField(value = "contact", columnType = "string", length = 500, nullable = true, comment = "联系人")
    private String contact;

    /** 联系人手机号 */
    @MpField(value = "mobile", columnType = "string", comment = "联系人手机号")
    private String mobile;

    /** 邮政编码 */
    @MpField(value = "post_code", columnType = "integer", nullable = true, comment = "邮政编码")
    private Integer postCode;

    /** 默认地址, 1:是。2:不是 */
    @MpField(value = "is_default", columnType = "integer", comment = "默认地址, 1:是。2:不是", defaultValue = "2")
    private Integer isDefault = 2;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;

    /** 退货点名称 */
    @MpField(value = "name", columnType = "string", nullable = true, comment = "退货点名称")
    private String name;

    /** 营业时间 */
    @MpField(value = "hours", columnType = "string", length = 50, nullable = true, comment = "营业时间")
    private String hours;

    /** 退货方式：logistics寄回 offline到店退 */
    @MpField(value = "return_type", columnType = "string", length = 20, comment = "退货方式：logistics寄回 offline到店退", defaultValue = "logistics")
    private String returnType = "logistics";

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "integer", comment = "供应商id", defaultValue = "0")
    private Integer supplierId = 0;
}
