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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 商城数据统计表
 */
@Data
@MpTable(value = "companys_statistics", comment = "商城数据统计表", indexes = {@MpIndex(name = "ix_add_date", columns = {"add_date"}), @MpIndex(name = "ix_statistic_type", columns = {"statistic_type"}), @MpIndex(name = "ix_statistic_title", columns = {"statistic_title"}), @MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class Statistics {

    /** 激活id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "激活id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 统计日期 Ymd */
    @MpField(value = "add_date", columnType = "integer", comment = "统计日期 Ymd")
    private Integer addDate;

    /** 统计数据描述，order_pay_num:订单支付数量,order_pay_fee:订单支付金额,order_pay_user_num:订单支付会员数,vip_user:新增vip会员数,svip_user:新增svip会员数,add_user:注册会员数 */
    @MpField(value = "statistic_title", columnType = "string", comment = "统计数据描述，order_pay_num:订单支付数量,order_pay_fee:订单支付金额,order_pay_user_num:订单支付会员数,vip_user:新增vip会员数,svip_user:新增svip会员数,add_user:注册会员数")
    private String statisticTitle;

    /** 统计类型，normal:实体订单,service:服务订单,member:会员 */
    @MpField(value = "statistic_type", columnType = "string", comment = "统计类型，normal:实体订单,service:服务订单,member:会员")
    private String statisticType;

    /** 统计数据 */
    @MpField(value = "data_value", columnType = "integer", comment = "统计数据", defaultValue = "0")
    private Integer dataValue = 0;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
