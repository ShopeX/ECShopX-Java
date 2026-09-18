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

import lombok.Data;

@Data
public class RefundAdminListQuery {

	private String pageRaw;
	private String pageSizeRaw;
	private String mobile;
	private String refundBn;
	private String aftersalesBn;
	private String orderId;
	private String shopId;
	private String refundType;
	private boolean refundTypeParameterPresent;
	private String refundChannel;
	private String refundStatus;
	private String timeStartBegin;
	private String timeStartEnd;
	private String userId;
	private String distributorIdRaw;
}
