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

package cn.shopex.ecshopx.promotions.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 用户砍价表 */
@Data
@MpTable(value = "promotions_user_bargains", comment = "用户砍价表", uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"bargain_id", "user_id"})})
public class UserBargains {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 砍价ID */
    @MpField(value = "bargain_id", columnType = "bigint", comment = "砍价ID")
    private Long bargainId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 公众号的appid */
    @MpField(value = "authorizer_appid", columnType = "string", length = 64, nullable = true, comment = "公众号的appid")
    private String authorizerAppid;

    /** 公众号的appid */
    @MpField(value = "wxa_appid", columnType = "string", length = 64, nullable = true, comment = "公众号的appid")
    private String wxaAppid;

    /** 订单标题 */
    @MpField(value = "item_name", columnType = "string", nullable = true, comment = "订单标题")
    private String itemName;

    /** 市场金额,单位为‘分’ */
    @MpField(value = "mkt_price", columnType = "integer", comment = "市场金额,单位为‘分’")
    private Integer mktPrice;

    /** 购买金额,单位为‘分’ */
    @MpField(value = "price", columnType = "integer", comment = "购买金额,单位为‘分’")
    private Integer price;

    /** 砍价次数 */
    @MpField(value = "cutprice_num", columnType = "integer", comment = "砍价次数")
    private Integer cutpriceNum;

    /** 预先生成的砍价详情 */
    @MpField(value = "cutprice_range", columnType = "json_array", comment = "预先生成的砍价详情")
    private String cutpriceRange;

    /** 已砍金额,单位为‘分’ */
    @MpField(value = "cutdown_amount", columnType = "integer", comment = "已砍金额,单位为‘分’", defaultValue = "0")
    private Integer cutdownAmount = 0;

    /** 是否已下单 */
    @MpField(value = "is_ordered", columnType = "boolean", comment = "是否已下单", defaultValue = "False")
    private Boolean isOrdered = false;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
