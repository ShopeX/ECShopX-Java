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

package cn.shopex.ecshopx.common.members.port;

import java.util.Map;

public interface WxappMemberPointRuleAndBalancePort {

	Map<String, Object> loadRule(long companyId);

	Map<String, Object> loadPointMemberInfo(long companyId, long userId);

	/**
	 * Resolves the point balance for wxapp member info: starts from the domain member point row, then
	 * when CRM point integration is on and {@code mobilePlainForOuterScope} is non-blank, overlays integral
	 * and frozen balances from CRM. The returned map always contains {@code point} (numeric); it contains
	 * {@code frozen_point} only when a CRM overlay row was applied.
	 */
	Map<String, Object> loadWxappPointDisplay(long companyId, long userId, String mobilePlainForOuterScope);
}
