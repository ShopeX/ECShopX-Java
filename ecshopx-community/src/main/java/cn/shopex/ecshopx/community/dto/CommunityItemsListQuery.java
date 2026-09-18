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

package cn.shopex.ecshopx.community.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * 运营端社区商品列表查询上下文（Controller 组装后交给 Service）。
 */
@Value
@Builder
public class CommunityItemsListQuery {

	long companyId;
	String operatorType;
	int distributorId;
	/** Non-null non-empty: filter by multiple shops (IN); otherwise use {@link #distributorId}. */
	List<Integer> distributorIds;

	String keywords;
	String itemName;
	String itemBn;
	String barcode;
	String approveStatus;
	/** 非空时直接作为联表 {@code audit_status} 条件；运营端可不传。 */
	String auditStatus;
	String brandIdRaw;
	String categoryRaw;

	boolean inActivityParameterPresent;
	boolean inActivity;

	Long normalizedActivityId;

	int page;
	int pageSize;
}
