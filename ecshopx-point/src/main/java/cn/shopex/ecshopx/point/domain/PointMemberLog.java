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

package cn.shopex.ecshopx.point.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 用户积分记录表 */
@Data
@MpTable(value = "point_member_log", comment = "用户积分记录表", indexes = {@MpIndex(name = "idx_company_id_external_id", columns = {"company_id", "external_id"}), @MpIndex(name = "idx_company_id_operater", columns = {"company_id", "operater"})})
public class PointMemberLog {

    /** 积分记录id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "积分记录id")
    private Long id;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /**
     * 积分交易类型，1:注册送积分 2.推荐送分 3.充值返积分 4.推广注册返积分 5.积分换购 6.储值兑换积分 7.订单返积分 8.会员等级返佣 9.取消订处理积分 10.售后处理积分
     * 11.大转盘抽奖送积分 12:管理员手动调整积分 13.外部开发者同步进来的会员积分 14:会员信息导入，初始化积分 15:会员信息导入，更新会员调整积分; 16 活动报名送积分
     */
    @MpField(value = "journal_type", columnType = "smallint", comment = "积分交易类型，1:注册送积分 2.推荐送分 3.充值返积分 4.推广注册返积分 5.积分换购 6.储值兑换积分 7.订单返积分 8.会员等级返佣 9.取消订处理积分 10.售后处理积分 11.大转盘抽奖送积分 12:管理员手动调整积分 13.外部开发者同步进来的会员积分 14:会员信息导入，初始化积分 15:会员信息导入，更新会员调整积分; 16 活动报名送积分")
    private Integer journalType;

    /** 积分描述 */
    @MpField(value = "point_desc", columnType = "string", comment = "积分描述")
    private String pointDesc = "";

    /** 入账积分 */
    @MpField(value = "income", columnType = "integer", comment = "入账积分", defaultValue = "0")
    private Integer income = 0;

    /** 出账积分 */
    @MpField(value = "outcome", columnType = "integer", comment = "出账积分", defaultValue = "0")
    private Integer outcome = 0;

    /** 订单号(充值) */
    @MpField(value = "order_id", columnType = "string", length = 64, nullable = true, comment = "订单号(充值)")
    private String orderId;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 外部唯一标识，外部调用方自定义的值 */
    @MpField(value = "external_id", columnType = "string", length = 50, comment = "外部唯一标识，外部调用方自定义的值")
    private String externalId = "";

    /** 操作员名称 */
    @MpField(value = "operater", columnType = "string", length = 50, comment = "操作员名称")
    private String operater = "";

    /** 操作员备注 */
    @MpField(value = "operater_remark", columnType = "string", length = 255, comment = "操作员备注")
    private String operaterRemark = "";
}
