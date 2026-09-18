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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.domain.PromotionsItemsTag;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionGroupsActivityFinishWritesService {

	private final MessageSource messageSource;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final PromotionsItemsTagMapper promotionsItemsTagMapper;
	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;
	private final PromotionGroupsActivityAdminRowAssembler adminRowAssembler;
	private final PromotionGroupsRelGoodsWriteService promotionGroupsRelGoodsWriteService;

	public PromotionGroupsActivityFinishWritesService(
			MessageSource messageSource,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			PromotionsItemsTagMapper promotionsItemsTagMapper,
			PromotionGroupsTeamMapper promotionGroupsTeamMapper,
			PromotionGroupsActivityAdminRowAssembler adminRowAssembler,
			PromotionGroupsRelGoodsWriteService promotionGroupsRelGoodsWriteService) {
		this.messageSource = messageSource;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.promotionsItemsTagMapper = promotionsItemsTagMapper;
		this.promotionGroupsTeamMapper = promotionGroupsTeamMapper;
		this.adminRowAssembler = adminRowAssembler;
		this.promotionGroupsRelGoodsWriteService = promotionGroupsRelGoodsWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> applyFinishWrites(long companyId, long groupsActivityId, Locale locale) {
		PromotionGroupsActivity entity =
				promotionGroupsActivityMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsActivity>()
								.eq(PromotionGroupsActivity::getGroupsActivityId, groupsActivityId)
								.eq(PromotionGroupsActivity::getCompanyId, companyId));
		if (entity == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}
		int now = (int) Instant.now().getEpochSecond();
		int end = now - 1;
		entity.setEndTime((long) end);
		entity.setUpdated(now);
		int updatedRows = promotionGroupsActivityMapper.updateById(entity);
		if (updatedRows == 0) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}
		Map<String, Object> payload = adminRowAssembler.toRow(entity, now);
		boolean truthy = payload != null && !payload.isEmpty();
		if (truthy) {
			promotionsItemsTagMapper.delete(
					new LambdaQueryWrapper<PromotionsItemsTag>()
							.eq(PromotionsItemsTag::getPromotionId, groupsActivityId)
							.eq(PromotionsItemsTag::getCompanyId, companyId)
							.eq(PromotionsItemsTag::getTagType, "single_group"));
		}
		promotionGroupsTeamMapper.update(
				null,
				new LambdaUpdateWrapper<PromotionGroupsTeam>()
						.eq(PromotionGroupsTeam::getActId, groupsActivityId)
						.eq(PromotionGroupsTeam::getCompanyId, companyId)
						.set(PromotionGroupsTeam::getEndTime, (long) end)
						.set(PromotionGroupsTeam::getUpdated, now));
		promotionGroupsRelGoodsWriteService.cleanupRelRowsAndRedisKeys(companyId, groupsActivityId);
		return payload;
	}
}
