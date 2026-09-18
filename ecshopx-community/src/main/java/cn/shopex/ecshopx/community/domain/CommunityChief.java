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

package cn.shopex.ecshopx.community.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 社区拼团团长表
 */
@Data
@MpTable(value = "community_chief", comment = "社区拼团团长表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_chief_mobile", columns = {"chief_mobile"}), @MpIndex(name = "ix_user_id", columns = {"user_id"})})
public class CommunityChief {

    /** 团长id */
    @MpId(value = "chief_id", type = IdType.AUTO, columnType = "bigint", comment = "团长id")
    private Long chiefId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 团长名称 */
    @MpField(value = "chief_name", columnType = "string", comment = "团长名称")
    private String chiefName;

    /** 团长头像 */
    @MpField(value = "chief_avatar", columnType = "string", comment = "团长头像")
    private String chiefAvatar;

    /** 团长手机号 */
    @MpField(value = "chief_mobile", columnType = "string", comment = "团长手机号")
    private String chiefMobile;

    /** 团长简介 */
    @MpField(value = "chief_desc", columnType = "string", comment = "团长简介")
    private String chiefDesc;

    /** 团长详细介绍，可为空 */
    @MpField(value = "chief_intro", columnType = "text", nullable = true, comment = "团长详细介绍")
    private String chiefIntro;

    /** 省，可为空 */
    @MpField(value = "province", columnType = "string", nullable = true, comment = "省")
    private String province;

    /** 市，可为空 */
    @MpField(value = "city", columnType = "string", nullable = true, comment = "市")
    private String city;

    /** 区，可为空 */
    @MpField(value = "area", columnType = "string", nullable = true, comment = "区")
    private String area;

    /** 地区编号集合（库内 JSON），可为空 */
    @MpField(value = "regions_id", columnType = "json_array", nullable = true, comment = "地区编号集合")
    private String regionsId;

    /** 地区名称集合（库内 JSON），可为空 */
    @MpField(value = "regions", columnType = "json_array", nullable = true, comment = "地区名称集合")
    private String regions;

    /** 具体地址，可为空，最长 500 */
    @MpField(value = "address", columnType = "string", length = 500, nullable = true, comment = "具体地址")
    private String address;

    /** 地图纬度，可为空 */
    @MpField(value = "lng", columnType = "string", nullable = true, comment = "地图纬度")
    private String lng;

    /** 地图经度，可为空 */
    @MpField(value = "lat", columnType = "string", nullable = true, comment = "地图经度")
    private String lat;

    /** 会员ID，可为空，默认 0 */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "会员ID", defaultValue = "0")
    private Long userId = 0L;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created_at", columnType = "integer")
    private Integer createdAt;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated_at", columnType = "integer", nullable = true)
    private Integer updatedAt;

    /** 团长提现的支付宝姓名，可为空 */
    @MpField(value = "alipay_name", columnType = "string", nullable = true, comment = "团长提现的支付宝姓名")
    private String alipayName;

    /** 团长提现的支付宝账号，可为空 */
    @MpField(value = "alipay_account", columnType = "string", nullable = true, comment = "团长提现的支付宝账号")
    private String alipayAccount;

    /** 银行名称，可为空 */
    @MpField(value = "bank_name", columnType = "string", nullable = true, comment = "银行名称")
    private String bankName;

    /** 团长提现的银行卡号，可为空 */
    @MpField(value = "bankcard_no", columnType = "string", nullable = true, comment = "团长提现的银行卡号")
    private String bankcardNo;
}
