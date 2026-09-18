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

package cn.shopex.ecshopx.members.integration.kaquan;

import java.util.List;
import java.util.Map;

public interface AdminMemberListVipGradeRowGetPort {

	Map<String, Object> userVipGradeGet(long companyId, long userId, boolean ifAll);

	/**
	 * For each list row, loads paid-card state via {@link #userVipGradeGet(long, long, boolean)} with
	 * {@code ifAll=false} and sets row {@code vip_grade} to the paid-type label or {@code null}.
	 */
	void mergeVipGradeLabelsIntoRows(long companyId, List<Map<String, Object>> memberListRows);
}
