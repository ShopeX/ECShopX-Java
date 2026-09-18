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

package cn.shopex.ecshopx.popularize.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 推广员表 */
@Data
@MpTable(value = "popularize_promoter", comment = "推广员表", indexes = {@MpIndex(name = "idx_pid", columns = {"pid"}), @MpIndex(name = "idx_companyid_userid", columns = {"company_id", "user_id"}), @MpIndex(name = "idx_identity_id", columns = {"identity_id"}), @MpIndex(name = "idx_is_subordinates", columns = {"is_subordinates"})})
public class Promoter {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 企业ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业ID")
    private Long companyId;

    /** 会员ID */
    @MpField(value = "user_id", columnType = "bigint", comment = "会员ID")
    private Long userId;

    /** 推广员身份ID */
    @MpField(value = "identity_id", columnType = "bigint", comment = "推广员身份ID", defaultValue = "0")
    private Long identityId = 0L;

    /** 是否可发展下级分销员 */
    @MpField(value = "is_subordinates", columnType = "integer", length = 4, comment = "是否可发展下级分销员", defaultValue = "0")
    private Integer isSubordinates = 0;

    /** 上级会员ID */
    @MpField(value = "pid", columnType = "bigint", nullable = true, comment = "上级会员ID")
    private Long pid;

    /** 上级手机号 */
    @MpField(value = "pmobile", columnType = "string", nullable = true, comment = "上级手机号")
    private String pmobile;

    /** 上级推广员名称 */
    @MpField(value = "pname", columnType = "string", nullable = true, comment = "上级推广员名称")
    private String pname;

    /** 推广员自定义店铺名称 */
    @MpField(value = "shop_name", columnType = "string", nullable = true, comment = "推广员自定义店铺名称")
    private String shopName;

    /** 推广员提现的支付宝姓名 */
    @MpField(value = "alipay_name", columnType = "string", nullable = true, comment = "推广员提现的支付宝姓名")
    private String alipayName;

    /** 推广店铺描述 */
    @MpField(value = "brief", columnType = "string", nullable = true, comment = "推广店铺描述")
    private String brief;

    /** 推广店铺封面 */
    @MpField(value = "shop_pic", columnType = "string", nullable = true, comment = "推广店铺封面")
    private String shopPic;

    /** 推广员提现的支付宝账号 */
    @MpField(value = "alipay_account", columnType = "string", nullable = true, comment = "推广员提现的支付宝账号")
    private String alipayAccount;

    /** 推广员等级 */
    @MpField(value = "grade_level", columnType = "integer", length = 4, comment = "推广员等级")
    private Integer gradeLevel;

    /** 是否为推广员 */
    @MpField(value = "is_promoter", columnType = "integer", length = 4, comment = "是否为推广员")
    private Integer isPromoter;

    /**
     * 开店状态 0 未开店 1已开店 2申请中 3禁用 4申请审核拒绝
     */
    @MpField(value = "shop_status", columnType = "integer", length = 4, comment = "开店状态 0 未开店 1已开店 2申请中 3禁用 4申请审核拒绝 ", defaultValue = "0")
    private Integer shopStatus = 0;

    /** 审核拒绝原因 */
    @MpField(value = "reason", columnType = "string", nullable = true, comment = "审核拒绝原因")
    private String reason;

    /** 是否有效 */
    @MpField(value = "disabled", columnType = "integer", comment = "是否有效")
    private Integer disabled;

    /** 是否有购买记录 */
    @MpField(value = "is_buy", columnType = "integer", comment = "是否有购买记录")
    private Integer isBuy;

    /** 推广员名称 */
    @MpField(value = "promoter_name", columnType = "string", nullable = true, comment = "推广员名称")
    private String promoterName;

    /** 地区ID */
    @MpField(value = "regions_id", columnType = "text", nullable = true, comment = "地区ID")
    private String regionsId;

    /** 地址 */
    @MpField(value = "address", columnType = "string", nullable = true, comment = "地址")
    private String address;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
