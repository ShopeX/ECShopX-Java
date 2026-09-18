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

package cn.shopex.ecshopx.distribution.port;

import java.util.Map;

/**
 * Orchestrates in-rule check for H5 distributor flows without creating a Maven cycle between distribution and
 * salesperson modules.
 */
public interface DistributionCheckInRulePort {

	Map<String, Object> checkInRule(long companyId, long userId, String workUserid);
}
