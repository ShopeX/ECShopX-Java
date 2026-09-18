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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.TradeRate;
import cn.shopex.ecshopx.orders.mapper.TradeRateMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeRateAdminDeleteService {

	private final TradeRateMapper tradeRateMapper;

	public TradeRateAdminDeleteService(TradeRateMapper tradeRateMapper) {
		this.tradeRateMapper = tradeRateMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void tradeRateDelete(String rateId) {
		QueryWrapper<TradeRate> q = new QueryWrapper<>();
		q.eq("rate_id", rateId);
		TradeRate existing = tradeRateMapper.selectOne(q);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}

		QueryWrapper<TradeRate> uw = new QueryWrapper<>();
		uw.eq("rate_id", rateId);
		TradeRate patch = new TradeRate();
		patch.setDisabled(true);
		int updated = tradeRateMapper.update(patch, uw);
		if (updated == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}
}
