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

import cn.shopex.ecshopx.common.dispatch.KaquanDispatchEventNames;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.kaquan.port.CouponAddEventDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelMemberTagsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class DiscountNewGiftCardCreateService {

	public static final int STATUS_INIT = 10;

	private final DiscountCardsMapper discountCardsMapper;
	private final RelItemsMapper relItemsMapper;
	private final RelMemberTagsMapper relMemberTagsMapper;
	private final DiscountCardsRowMapperService rowMapperService;
	private final DiscountCardsMultiLangWriteService multiLangWriteService;
	private final CouponAddEventDispatchPublisher couponAddEventDispatchPublisher;
	private final ObjectMapper objectMapper;
	private final DiscountNewGiftCardSetParamsApplier newGiftSetParamsApplier;

	public DiscountNewGiftCardCreateService(DiscountCardsMapper discountCardsMapper, RelItemsMapper relItemsMapper,
			RelMemberTagsMapper relMemberTagsMapper, DiscountCardsRowMapperService rowMapperService,
			DiscountCardsMultiLangWriteService multiLangWriteService,
			CouponAddEventDispatchPublisher couponAddEventDispatchPublisher, ObjectMapper objectMapper,
			DiscountNewGiftCardSetParamsApplier newGiftSetParamsApplier) {
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsMapper = relItemsMapper;
		this.relMemberTagsMapper = relMemberTagsMapper;
		this.rowMapperService = rowMapperService;
		this.multiLangWriteService = multiLangWriteService;
		this.couponAddEventDispatchPublisher = couponAddEventDispatchPublisher;
		this.objectMapper = objectMapper;
		this.newGiftSetParamsApplier = newGiftSetParamsApplier;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createKaquan(Map<String, Object> data) {
		Map<String, Object> dataInfo = new HashMap<>(data);
		newGiftSetParamsApplier.apply(dataInfo, true);
		if (dataInfo.get("distributor_id") instanceof List<?> l && !l.isEmpty()) {
			dataInfo.put("use_all_shops", "false");
		} else {
			dataInfo.put("use_all_shops", "true");
		}
		dataInfo.put("kq_status", STATUS_INIT);
		DiscountCards entity = new DiscountCards();
		DiscountCardsColumnApplier.apply(entity, dataInfo, objectMapper);
		int rows = discountCardsMapper.insert(entity);
		if (rows <= 0 || entity.getCardId() == null || entity.getCardId() <= 0) {
			throw new ResourceException(KaquanDiscountCardMessages.DATA_CREATE_FAILED_RETRY);
		}
		long cardId = entity.getCardId();
		long companyId = entity.getCompanyId();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) dataInfo.get("items");
		if (items != null && !items.isEmpty()) {
			insertRelItemsChunks(items, cardId, companyId);
		}
		@SuppressWarnings("unchecked")
		List<Object> tagIds = (List<Object>) dataInfo.get("user_tag_ids");
		if (tagIds != null && !tagIds.isEmpty()) {
			List<KaquanRelMemberTagBatchRow> batch = new ArrayList<>();
			for (Object t : tagIds) {
				long tid = DiscountCardParamNormalize.longFromObject(t, 0L);
				KaquanRelMemberTagBatchRow row = new KaquanRelMemberTagBatchRow();
				row.setCompanyId(companyId);
				row.setCardId(cardId);
				row.setTagId(tid);
				batch.add(row);
			}
			if (!batch.isEmpty()) {
				relMemberTagsMapper.insertBatch(batch);
			}
		}
		Map<String, Object> langSource = new HashMap<>(data);
		multiLangWriteService.writeAfterCreate(cardId, companyId, langSource);
		final long publishCardId = cardId;
		final long publishCompanyId = companyId;
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				couponAddEventDispatchPublisher.publish(
						publishCardId, publishCompanyId, KaquanDispatchEventNames.EVENT_COUPON_ADD_NEW_GIFT);
			}
		});
		DiscountCards loaded = discountCardsMapper.selectById(cardId);
		if (loaded == null) {
			throw new ResourceException(KaquanDiscountCardMessages.DATA_CREATE_FAILED_RETRY);
		}
		return rowMapperService.toSnakeCaseMap(loaded);
	}

	private void insertRelItemsChunks(List<Map<String, Object>> items, long cardId, long companyId) {
		List<KaquanRelItemBatchRow> batch = new ArrayList<>();
		for (Map<String, Object> item : items) {
			long itemId = DiscountCardParamNormalize.longFromObject(item.get("id"), 0L);
			KaquanRelItemBatchRow row = new KaquanRelItemBatchRow();
			row.setItemId(itemId);
			row.setCardId(cardId);
			row.setCompanyId(companyId);
			row.setItemType("normal");
			row.setIsShow(1);
			int ul = 0;
			if (item.containsKey("limit")) {
				ul = DiscountCardParamNormalize.parseIntFlexible(item.get("limit"), 0);
			}
			row.setUseLimit(ul);
			batch.add(row);
			if (batch.size() >= 100) {
				relItemsMapper.insertBatch(batch);
				batch.clear();
			}
		}
		if (!batch.isEmpty()) {
			relItemsMapper.insertBatch(batch);
		}
	}
}
