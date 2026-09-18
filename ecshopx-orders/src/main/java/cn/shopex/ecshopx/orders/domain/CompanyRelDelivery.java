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

/** 商城关联配送配置表 */
@Data
@MpTable(value = "company_rel_delivery", comment = "商城关联配送配置表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class CompanyRelDelivery {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 配送类型【1 商家自配-按整单计算】【2 商家自配-按距离计算】 */
    @MpField(value = "type", columnType = "boolean", comment = "配送类型【1 商家自配-按整单计算】【2 商家自配-按距离计算】")
    private Integer type;

    /** 状态【1 启用】【0 禁用】 */
    @MpField(value = "status", columnType = "boolean", comment = "状态【1 启用】【0 禁用】")
    private Integer status;

    /** json存储，运费规则 */
    @MpField(value = "rules", columnType = "text", comment = "json存储，运费规则")
    private String rules;

    /** json存储，其他参数 */
    @MpField(value = "other_params", columnType = "text", comment = "json存储，其他参数")
    private String otherParams;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer", nullable = true, comment = "创建时间")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
