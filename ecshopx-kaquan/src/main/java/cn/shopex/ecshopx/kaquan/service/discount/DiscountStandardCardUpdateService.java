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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.kaquan.port.CouponEditEventDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.RelItems;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class DiscountStandardCardUpdateService {

	private final DiscountStandardCardSetParamsService setParamsService;
	private final DiscountStandardCardItemIdsService itemIdsService;
	private final DiscountCardsMapper discountCardsMapper;
	private final DiscountStandardCardRelItemsPersistenceService relItemsPersistenceService;
	private final DiscountCardsRowMapperService rowMapperService;
	private final DiscountCardsMultiLangWriteService multiLangWriteService;
	private final CouponEditEventDispatchPublisher couponEditEventDispatchPublisher;
	private final ObjectMapper objectMapper;
	private final RelItemsMapper relItemsMapper;

	public DiscountStandardCardUpdateService(DiscountStandardCardSetParamsService setParamsService,
			DiscountStandardCardItemIdsService itemIdsService, DiscountCardsMapper discountCardsMapper,
			DiscountStandardCardRelItemsPersistenceService relItemsPersistenceService,
			DiscountCardsRowMapperService rowMapperService, DiscountCardsMultiLangWriteService multiLangWriteService,
			CouponEditEventDispatchPublisher couponEditEventDispatchPublisher, ObjectMapper objectMapper,
			RelItemsMapper relItemsMapper) {
		this.setParamsService = setParamsService;
		this.itemIdsService = itemIdsService;
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsPersistenceService = relItemsPersistenceService;
		this.rowMapperService = rowMapperService;
		this.multiLangWriteService = multiLangWriteService;
		this.couponEditEventDispatchPublisher = couponEditEventDispatchPublisher;
		this.objectMapper = objectMapper;
		this.relItemsMapper = relItemsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	@SuppressWarnings("unused")
	public Map<String, Object> updateKaquan(Map<String, Object> postdata, String authorizerAppid, DiscountCards existing) {
		Map<String, Object> langSource = new HashMap<>(postdata);
		Map<String, Object> dataInfo = new HashMap<>(postdata);
		setParamsService.apply(dataInfo);
		itemIdsService.apply(dataInfo);
		DiscountCardsColumnApplier.applyForUpdate(existing, dataInfo, objectMapper);
		int rows = discountCardsMapper.updateById(existing);
		if (rows <= 0) {
			throw new ResourceException(KaquanDiscountCardMessages.DATA_UPDATE_FAILED_RETRY);
		}
		long cardId = existing.getCardId();
		long companyId = existing.getCompanyId();
		relItemsMapper.delete(new LambdaQueryWrapper<RelItems>()
				.eq(RelItems::getCardId, cardId)
				.eq(RelItems::getCompanyId, companyId));
		relItemsPersistenceService.persist(dataInfo, cardId, companyId);
		multiLangWriteService.writeAfterUpdate(cardId, companyId, langSource);
		final long publishCardId = cardId;
		final long publishCompanyId = companyId;
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				couponEditEventDispatchPublisher.publish(publishCardId, publishCompanyId);
			}
		});
		DiscountCards loaded = discountCardsMapper.selectById(cardId);
		if (loaded == null) {
			throw new ResourceException(KaquanDiscountCardMessages.NO_UPDATE_DATA_FOUND);
		}
		return rowMapperService.toSnakeCaseMap(loaded);
	}
}
