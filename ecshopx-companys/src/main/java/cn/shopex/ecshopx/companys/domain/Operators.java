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
 * 账号表
 */
@Data
@MpTable(value = "operators", comment = "账号表", indexes = {@MpIndex(name = "idx_eid", columns = {"eid"})}, uniqueIndexes = {@MpIndex(name = "idx_passportuid", columns = {"passport_uid"})})
public class Operators {

    /** 账号id */
    @MpId(value = "operator_id", type = IdType.AUTO, columnType = "bigint", comment = "账号id")
    private Long operatorId;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", length = 255, comment = "手机号")
    private String mobile;

    /** 员工账号名 */
    @MpField(value = "login_name", columnType = "string", nullable = true, comment = "员工账号名")
    private String loginName;

    /** 操作员类型类型。admin:超级管理员;staff:员工;distributor:店铺管理员;dealer:经销商;merchant:商户;supplier:供应商;self_delivery_staff:自配送员 */
    @MpField(value = "operator_type", columnType = "string", comment = "操作员类型类型。admin:超级管理员;staff:员工;distributor:店铺管理员;dealer:经销商;merchant:商户;supplier:供应商;self_delivery_staff:自配送员", defaultValue = "admin")
    private String operatorType = "admin";

    @MpField(value = "password", columnType = "string", nullable = true)
    private String password;

    @MpField(value = "eid", columnType = "string", nullable = true)
    private String eid;

    @MpField(value = "passport_uid", columnType = "string", nullable = true)
    private String passportUid;

    /** 可选绑定 Shopex 登录账号 */
    @MpField(value = "shopex_bind_account", columnType = "string", length = 255, nullable = true, comment = "可选绑定 Shopex 登录账号")
    private String shopexBindAccount;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;

    /** 员工管理的店铺id集合 */
    @MpField(value = "distributor_ids", columnType = "text", nullable = true, comment = "员工管理的店铺id集合")
    private String distributorIds;

    /** 员工管理的门店id集合 */
    @MpField(value = "shop_ids", columnType = "text", nullable = true, comment = "员工管理的门店id集合")
    private String shopIds;

    /** 名称 */
    @MpField(value = "username", columnType = "string", nullable = true, comment = "名称")
    private String username;

    /** 头像 */
    @MpField(value = "head_portrait", columnType = "string", nullable = true, comment = "头像")
    private String headPortrait;

    /** 区域id */
    @MpField(value = "regionauth_id", columnType = "bigint", comment = "区域id", defaultValue = "0")
    private Long regionauthId = 0L;

    /** 联系人姓名 */
    @MpField(value = "contact", columnType = "string", length = 500, nullable = true, comment = "联系人姓名")
    private String contact;

    /** 分账信息 */
    @MpField(value = "split_ledger_info", columnType = "string", nullable = true, comment = "分账信息")
    private String splitLedgerInfo;

    /** 是否禁用。1:是 0:否 */
    @MpField(value = "is_disable", columnType = "boolean", nullable = true, comment = "是否禁用。1:是 0:否", defaultValue = "0")
    private Boolean isDisable = false;

    /** adapay子商户开户时间 */
    @MpField(value = "adapay_open_account_time", columnType = "string", nullable = true, comment = "adapay子商户开户时间")
    private String adapayOpenAccountTime;

    /** 经销商子账号父级id */
    @MpField(value = "dealer_parent_id", columnType = "string", nullable = true, comment = "经销商子账号父级id")
    private String dealerParentId;

    /** 是否是经销商主账号。1:是,0:否 */
    @MpField(value = "is_dealer_main", columnType = "boolean", comment = "是否是经销商主账号。1:是,0:否", defaultValue = "1")
    private Boolean isDealerMain = true;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", nullable = true, comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;

    /** 是否是商户端超级管理员。1:是,0:否 */
    @MpField(value = "is_merchant_main", columnType = "boolean", comment = "是否是商户端超级管理员。1:是,0:否", defaultValue = "0")
    private Boolean isMerchantMain = false;

    /** 是否是店铺超级管理员.1:是,0:否 */
    @MpField(value = "is_distributor_main", columnType = "boolean", comment = "是否是店铺超级管理员.1:是,0:否", defaultValue = "0")
    private Boolean isDistributorMain = false;
}
