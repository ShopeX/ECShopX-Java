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

import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class PromotionGroupsTeamAutoCancelService {

	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;
	private final PromotionGroupsTeamFailService promotionGroupsTeamFailService;

	public PromotionGroupsTeamAutoCancelService(
			PromotionGroupsTeamMapper promotionGroupsTeamMapper,
			PromotionGroupsTeamFailService promotionGroupsTeamFailService) {
		this.promotionGroupsTeamMapper = promotionGroupsTeamMapper;
		this.promotionGroupsTeamFailService = promotionGroupsTeamFailService;
	}

	public int scheduleAutoCancelGroupOrders() {
		int pageSize = 100;
		int now = (int) Instant.now().getEpochSecond();
		LambdaQueryWrapper<PromotionGroupsTeam> base =
				new LambdaQueryWrapper<PromotionGroupsTeam>()
						.eq(PromotionGroupsTeam::getTeamStatus, 1L)
						.le(PromotionGroupsTeam::getEndTime, (long) now);
		long totalCount = promotionGroupsTeamMapper.selectCount(base);
		if (totalCount == 0L) {
			return 0;
		}
		int processed = 0;
		int totalPage = (int) Math.ceil(totalCount / (double) pageSize);
		for (int i = 1; i <= totalPage; i++) {
			Page<PromotionGroupsTeam> p = new Page<>(1, pageSize, false);
			LambdaQueryWrapper<PromotionGroupsTeam> wrapper =
					new LambdaQueryWrapper<PromotionGroupsTeam>()
							.eq(PromotionGroupsTeam::getTeamStatus, 1L)
							.le(PromotionGroupsTeam::getEndTime, (long) now)
							.orderByDesc(PromotionGroupsTeam::getCreated);
			Page<PromotionGroupsTeam> page = promotionGroupsTeamMapper.selectPage(p, wrapper);
			for (PromotionGroupsTeam row : page.getRecords()) {
				promotionGroupsTeamFailService.teamFail(row.getTeamId());
				processed++;
			}
		}
		return processed;
	}
}
