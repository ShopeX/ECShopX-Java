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

package cn.shopex.ecshopx.members.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员主表，其他平台的会员本地化表 */
@Data
@MpTable(value = "members", comment = "会员主表，其他平台的会员本地化表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_company_id_user_id", columns = {"company_id", "user_id"}), @MpIndex(name = "idx_mobile", columns = {"mobile"}, lengths = {64}), @MpIndex(name = "idx_grade_id", columns = {"grade_id"}), @MpIndex(name = "idx_company_id_user_card_code", columns = {"company_id", "user_card_code"})}, uniqueIndexes = {@MpIndex(name = "mobile_company", columns = {"mobile", "company_id"}), @MpIndex(name = "login_email_company", columns = {"login_email", "company_id"})})
public class Members {

    /** 用户id */
    @MpId(value = "user_id", type = IdType.AUTO, columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 等级id */
    @MpField(value = "grade_id", columnType = "bigint", comment = "等级id")
    private Long gradeId;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", length = 255, comment = "手机号")
    private String mobile;

    /** 带区号的手机号 */
    @MpField(value = "region_mobile", columnType = "string", length = 50, comment = "带区号的手机号")
    private String regionMobile;

    /** 手机号的区号 */
    @MpField(value = "mobile_country_code", columnType = "string", length = 50, comment = "手机号的区号")
    private String mobileCountryCode;

    /** 登录邮箱（小写化存储） */
    @MpField(value = "login_email", columnType = "string", length = 255, nullable = true, comment = "登录邮箱(小写)")
    private String loginEmail;

    /** 邮箱激活时间（Unix 秒）；NULL=未激活 */
    @MpField(value = "email_verified_at", columnType = "integer", nullable = true, comment = "邮箱验证时间戳")
    private Long emailVerifiedAt;

    /** 密码 */
    @MpField(value = "password", columnType = "string", length = 255, comment = "密码")
    private String password;

    /** 会员卡号 */
    @MpField(value = "user_card_code", columnType = "string", comment = "会员卡号")
    private String userCardCode;

    /** 线下会员卡号 */
    @MpField(value = "offline_card_code", columnType = "string", nullable = true, comment = "线下会员卡号")
    private String offlineCardCode;

    /** 公众号的appid */
    @MpField(value = "authorizer_appid", columnType = "string", length = 64, nullable = true, comment = "公众号的appid")
    private String authorizerAppid;

    /** 小程序的appid */
    @MpField(value = "wxa_appid", columnType = "string", length = 64, nullable = true, comment = "小程序的appid")
    private String wxaAppid;

    /** 支付宝小程序appid */
    @MpField(value = "alipay_appid", columnType = "string", length = 64, nullable = true, comment = "支付宝小程序appid")
    private String alipayAppid;

    /** 推荐人id */
    @MpField(value = "inviter_id", columnType = "bigint", nullable = true, comment = "推荐人id")
    private Long inviterId = 0L;

    /** 来源类型 default默认 */
    @MpField(value = "source_from", columnType = "string", nullable = true, comment = "来源类型 default默认", defaultValue = "default")
    private String sourceFrom = "default";

    /** 来源id */
    @MpField(value = "source_id", columnType = "bigint", nullable = true, comment = "来源id")
    private Long sourceId;

    /** 监控页面id */
    @MpField(value = "monitor_id", columnType = "bigint", nullable = true, comment = "监控页面id")
    private Long monitorId;

    /** 最近来源id */
    @MpField(value = "latest_source_id", columnType = "bigint", nullable = true, comment = "最近来源id")
    private Long latestSourceId;

    /** 最近监控页面id */
    @MpField(value = "latest_monitor_id", columnType = "bigint", nullable = true, comment = "最近监控页面id")
    private Long latestMonitorId;

    /** 会员备注 */
    @MpField(value = "remarks", columnType = "string", nullable = true, comment = "会员备注")
    private String remarks;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    /** 创建年份，默认 0 */
    @MpField(value = "created_year", columnType = "integer", nullable = true, comment = "创建年份", defaultValue = "0")
    private Integer createdYear = 0;

    /** 创建月份，默认 0 */
    @MpField(value = "created_month", columnType = "integer", nullable = true, comment = "创建月份", defaultValue = "0")
    private Integer createdMonth = 0;

    /** 创建日期，默认 0 */
    @MpField(value = "created_day", columnType = "integer", nullable = true, comment = "创建日期", defaultValue = "0")
    private Integer createdDay = 0;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;

    /** 是否禁用。0:可用；1:禁用 */
    @MpField(value = "disabled", columnType = "boolean", comment = "是否禁用。0:可用；1:禁用", defaultValue = "0")
    private Boolean disabled = false;

    /** 是否可以使用积分 */
    @MpField(value = "use_point", columnType = "boolean", comment = "是否可以使用积分", defaultValue = "0")
    private Boolean usePoint = false;

    /** 第三方数据 */
    @MpField(value = "third_data", columnType = "string", nullable = true, comment = "第三方数据")
    private String thirdData;

    /** 注册时的分销商ID */
    @MpField(value = "reg_distributor", columnType = "integer", nullable = true, comment = "注册时的分销商ID", defaultValue = "0")
    private Integer regDistributor = 0;

    /** 注册时的导购ID */
    @MpField(value = "reg_salesperson", columnType = "string", length = 100, nullable = true, comment = "注册时的导购ID")
    private String regSalesperson;

    /** 作为分配的店铺ID */
    @MpField(value = "op_distributor", columnType = "integer", nullable = true, comment = "作为分配的店铺ID", defaultValue = "0")
    private Integer opDistributor = 0;

    /** 分配的导购员工编号(employee_number/work_userid) */
    @MpField(value = "fp_salesperson", columnType = "string", length = 100, nullable = true, comment = "分配的导购员工编号(employee_number/work_userid)")
    private String fpSalesperson;

    /** 是否有分配导购 */
    @MpField(value = "has_fp", columnType = "boolean", comment = "是否有分配导购", defaultValue = "0")
    private Boolean hasFp = false;

    /** 是否已加为好友。0:否；1:是 */
    @MpField(value = "is_become_friend", columnType = "boolean", comment = "是否已加为好友。0:否；1:是", defaultValue = "0")
    private Boolean isBecomeFriend = false;

    /** 数云 OPEN 线上 wxapp 同步成功时间（Unix）；NULL 未成功 */
    @MpField(
            value = "shuyun_open_online_wxapp_sync_at",
            columnType = "integer",
            nullable = true,
            comment = "数云 OPEN 线上 wxapp 同步成功时间（Unix）；NULL 未成功")
    private Integer shuyunOpenOnlineWxappSyncAt;

    /** 店务 OFFLINE member.register 成功时写入的分销商 ID；NULL 未写入 */
    @MpField(
            value = "offline_reg_distributor",
            columnType = "integer",
            nullable = true,
            comment = "店务 OFFLINE member.register 成功时写入的分销商 ID；NULL 未写入")
    private Integer offlineRegDistributor;

    /** 会员姓名（与 members_info 联查映射，非 members 表物理列） */
    @MpField(value = "username", exist = false)
    private String username;
}
