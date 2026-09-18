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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.promotions.domain.TurntableUserCount;
import cn.shopex.ecshopx.promotions.domain.TurntableUserDayCount;
import cn.shopex.ecshopx.promotions.mapper.TurntableUserCountMapper;
import cn.shopex.ecshopx.promotions.mapper.TurntableUserDayCountMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 抽奖次数 DB CAS：总表 + 日表（PRD §6.3）。
 */
@Service
public class TurntableCountReserveService {

	private final TurntableUserCountMapper userCountMapper;
	private final TurntableUserDayCountMapper userDayCountMapper;

	public TurntableCountReserveService(
			TurntableUserCountMapper userCountMapper, TurntableUserDayCountMapper userDayCountMapper) {
		this.userCountMapper = userCountMapper;
		this.userDayCountMapper = userDayCountMapper;
	}

	public record ReserveResult(boolean ok, boolean totalFailed, boolean dayFailed) {
		public static ReserveResult success() {
			return new ReserveResult(true, false, false);
		}

		public static ReserveResult totalLimit() {
			return new ReserveResult(false, true, false);
		}

		public static ReserveResult dayLimit() {
			return new ReserveResult(false, false, true);
		}
	}

	@Transactional
	public ReserveResult reserve(
			long companyId, long userId, long actId, String dayKey, long limitTotal, long limitDay) {
		int now = (int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE);
		if (!reserveTotal(companyId, userId, actId, limitTotal, now)) {
			return ReserveResult.totalLimit();
		}
		if (!reserveDay(companyId, userId, actId, dayKey, limitDay, now)) {
			releaseTotal(companyId, userId, actId, now);
			return ReserveResult.dayLimit();
		}
		return ReserveResult.success();
	}

	@Transactional
	public void release(long companyId, long userId, long actId, String dayKey) {
		int now = (int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE);
		releaseDay(companyId, userId, actId, dayKey, now);
		releaseTotal(companyId, userId, actId, now);
	}

	private boolean reserveTotal(long companyId, long userId, long actId, long limitTotal, int now) {
		int rows = userCountMapper.casIncrTotal(companyId, userId, actId, limitTotal, now);
		if (rows > 0) {
			return true;
		}
		TurntableUserCount row = new TurntableUserCount();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setActId(actId);
		row.setTotalCount(1L);
		row.setCreated(now);
		row.setUpdated(now);
		try {
			userCountMapper.insert(row);
			if (limitTotal > 0L && 1L > limitTotal) {
				releaseTotal(companyId, userId, actId, now);
				return false;
			}
			return true;
		} catch (DuplicateKeyException e) {
			return userCountMapper.casIncrTotal(companyId, userId, actId, limitTotal, now) > 0;
		}
	}

	private boolean reserveDay(
			long companyId, long userId, long actId, String dayKey, long limitDay, int now) {
		int rows = userDayCountMapper.casIncrDay(companyId, userId, actId, dayKey, limitDay, now);
		if (rows > 0) {
			return true;
		}
		TurntableUserDayCount row = new TurntableUserDayCount();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setActId(actId);
		row.setDayKey(dayKey);
		row.setDayCount(1L);
		row.setCreated(now);
		row.setUpdated(now);
		try {
			userDayCountMapper.insert(row);
			if (limitDay > 0L && 1L > limitDay) {
				releaseDay(companyId, userId, actId, dayKey, now);
				return false;
			}
			return true;
		} catch (DuplicateKeyException e) {
			return userDayCountMapper.casIncrDay(companyId, userId, actId, dayKey, limitDay, now) > 0;
		}
	}

	private void releaseTotal(long companyId, long userId, long actId, int now) {
		userCountMapper.casDecrTotal(companyId, userId, actId, now);
	}

	private void releaseDay(long companyId, long userId, long actId, String dayKey, int now) {
		userDayCountMapper.casDecrDay(companyId, userId, actId, dayKey, now);
	}
}
