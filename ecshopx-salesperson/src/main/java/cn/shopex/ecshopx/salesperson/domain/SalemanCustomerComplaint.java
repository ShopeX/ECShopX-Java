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

package cn.shopex.ecshopx.salesperson.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 导购客诉表 */
@Data
@MpTable(value = "companys_saleman_customer_complaints", comment = "导购客诉表")
public class SalemanCustomerComplaint {

    /** 主键id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "integer", comment = "主键id")
    private Integer id;

    /** 会员id */
    @MpField(value = "user_id", columnType = "integer", comment = "会员id")
    private Integer userId;

    /**  */
    @MpField(value = "company_id", columnType = "integer")
    private Integer companyId;

    /** 会员名 */
    @MpField(value = "user_name", columnType = "string", comment = "会员名")
    private String userName;

    /** 会员手机号 */
    @MpField(value = "user_mobile", columnType = "string", comment = "会员手机号")
    private String userMobile;

    /** 导购员id */
    @MpField(value = "saleman_id", columnType = "integer", comment = "导购员id")
    private Integer salemanId;

    /** 导购员名 */
    @MpField(value = "saleman_name", columnType = "string", comment = "导购员名")
    private String salemanName;

    /** 导购员企业微信头像 */
    @MpField(value = "saleman_avatar", columnType = "string", nullable = true, comment = "导购员企业微信头像")
    private String salemanAvatar;

    /** 导购员手机号 */
    @MpField(value = "saleman_mobile", columnType = "string", comment = "导购员手机号")
    private String salemanMobile;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 导购员所在店铺名 */
    @MpField(value = "saleman_distribution_name", columnType = "string", comment = "导购员所在店铺名")
    private String salemanDistributionName = "";

    /** 投诉内容 */
    @MpField(value = "complaints_content", columnType = "string", comment = "投诉内容")
    private String complaintsContent;

    /** 投诉图片 */
    @MpField(value = "complaints_images", columnType = "text", nullable = true, comment = "投诉图片")
    private String complaintsImages;

    /** 回复状态:0未回复；1已回复 */
    @MpField(value = "reply_status", columnType = "boolean", comment = "回复状态:0未回复；1已回复", defaultValue = "0")
    private Boolean replyStatus = false;

    /** 回复内容，json数组，内含操作员id，操作员手机号，操作员名称，回复时间，回复内容 */
    @MpField(value = "reply_content", columnType = "text", nullable = true, comment = "回复内容，json数组，内含操作员id，操作员手机号，操作员名称，回复时间，回复内容")
    private String replyContent;

    /** 回复时间 */
    @MpField(value = "reply_time", columnType = "integer", nullable = true, comment = "回复时间", defaultValue = "0")
    private Integer replyTime = 0;

    /** 回复操作员id */
    @MpField(value = "reply_operator_id", columnType = "integer", nullable = true, comment = "回复操作员id", defaultValue = "0")
    private Integer replyOperatorId = 0;

    /** 回复操作员名称 */
    @MpField(value = "reply_operator_name", columnType = "string", nullable = true, comment = "回复操作员名称")
    private String replyOperatorName = "";

    /** 回复操作员手机号 */
    @MpField(value = "reply_operator_mobile", columnType = "string", nullable = true, comment = "回复操作员手机号")
    private String replyOperatorMobile = "";

    /**  */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /**  */
    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
