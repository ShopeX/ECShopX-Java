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

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskUserProgress;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskUserProgressMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberTaskUserProgressService {

	private final MemberTaskUserProgressMapper memberTaskUserProgressMapper;

	public MemberTaskUserProgressService(MemberTaskUserProgressMapper memberTaskUserProgressMapper) {
		this.memberTaskUserProgressMapper = memberTaskUserProgressMapper;
	}

	public long getAmountFen(long taskId, long userId) {
		MemberTaskUserProgress row =
				memberTaskUserProgressMapper.selectOne(
						new QueryWrapper<MemberTaskUserProgress>()
								.eq("task_id", taskId)
								.eq("user_id", userId)
								.last("LIMIT 1"));
		if (row == null || row.getAmountFen() == null) {
			return 0L;
		}
		return row.getAmountFen();
	}

	@Transactional
	public long addAmountFen(long companyId, long taskId, long userId, long deltaFen, int nowSec) {
		if (deltaFen <= 0L) {
			return getAmountFen(taskId, userId);
		}
		MemberTaskUserProgress row =
				memberTaskUserProgressMapper.selectOne(
						new QueryWrapper<MemberTaskUserProgress>()
								.eq("task_id", taskId)
								.eq("user_id", userId)
								.last("LIMIT 1 FOR UPDATE"));
		if (row == null) {
			MemberTaskUserProgress created = new MemberTaskUserProgress();
			created.setCompanyId(companyId);
			created.setTaskId(taskId);
			created.setUserId(userId);
			created.setAmountFen(deltaFen);
			created.setUpdated(nowSec);
			try {
				memberTaskUserProgressMapper.insert(created);
				return deltaFen;
			} catch (DuplicateKeyException e) {
				row =
						memberTaskUserProgressMapper.selectOne(
								new QueryWrapper<MemberTaskUserProgress>()
										.eq("task_id", taskId)
										.eq("user_id", userId)
										.last("LIMIT 1 FOR UPDATE"));
			}
		}
		if (row == null) {
			return 0L;
		}
		long next = Math.max(0L, (row.getAmountFen() == null ? 0L : row.getAmountFen()) + deltaFen);
		memberTaskUserProgressMapper.update(
				null,
				new UpdateWrapper<MemberTaskUserProgress>()
						.eq("id", row.getId())
						.set("amount_fen", next)
						.set("updated", nowSec));
		return next;
	}

	@Transactional
	public long deductAmountFen(long taskId, long userId, long deltaFen, int nowSec) {
		if (deltaFen <= 0L) {
			return getAmountFen(taskId, userId);
		}
		MemberTaskUserProgress row =
				memberTaskUserProgressMapper.selectOne(
						new QueryWrapper<MemberTaskUserProgress>()
								.eq("task_id", taskId)
								.eq("user_id", userId)
								.last("LIMIT 1 FOR UPDATE"));
		if (row == null) {
			return 0L;
		}
		long current = row.getAmountFen() == null ? 0L : row.getAmountFen();
		long next = Math.max(0L, current - deltaFen);
		memberTaskUserProgressMapper.update(
				null,
				new UpdateWrapper<MemberTaskUserProgress>()
						.eq("id", row.getId())
						.set("amount_fen", next)
						.set("updated", nowSec));
		return next;
	}
}
