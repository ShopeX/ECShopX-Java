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

package cn.shopex.ecshopx.common.kaquan.port;

import java.util.List;
import java.util.Map;

public interface OpenapiMemberListKaquanLookupPort {

	/** 对齐 MemberCardService::getGradeInfo；无匹配返回 0L（仍写入 filter.grade_id）。 */
	long resolveGradeIdByName(long companyId, String gradeName);

	/** 对齐 MemberCardVipGradeService::find(is_disabled=1)；无匹配返回 0L。 */
	long resolveVipGradeIdByName(long companyId, String vipGradeName);

	/**
	 * 对齐 appendVipGradeToList：userIds → Map&lt;userId, List&lt;{id,type,end_date,grade_nme}&gt;&gt;。
	 * 过滤 end_date &lt; now()；grade_nme 保留拼写。
	 */
	Map<Long, List<Map<String, Object>>> loadActiveVipGradesByUserIds(long companyId, List<Long> userIds);
}
