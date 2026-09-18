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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 社区团购购物车；订单与社区拼团活动等关联信息。
 */
@Data
@MpTable("community_order_rel_activity")
public class CommunityOrderRelActivity {

    /** 订单号（非自增，业务赋值） */
    @MpId(value = "order_id", type = IdType.INPUT)
    private Long orderId;

    /** 公司id */
    @MpField("company_id")
    private Long companyId;

    /** 团长id */
    @MpField("chief_id")
    private Long chiefId;

    /** 团长名称 */
    @MpField("chief_name")
    private String chiefName;

    /** 团长头像 */
    @MpField("chief_avatar")
    private String chiefAvatar;

    /** 活动id */
    @MpField("activity_id")
    private Long activityId;

    /** 活动名称 */
    @MpField("activity_name")
    private String activityName;

    /** 自提点名称，可为空 */
    @MpField("ziti_name")
    private String zitiName;

    /** 具体地址，可为空，最长 500 */
    @MpField("ziti_address")
    private String zitiAddress;

    /** 地图纬度，可为空（列名 lng） */
    @MpField("lng")
    private String zitiLng;

    /** 地图经度，可为空（列名 lat） */
    @MpField("lat")
    private String zitiLat;

    /** 自提点联系人，可为空 */
    @MpField("ziti_contact_user")
    private String zitiContactUser;

    /** 自提点联系电话，可为空 */
    @MpField("ziti_contact_mobile")
    private String zitiContactMobile;

    /** 跟团号，可为空 */
    @MpField("activity_trade_no")
    private Integer activityTradeNo;

    /** 附加信息，可为空 */
    @MpField("extra_data")
    private String extraData;

    /** 创建时间（整型时间戳） */
    @MpField("created")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField("updated")
    private Integer updated;

    /** 佣金比例，最长 20，默认 0 */
    @MpField("rebate_ratio")
    private String rebateRatio = "0";
}
