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

package cn.shopex.ecshopx.promotions.domain.membertask;

import java.util.Set;

public final class MemberTaskPurchaseTypes {

	private static final Set<String> PURCHASE_RULE_DETAILS =
			Set.of("first_order", "single_spend", "total_spend", "share_order");

	private MemberTaskPurchaseTypes() {}

	public static boolean usesPurchaseRule(String typeDetail) {
		return typeDetail != null && PURCHASE_RULE_DETAILS.contains(typeDetail);
	}
}
