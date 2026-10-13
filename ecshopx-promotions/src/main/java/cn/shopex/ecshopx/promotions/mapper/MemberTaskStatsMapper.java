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

package cn.shopex.ecshopx.promotions.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberTaskStatsMapper {

	long countCompletions(@Param("companyId") long companyId, @Param("taskId") long taskId);

	long countCompletionUsers(@Param("companyId") long companyId, @Param("taskId") long taskId);

	List<Map<String, Object>> countGrantsByStatus(
			@Param("companyId") long companyId, @Param("taskId") long taskId);

	/** 订单列 {@code member_task_id} 归因且已支付金额（分，{@code total_fee} 之和）。 */
	long sumAttributedOrderSpendFen(
			@Param("companyId") long companyId, @Param("taskId") long taskId);

	/** 邀请注册等：completion 上 {@code invitee_user_id} 去重人数。 */
	long countAttributedJoinUsers(
			@Param("companyId") long companyId, @Param("taskId") long taskId);
}
