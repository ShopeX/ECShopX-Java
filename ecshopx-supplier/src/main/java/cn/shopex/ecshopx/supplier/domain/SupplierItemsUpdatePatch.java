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

package cn.shopex.ecshopx.supplier.domain;

import lombok.Data;

/**
 * Conditional column set for bulk supplier_items updates; excludes entity-wide null defaults from ORM updates.
 */
@Data
public class SupplierItemsUpdatePatch {

	private Integer price;
	private Integer costPrice;
	private Integer marketPrice;
	private Integer store;
	private Integer rebate;
	private String rebateType;
	private String approveStatus;
	private Integer isMarket;
	private String auditStatus;

	private boolean marketColumnPresent;
	private Object isMarketValueRaw;
}
