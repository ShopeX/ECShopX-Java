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

package cn.shopex.ecshopx.common.operatorcart.dto;

import lombok.Data;

/** 运营购物车单行 SKU 快照（分、库存等） */
@Data
public class OperatorCartSkuRowDto {

	private long itemId;
	private long defaultItemId;
	private String itemName;
	/** 销售单价，分 */
	private int unitPriceFen;
	private int store;
	private boolean saleDisabled;
	private String itemCategory;
	private Integer brandId;
	/** drug / normal 等，与 items.special_type 及购物车落库一致 */
	private String specialType;

	/** items.type：0 普通，1 跨境等 */
	private Integer goodsType;
	private String itemType;
	/** items.approve_status 快照（Handle 前通常为 onsale） */
	private String itemApproveStatus;
	private Boolean isGift;
	/** 原价，分 */
	private Integer marketPriceFen;
	private String brief;
	private String pics;
	private String crossborderTaxRate;
	private Long taxstrategyId;
	private Integer taxationNum;
	private Long origincountryId;
	private Integer isMedicine;
	private Integer isPrescription;
	private Integer startNum;
	private Long goodsId;
}
