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

package cn.shopex.ecshopx.employeepurchase.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 企业员工家属表 */
@Data
@MpTable(value = "employee_purchase_relatives", comment = "企业员工家属表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "employee_user_id", columns = {"employee_user_id"}), @MpIndex(name = "activity_id", columns = {"activity_id"}), @MpIndex(name = "user_id", columns = {"user_id"}), @MpIndex(name = "idx_member_mobile", columns = {"member_mobile"}, lengths = {64}), @MpIndex(name = "idx_enterprise_userid_actid", columns = {"enterprise_id", "user_id", "activity_id"})})
public class Relatives {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id,为0时表示为商城的员工 */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺id,为0时表示为商城的员工", defaultValue = "0")
    private Integer distributorId = 0;

    /** 企业id */
    @MpField(value = "enterprise_id", columnType = "bigint", comment = "企业id")
    private Long enterpriseId;

    /** 会员id */
    @MpField(value = "user_id", columnType = "bigint", comment = "会员id")
    private Long userId;

    /** 会员手机号 */
    @MpField(value = "member_mobile", columnType = "string", length = 255, comment = "会员手机号")
    private String memberMobile;

    /** 活动ID */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动ID")
    private Long activityId;

    /** 员工id */
    @MpField(value = "employee_id", columnType = "bigint", comment = "员工id")
    private Long employeeId;

    /** 员工关联用户id */
    @MpField(value = "employee_user_id", columnType = "bigint", comment = "员工关联用户id")
    private Long employeeUserId;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Integer created;

    /** 失效 */
    @MpField(value = "disabled", columnType = "boolean", comment = "失效", defaultValue = "0")
    private Boolean disabled = false;
}
