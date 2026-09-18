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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 资源包表
 */
@Data
@MpTable(value = "resources", comment = "资源包表")
public class Resources {

    /** 资源包id */
    @MpId(value = "resource_id", type = IdType.AUTO, columnType = "bigint", comment = "资源包id")
    private Long resourceId;

    /** 资源包名称 */
    @MpField(value = "resource_name", columnType = "string", comment = "资源包名称")
    private String resourceName;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 企业id */
    @MpField(value = "eid", columnType = "string", nullable = true, comment = "企业id")
    private String eid;

    @MpField(value = "passport_uid", columnType = "string", nullable = true)
    private String passportUid;

    /** 资源门店数 */
    @MpField(value = "shop_num", columnType = "integer", comment = "资源门店数")
    private Integer shopNum;

    /** 可用门店数 */
    @MpField(value = "left_shop_num", columnType = "integer", comment = "可用门店数")
    private Integer leftShopNum;

    /** 资源来源。demo:开通试用,purchased:购买,gift:赠品 */
    @MpField(value = "source", columnType = "string", comment = "资源来源。demo:开通试用,purchased:购买,gift:赠品")
    private String source;

    /** 可用天数 */
    @MpField(value = "available_days", columnType = "integer", comment = "可用天数")
    private Integer availableDays;

    /** 激活时间 */
    @MpField(value = "active_at", columnType = "bigint", comment = "激活时间")
    private Long activeAt;

    /** 过期时间 */
    @MpField(value = "expired_at", columnType = "bigint", comment = "过期时间")
    private Long expiredAt;

    /** 激活码 */
    @MpField(value = "active_code", columnType = "string", length = 255, nullable = true, comment = "激活码")
    private String activeCode;

    /** 在线开通工单号 */
    @MpField(value = "issue_id", columnType = "string", length = 50, nullable = true, comment = "在线开通工单号")
    private String issueId;

    /** 商品code */
    @MpField(value = "goods_code", columnType = "string", length = 50, nullable = true, comment = "商品code")
    private String goodsCode;

    /** 基础系统code */
    @MpField(value = "product_code", columnType = "string", length = 50, nullable = true, comment = "基础系统code")
    private String productCode;
}
