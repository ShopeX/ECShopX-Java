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

import cn.shopex.ecshopx.promotions.domain.TurntablePrizeDayStock;
import cn.shopex.ecshopx.promotions.mapper.TurntablePrizeDayStockMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 抽奖日库存 reserve / release（PRD §6.3）。 */
@Service
public class TurntablePrizeDayStockReserveService {

	private final TurntablePrizeDayStockMapper prizeDayStockMapper;

	public TurntablePrizeDayStockReserveService(TurntablePrizeDayStockMapper prizeDayStockMapper) {
		this.prizeDayStockMapper = prizeDayStockMapper;
	}

	/** dailyStock==0 → 永远失败（今日不可中）。 */
	@Transactional
	public boolean reserve(long actId, String prizeId, String dayKey, int dailyStock) {
		if (!StringUtils.hasText(prizeId) || !StringUtils.hasText(dayKey)) {
			return false;
		}
		if (dailyStock <= 0) {
			return false;
		}
		int now = (int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE);
		int rows = prizeDayStockMapper.casReserve(actId, prizeId, dayKey, dailyStock, now);
		if (rows > 0) {
			return true;
		}
		TurntablePrizeDayStock row = new TurntablePrizeDayStock();
		row.setActId(actId);
		row.setPrizeId(prizeId);
		row.setDayKey(dayKey);
		row.setReservedCount(1L);
		row.setCreated(now);
		row.setUpdated(now);
		try {
			prizeDayStockMapper.insert(row);
			return true;
		} catch (DuplicateKeyException e) {
			return prizeDayStockMapper.casReserve(actId, prizeId, dayKey, dailyStock, now) > 0;
		}
	}

	@Transactional
	public void release(long actId, String prizeId, String dayKey) {
		if (!StringUtils.hasText(prizeId) || !StringUtils.hasText(dayKey)) {
			return;
		}
		int now = (int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE);
		prizeDayStockMapper.casRelease(actId, prizeId, dayKey, now);
	}
}
