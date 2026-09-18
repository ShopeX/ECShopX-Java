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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@MpTable(value = "community_order_rel_activity", comment = "社区团购购物车")
public class CommunityOrderRelActivity {

	@MpId(value = "order_id", type = IdType.INPUT, columnType = "bigint", length = 64, comment = "订单号")
	private Long orderId;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司id")
	private Long companyId;

	@MpField(value = "chief_id", columnType = "bigint", comment = "团长id")
	private Long chiefId;

	@MpField(value = "chief_name", columnType = "string", comment = "团长名称")
	private String chiefName;

	@MpField(value = "chief_avatar", columnType = "string", comment = "团长头像")
	private String chiefAvatar;

	@MpField(value = "activity_id", columnType = "bigint", comment = "活动id")
	private Long activityId;

	@MpField(value = "activity_name", columnType = "string", comment = "活动名称")
	private String activityName;

	@MpField(value = "ziti_name", columnType = "string", nullable = true, comment = "自提点名称")
	private String zitiName;

	@MpField(value = "ziti_address", columnType = "string", length = 500, nullable = true, comment = "具体地址")
	private String zitiAddress;

	@MpField(value = "lng", columnType = "string", nullable = true, comment = "地图纬度")
	private String zitiLng;

	@MpField(value = "lat", columnType = "string", nullable = true, comment = "地图经度")
	private String zitiLat;

	@MpField(value = "ziti_contact_user", columnType = "string", nullable = true, comment = "自提点联系人")
	private String zitiContactUser;

	@MpField(value = "ziti_contact_mobile", columnType = "string", nullable = true, comment = "自提点联系电话")
	private String zitiContactMobile;

	@MpField(value = "activity_trade_no", columnType = "integer", nullable = true, comment = "跟团号")
	private Integer activityTradeNo;

	@MpField(value = "extra_data", columnType = "text", nullable = true, comment = "附加信息")
	private String extraData;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;

	@MpField(value = "rebate_ratio", columnType = "string", length = 20, comment = "佣金比例", defaultValue = "0")
	private String rebateRatio = "0";
}
