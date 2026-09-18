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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import java.math.BigDecimal;
import lombok.Data;

/** 运费模板表 */
@Data
@MpTable(value = "shipping_templates", comment = "运费模板表")
public class ShippingTemplates {

    /** 运费模板id */
    @MpId(value = "template_id", type = IdType.AUTO, columnType = "bigint", comment = "运费模板id")
    private Long templateId;

    /** 商家id */
    @MpField(value = "company_id", columnType = "bigint", comment = "商家id")
    private Long companyId;

    /** 运费模板名称 */
    @MpField(value = "name", columnType = "string", length = 50, comment = "运费模板名称")
    private String name = "";

    /** 是否包邮 */
    @MpField(value = "is_free", columnType = "string", comment = "是否包邮", defaultValue = "0")
    private String isFree = "0";

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商id", defaultValue = "0")
    private Long supplierId = 0L;

    /** 运费计算参数来源 */
    @MpField(value = "valuation", columnType = "string", length = 1, comment = "运费计算参数来源", defaultValue = "1")
    private String valuation = "1";

    /** 物流保价 */
    @MpField(value = "protect", insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS, columnType = "boolean", nullable = true, comment = "物流保价", defaultValue = "False")
    private Boolean protect;

    /** 保价费率（精度 6,3） */
    @MpField(value = "protect_rate", insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS, columnType = "decimal", nullable = true, precision = 6, scale = 3, comment = "保价费率", defaultValue = "0.000")
    private BigDecimal protectRate;

    /** 保价费最低值（精度 10,2） */
    @MpField(value = "minprice", insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS, columnType = "decimal", nullable = true, precision = 10, scale = 2, comment = "保价费最低值", defaultValue = "0.00")
    private BigDecimal minprice;

    /** 是否开启 */
    @MpField(value = "status", columnType = "boolean", comment = "是否开启", defaultValue = "True")
    private Boolean status = true;

    /** 运费模板中运费信息对象，包含默认运费和指定地区运费 */
    @MpField(value = "fee_conf", columnType = "text", nullable = true, comment = "运费模板中运费信息对象，包含默认运费和指定地区运费")
    private String feeConf;

    /** 不包邮地区 */
    @MpField(value = "nopost_conf", columnType = "text", nullable = true, comment = "不包邮地区", defaultValue = "[]")
    private String nopostConf;

    /** 指定包邮的条件 */
    @MpField(value = "free_conf", columnType = "text", nullable = true, comment = "指定包邮的条件")
    private String freeConf;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 最后修改时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "最后修改时间")
    private Integer updateTime;
}
