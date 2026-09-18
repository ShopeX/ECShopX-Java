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

package cn.shopex.ecshopx.community.dto.export;

import lombok.Data;

/** 团购普通订单导出行，与 {@code CommunityNormalOrderExportMapper} 查询列一致。 */
@Data
public class CommunityNormalOrderExportRow {

	private Long orderId;
	private Long userId;
	private String remark;
	private String receiptType;
	private String receiverName;
	private String receiverMobile;
	private String receiverState;
	private String receiverCity;
	private String receiverDistrict;
	private String receiverAddress;
	private String orderStatus;
	private String deliveryStatus;
	private String zitiStatus;
	private String cancelStatus;
	private Integer createTime;
	private String itemName;
	private String itemSpecDesc;
	private Integer num;
	private Integer itemFee;
	private Integer discountFee;
	private Integer totalFee;
	private Integer price;
	private Long itemId;
	private String itemBn;
	private String activityTradeNo;
	private String zitiName;
	private String zitiContactUser;
	private String zitiContactMobile;
	private String zitiAddress;
	private String chiefName;
	private String extraData;
	private String username;
	private String chiefMobile;
}
