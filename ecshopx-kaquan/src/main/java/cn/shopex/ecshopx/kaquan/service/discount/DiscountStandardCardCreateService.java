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
import cn.shopex.ecshopx.common.kaquan.port.CouponAddEventDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class DiscountStandardCardCreateService {

	private final DiscountStandardCardSetParamsService setParamsService;
	private final DiscountStandardCardItemIdsService itemIdsService;
	private final DiscountCardsMapper discountCardsMapper;
	private final DiscountStandardCardRelItemsPersistenceService relItemsPersistenceService;
	private final DiscountCardsRowMapperService rowMapperService;
	private final DiscountCardsMultiLangWriteService multiLangWriteService;
	private final CouponAddEventDispatchPublisher couponAddEventDispatchPublisher;
	private final ObjectMapper objectMapper;

	public DiscountStandardCardCreateService(DiscountStandardCardSetParamsService setParamsService,
			DiscountStandardCardItemIdsService itemIdsService, DiscountCardsMapper discountCardsMapper,
			DiscountStandardCardRelItemsPersistenceService relItemsPersistenceService,
			DiscountCardsRowMapperService rowMapperService, DiscountCardsMultiLangWriteService multiLangWriteService,
			CouponAddEventDispatchPublisher couponAddEventDispatchPublisher, ObjectMapper objectMapper) {
		this.setParamsService = setParamsService;
		this.itemIdsService = itemIdsService;
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsPersistenceService = relItemsPersistenceService;
		this.rowMapperService = rowMapperService;
		this.multiLangWriteService = multiLangWriteService;
		this.couponAddEventDispatchPublisher = couponAddEventDispatchPublisher;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createKaquan(Map<String, Object> postdata, String authorizerAppid) {
		Map<String, Object> dataInfo = new HashMap<>(postdata);
		setParamsService.apply(dataInfo);
		itemIdsService.apply(dataInfo);
		DiscountCards entity = new DiscountCards();
		DiscountCardsColumnApplier.apply(entity, dataInfo, objectMapper);
		int rows = discountCardsMapper.insert(entity);
		if (rows <= 0 || entity.getCardId() == null || entity.getCardId() <= 0) {
			throw new ResourceException(KaquanDiscountCardMessages.DATA_CREATE_FAILED_RETRY);
		}
		long cardId = entity.getCardId();
		long companyId = entity.getCompanyId();
		relItemsPersistenceService.persist(dataInfo, cardId, companyId);
		Map<String, Object> langSource = new HashMap<>(postdata);
		multiLangWriteService.writeAfterCreate(cardId, companyId, langSource);
		final long publishCardId = cardId;
		final long publishCompanyId = companyId;
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				couponAddEventDispatchPublisher.publish(publishCardId, publishCompanyId);
			}
		});
		DiscountCards loaded = discountCardsMapper.selectById(cardId);
		if (loaded == null) {
			throw new ResourceException(KaquanDiscountCardMessages.DATA_CREATE_FAILED_RETRY);
		}
		return rowMapperService.toSnakeCaseMap(loaded);
	}
}
