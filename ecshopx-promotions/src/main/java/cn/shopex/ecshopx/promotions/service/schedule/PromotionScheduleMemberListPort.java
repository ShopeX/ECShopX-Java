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

package cn.shopex.ecshopx.promotions.service.schedule;

import java.util.List;
import java.util.Map;

/** 与 PHP {@code MemberService::getList} 在计划活动场景下的统计与分页列表对齐。 */
public interface PromotionScheduleMemberListPort {

	long countMembers(long companyId, MemberFilterSpec spec);

	/**
	 * @param page 1-based
	 */
	List<Map<String, Object>> getMembers(long companyId, MemberFilterSpec spec, int pageSize, int page);
}
