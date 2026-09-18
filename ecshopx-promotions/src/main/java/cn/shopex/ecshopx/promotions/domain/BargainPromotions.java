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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 砍价活动表 */
@Data
@MpTable(value = "promotions_bargain", comment = "砍价活动表")
public class BargainPromotions {

    /** 砍价活动ID */
    @MpId(value = "bargain_id", type = IdType.AUTO, columnType = "bigint", comment = "砍价活动ID")
    private Long bargainId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 活动名称 */
    @MpField(value = "title", columnType = "string", comment = "活动名称")
    private String title;

    /** 广告图 */
    @MpField(value = "ad_pic", columnType = "string", comment = "广告图")
    private String adPic;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", length = 255, comment = "商品名称")
    private String itemName;

    /** 商品图片 */
    @MpField(value = "item_pics", columnType = "string", comment = "商品图片")
    private String itemPics;

    /** 商品详情 */
    @MpField(value = "item_intro", columnType = "text", nullable = true, comment = "商品详情")
    private String itemIntro;

    /** 市场价格,单位为‘分’ */
    @MpField(value = "mkt_price", columnType = "integer", comment = "市场价格,单位为‘分’")
    private Integer mktPrice;

    /** 购买价格,单位为‘分’ */
    @MpField(value = "price", columnType = "integer", comment = "购买价格,单位为‘分’")
    private Integer price;

    /** 购买限制 */
    @MpField(value = "limit_num", columnType = "integer", comment = "购买限制")
    private Integer limitNum;

    /** 已购买数量 */
    @MpField(value = "order_num", columnType = "integer", comment = "已购买数量", defaultValue = "0")
    private Integer orderNum = 0;

    /** 砍价规则 */
    @MpField(value = "cutdown_rules", columnType = "text", comment = "砍价规则")
    private String cutdownRules;

    /** 砍价范围，单位 分。值有 min 最小值;max 最大值 */
    @MpField(value = "cutdown_range", columnType = "json_array", comment = "砍价范围，单位 分。值有 min 最小值;max 最大值")
    private String cutdownRange;

    /** 砍价人数，单位(个)。值有 min 最小人数;max 最大人数 */
    @MpField(value = "people_range", columnType = "json_array", comment = "砍价人数，单位(个)。值有 min 最小人数;max 最大人数")
    private String peopleRange;

    /** 每个人最少能看的价钱,单位为‘分’ */
    @MpField(value = "min_price", columnType = "integer", comment = "每个人最少能看的价钱,单位为‘分’")
    private Integer minPrice = 0;

    /** 开始时间 */
    @MpField(value = "begin_time", columnType = "bigint", comment = "开始时间")
    private Long beginTime;

    /** 结束时间 */
    @MpField(value = "end_time", columnType = "bigint", comment = "结束时间")
    private Long endTime;

    /** 分享内容 */
    @MpField(value = "share_msg", columnType = "string", comment = "分享内容")
    private String shareMsg;

    /** 翻牌图片 */
    @MpField(value = "help_pics", columnType = "json_array", comment = "翻牌图片")
    private String helpPics;

    /** 商品id */
    @MpField(value = "item_id", columnType = "string", length = 11, comment = "商品id")
    private String itemId;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
