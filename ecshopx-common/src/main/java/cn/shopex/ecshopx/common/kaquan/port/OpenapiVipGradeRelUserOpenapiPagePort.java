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

public interface OpenapiVipGradeRelUserOpenapiPagePort {

	record PageResult(long totalCount, List<Long> userIds) {}

	/**
	 * 条件：company_id + vip_grade_id；不过滤 end_date；ORDER BY user_id DESC；
	 * totalCount 为 rel 行数（非 distinct user_id）；同一 user 多行时 list 可重复 user_id。
	 */
	PageResult listUserIdsByVipGrade(long companyId, long vipGradeId, int page, int pageSize);
}
