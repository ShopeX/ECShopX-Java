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
 * 店务端微信关联表
 */
@Data
@MpTable(value = "distributor_wechat_rel", comment = "店务端微信关联表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_operator_id", columns = {"operator_id"})}, uniqueIndexes = {@MpIndex(name = "ix_company_operator", columns = {"company_id", "app_type", "operator_id"}), @MpIndex(name = "ix_company_wx_user", columns = {"company_id", "app_id", "openid"})})
public class DistributorWechatRel {

    /** 微信用户关联表自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "微信用户关联表自增id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 微信app_id */
    @MpField(value = "app_id", columnType = "string", nullable = true, comment = "微信app_id")
    private String appId = "";

    /** 类型：wx[公众号],wxa[小程序] */
    @MpField(value = "app_type", columnType = "string", comment = "类型：wx[公众号],wxa[小程序]", defaultValue = "wx")
    private String appType = "wx";

    /** 微信openid */
    @MpField(value = "openid", columnType = "string", nullable = true, comment = "微信openid")
    private String openid = "";

    /** 微信unionid */
    @MpField(value = "unionid", columnType = "string", comment = "微信unionid")
    private String unionid = "";

    /** 系统账户id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "系统账户id", defaultValue = "0")
    private Long operatorId = 0L;

    /** 绑定时间 */
    @MpField(value = "bound_time", columnType = "bigint", nullable = true, comment = "绑定时间", defaultValue = "0")
    private Long boundTime = 0L;
}
