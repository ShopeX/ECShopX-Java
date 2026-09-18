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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopSalespersonWorkWechatWxappLoginMutationService {

	private final ShopSalespersonMapper shopSalespersonMapper;

	public ShopSalespersonWorkWechatWxappLoginMutationService(ShopSalespersonMapper shopSalespersonMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
	}

	public Optional<ShopSalesperson> findFirstByWorkUseridOrWorkClearUserid(String userid) {
		if (!StringUtils.hasText(userid)) {
			return Optional.empty();
		}
		ShopSalesperson row = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.and(w -> w.eq(ShopSalesperson::getWorkUserid, userid).or().eq(ShopSalesperson::getWorkClearUserid, userid))
				.select(ShopSalesperson::getSalespersonId, ShopSalesperson::getWorkUserid, ShopSalesperson::getWorkClearUserid)
				.last("LIMIT 1"));
		return Optional.ofNullable(row);
	}

	public void updateWorkClearUseridBySalespersonId(long salespersonId, String plainUserid) {
		long now = Instant.now().getEpochSecond();
		LambdaUpdateWrapper<ShopSalesperson> uw = new LambdaUpdateWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.set(ShopSalesperson::getWorkClearUserid, plainUserid)
				.set(ShopSalesperson::getUpdated, now);
		int rows = shopSalespersonMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	public Optional<Long> findSalespersonIdByWorkUseridEqOpenUserid(String openUseridFromBa) {
		if (!StringUtils.hasText(openUseridFromBa)) {
			return Optional.empty();
		}
		ShopSalesperson row = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getWorkUserid, openUseridFromBa)
				.select(ShopSalesperson::getSalespersonId)
				.last("LIMIT 1"));
		return row == null || row.getSalespersonId() == null ? Optional.empty() : Optional.of(row.getSalespersonId());
	}

	public void updateWorkConfigIdsBySalespersonId(long salespersonId, String workConfigid, String workQrcodeConfigid) {
		boolean cfg = workConfigid != null && !workConfigid.trim().isEmpty() && !"0".equals(workConfigid.trim());
		boolean qr = workQrcodeConfigid != null && !workQrcodeConfigid.trim().isEmpty()
				&& !"0".equals(workQrcodeConfigid.trim());
		if (!cfg && !qr) {
			return;
		}
		long now = Instant.now().getEpochSecond();
		LambdaUpdateWrapper<ShopSalesperson> uw = new LambdaUpdateWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.set(ShopSalesperson::getUpdated, now);
		if (cfg) {
			uw.set(ShopSalesperson::getWorkConfigid, workConfigid);
		}
		if (qr) {
			uw.set(ShopSalesperson::getWorkQrcodeConfigid, workQrcodeConfigid);
		}
		int rows = shopSalespersonMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}
}
