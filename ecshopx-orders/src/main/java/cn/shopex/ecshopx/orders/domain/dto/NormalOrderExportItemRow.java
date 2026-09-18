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

package cn.shopex.ecshopx.orders.domain.dto;

import lombok.Data;

@Data
public class NormalOrderExportItemRow {

	private Long itemRowId;
	private Long orderId;
	private Long itemId;
	private String itemName;
	private String itemBn;
	private Integer price;
	private Integer costPrice;
	private Integer num;
	private Integer itemFee;
	private Integer commissionFee;
	private Integer costFee;
	private Integer itemPointFee;
	private Integer itemTotalFee;
	private Integer memberDiscount;
	private Integer couponDiscount;
	private Integer itemDiscountFee;
	private String discountInfo;
	private String deliveryStatus;
	private Integer itemDeliveryTime;
	private String itemDeliveryCode;
	private String itemDeliveryCorp;
	private String aftersalesStatus;
	private String itemSpecDesc;
	private Integer itemSupplierId;

	private String mobile;
	private Long userId;
	private Integer orderCreateTime;
	private Integer orderTotalFee;
	private Integer orderFreightFee;
	private Long distributorId;
	private String orderClass;
	private String orderStatus;
	private String payType;
	private String payStatus;
	private String receiptType;
	private String zitiStatus;
	private String receiverName;
	private String receiverMobile;
	private String receiverZip;
	private String receiverState;
	private String receiverCity;
	private String receiverDistrict;
	private String receiverAddress;
	private String remark;
	private Integer endTime;
	private String orderDeliveryStatus;
	private Integer orderDeliveryTime;
	private String orderDeliveryCorp;
	private String orderDeliveryCode;
	private String cancelStatus;
	private String prescriptionStatus;
}
