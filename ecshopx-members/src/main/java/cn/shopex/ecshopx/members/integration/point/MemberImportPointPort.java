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

package cn.shopex.ecshopx.members.integration.point;

/**
 * Member Excel import point adjustment (aligned with PHP PointMemberService::addPoint journal_type=15).
 */
public interface MemberImportPointPort {

	/**
	 * @param point absolute points to change (must be &gt; 0)
	 * @param plus true to credit, false to debit
	 * @param record ledger description prefix
	 */
	void adjustImportPoint(long userId, long companyId, int point, boolean plus, String record);
}
