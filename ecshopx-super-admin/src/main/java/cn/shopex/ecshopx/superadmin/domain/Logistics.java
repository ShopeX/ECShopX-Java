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

package cn.shopex.ecshopx.superadmin.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 物流公司表 */
@Data
@MpTable(value = "logistics", comment = "物流公司表")
public class Logistics {

    /** 物流公司ID */
    @MpId(value = "corp_id", type = IdType.AUTO, columnType = "smallint", comment = "物流公司ID")
    private Integer corpId;

    /** 快递鸟代码 */
    @MpField(value = "corp_code", columnType = "string", comment = "快递鸟代码")
    private String corpCode;

    /** 快递100代码 */
    @MpField(value = "kuaidi_code", columnType = "string", comment = "快递100代码")
    private String kuaidiCode;

    /** 物流公司全名 */
    @MpField(value = "full_name", columnType = "string", comment = "物流公司全名")
    private String fullName;

    /** 物流公司简称 */
    @MpField(value = "corp_name", columnType = "string", comment = "物流公司简称")
    private String corpName;

    /** 排序，默认 99 */
    @MpField(value = "order_sort", columnType = "smallint", comment = "排序", defaultValue = "99")
    private Integer orderSort = 99;

    /** 是否自定义，默认 false，可为空 */
    @MpField(value = "custom", columnType = "boolean", nullable = true, comment = "是否自定义", defaultValue = "False")
    private Boolean custom = false;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 物流公司电话，最长 20，可为空 */
    @MpField(value = "phone", columnType = "string", length = 20, nullable = true, comment = "物流公司电话")
    private String phone;

    /** logo，可为空 */
    @MpField(value = "logo", columnType = "string", nullable = true, comment = "logo")
    private String logo;
}
