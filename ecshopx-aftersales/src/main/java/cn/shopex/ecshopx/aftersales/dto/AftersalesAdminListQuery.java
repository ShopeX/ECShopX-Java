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

package cn.shopex.ecshopx.aftersales.dto;

import java.util.List;
import lombok.Data;

/**
 * 运营端售后分页列表：Controller 解析后的查询参数。
 */
@Data
public class AftersalesAdminListQuery {

	private int page;
	private int pageSize;
	private boolean orderByCreateTimeAsc;
	/** 与下游 filter 中已写入 {@code is_prescription_order} 键同构（键存在即 true，含值为 "0"/"1"）。 */
	private boolean prescriptionOrderFilterActive;
	private String isPrescriptionOrderValue;

	private String timeStartBegin;
	private String timeStartEnd;
	private String aftersalesStatus;
	private String aftersalesType;
	private String aftersalesBn;
	private String itemId;
	private String itemBn;
	private String orderId;
	private String receiverMobile;
	private String shopId;
	private String userId;
	private String mobile;
	private String distributorId;
	private List<String> distributorIds;
	private String orderClass;
	private String itemName;
}
