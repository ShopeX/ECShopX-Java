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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;

/** H3：同一会员同一首单任务仅允许一条 voided=0 的 completion。 */
@Component
public class MemberTaskFirstOrderCompletionPolicy {

	private final MemberTaskCompletionMapper memberTaskCompletionMapper;

	public MemberTaskFirstOrderCompletionPolicy(MemberTaskCompletionMapper memberTaskCompletionMapper) {
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
	}

	public boolean hasActiveFirstOrderCompletion(long taskId, long userId) {
		Long n =
				memberTaskCompletionMapper.selectCount(
						new QueryWrapper<MemberTaskCompletion>()
								.eq("task_id", taskId)
								.eq("user_id", userId)
								.eq("voided", 0));
		return n != null && n > 0L;
	}
}
