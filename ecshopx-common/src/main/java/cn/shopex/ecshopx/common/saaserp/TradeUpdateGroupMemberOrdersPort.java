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

package cn.shopex.ecshopx.common.saaserp;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Supplies paid team-order rows for Saas ERP trade sync on group orders, decoupled from the third-party module to
 * avoid a Maven cycle with the promotions domain.
 */
public interface TradeUpdateGroupMemberOrdersPort {

	List<LinkedHashMap<String, Object>> listPaidTeamOrderRowsForLeader(
			long companyId, String leaderOrderId, long memberId);
}
