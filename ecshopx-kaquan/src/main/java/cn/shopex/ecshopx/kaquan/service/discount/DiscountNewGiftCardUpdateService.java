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
import cn.shopex.ecshopx.common.kaquan.port.CouponEditEventDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.RelItems;
import cn.shopex.ecshopx.kaquan.domain.RelMemberTags;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelMemberTagsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class DiscountNewGiftCardUpdateService {

	public static final int STATUS_NORMAL = 0;
	public static final int STATUS_CLOSE = 2;
	public static final int STATUS_INIT = 10;

	private final DiscountCardsMapper discountCardsMapper;
	private final RelItemsMapper relItemsMapper;
	private final RelMemberTagsMapper relMemberTagsMapper;
	private final UserDiscountMapper userDiscountMapper;
	private final DiscountCardsRowMapperService rowMapperService;
	private final DiscountCardsMultiLangWriteService multiLangWriteService;
	private final CouponEditEventDispatchPublisher couponEditEventDispatchPublisher;
	private final DiscountNewGiftCardSetParamsApplier newGiftSetParamsApplier;
	private final ObjectMapper objectMapper;

	public DiscountNewGiftCardUpdateService(DiscountCardsMapper discountCardsMapper, RelItemsMapper relItemsMapper,
			RelMemberTagsMapper relMemberTagsMapper, UserDiscountMapper userDiscountMapper,
			DiscountCardsRowMapperService rowMapperService, DiscountCardsMultiLangWriteService multiLangWriteService,
			CouponEditEventDispatchPublisher couponEditEventDispatchPublisher,
			DiscountNewGiftCardSetParamsApplier newGiftSetParamsApplier, ObjectMapper objectMapper) {
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsMapper = relItemsMapper;
		this.relMemberTagsMapper = relMemberTagsMapper;
		this.userDiscountMapper = userDiscountMapper;
		this.rowMapperService = rowMapperService;
		this.multiLangWriteService = multiLangWriteService;
		this.couponEditEventDispatchPublisher = couponEditEventDispatchPublisher;
		this.newGiftSetParamsApplier = newGiftSetParamsApplier;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateKaquan(Map<String, Object> data) {
		boolean requestedItems = data.containsKey("items");
		Map<String, Object> merged = new HashMap<>(data);
		newGiftSetParamsApplier.apply(merged, false);
		long companyId = DiscountCardParamNormalize.longFromObject(merged.get("company_id"), 0L);
		long cardId = DiscountCardParamNormalize.longFromObject(merged.get("card_id"), 0L);
		if (companyId <= 0 || cardId <= 0) {
			throw new ResourceException(KaquanDiscountCardMessages.COUPON_INVALID);
		}
		DiscountCards existing = discountCardsMapper.selectOne(new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCardId, cardId)
				.eq(DiscountCards::getCompanyId, companyId));
		if (existing == null) {
			throw new ResourceException(KaquanDiscountCardMessages.COUPON_INVALID);
		}
		if (!"new_gift".equals(DiscountCardParamNormalize.stringVal(existing.getCardType()))) {
			throw new ResourceException(KaquanDiscountCardMessages.CARD_TYPE_ERROR);
		}
		if (merged.containsKey("kq_status")) {
			int oldSt = existing.getKqStatus() != null ? existing.getKqStatus() : 0;
			int newSt = DiscountCardParamNormalize.parseIntFlexible(merged.get("kq_status"), oldSt);
			if (oldSt == STATUS_CLOSE && newSt != STATUS_CLOSE) {
				throw new ResourceException(KaquanDiscountCardMessages.PAUSED_CARD_CANNOT_REACTIVATE);
			}
		}
		if (merged.containsKey("quantity")) {
			long receiveNum = userDiscountMapper.selectCount(new LambdaQueryWrapper<UserDiscount>()
					.eq(UserDiscount::getCardId, cardId)
					.eq(UserDiscount::getCompanyId, companyId));
			int oldQty = existing.getQuantity() != null ? existing.getQuantity() : 0;
			int usableNum = oldQty - (int) receiveNum;
			int newQty = Math.abs(DiscountCardParamNormalize.parseIntFlexible(merged.get("quantity"), 0));
			if (newQty < usableNum) {
				throw new ResourceException(
						String.format(KaquanDiscountCardMessages.REDUCE_QUANTITY_CANNOT_LESS_THAN_REMAINING, usableNum));
			}
			merged.put("quantity", newQty);
		}
		validateMemberScopeExpandOnlyAfterSetParams(existing, merged);
		long nowSec = System.currentTimeMillis() / 1000L;
		boolean isActive = computeIsActive(existing, nowSec);
		int kq = existing.getKqStatus() != null ? existing.getKqStatus() : 0;
		if (kq != STATUS_INIT && isActive) {
			merged.remove("items");
			merged.remove("distributor_id");
		}
		double hour = computeUsageHours(existing);
		if (merged.containsKey("lock_time")) {
			int lock = DiscountCardParamNormalize.parseIntFlexible(merged.get("lock_time"), 0);
			if (lock > hour) {
				throw new ResourceException(KaquanDiscountCardMessages.LOCK_TIME_CANNOT_EXCEED_USAGE_TIME);
			}
		}
		if (kq == STATUS_INIT) {
			if (!merged.containsKey("items") || !merged.containsKey("distributor_id")) {
				merged.put("kq_status", STATUS_INIT);
			} else if (merged.containsKey("kq_status")
					&& DiscountCardParamNormalize.parseIntFlexible(merged.get("kq_status"), -1) == STATUS_INIT) {
				merged.put("kq_status", STATUS_NORMAL);
			}
		}
		if (merged.get("distributor_id") instanceof List<?> dl && !dl.isEmpty()) {
			merged.put("use_all_shops", "false");
		} else if (merged.containsKey("distributor_id")) {
			merged.put("use_all_shops", "true");
		}
		DiscountCardsColumnApplier.applyForUpdate(existing, merged, objectMapper);
		int rows = discountCardsMapper.updateById(existing);
		if (rows <= 0) {
			throw new ResourceException(KaquanDiscountCardMessages.DATA_UPDATE_FAILED_RETRY);
		}
		boolean persistItems = requestedItems && merged.containsKey("items");
		if (persistItems) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> items = (List<Map<String, Object>>) merged.get("items");
			relItemsMapper.delete(new LambdaQueryWrapper<RelItems>()
					.eq(RelItems::getCardId, cardId)
					.eq(RelItems::getCompanyId, companyId));
			if (items != null && !items.isEmpty()) {
				insertRelItemsChunks(items, cardId, companyId);
			}
		}
		relMemberTagsMapper.delete(new LambdaQueryWrapper<RelMemberTags>()
				.eq(RelMemberTags::getCardId, cardId)
				.eq(RelMemberTags::getCompanyId, companyId));
		if (data.containsKey("user_tag_ids")) {
			@SuppressWarnings("unchecked")
			List<Object> tagIds = (List<Object>) merged.get("user_tag_ids");
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
				relMemberTagsMapper.insertBatch(batch);
			}
		}
		multiLangWriteService.writeAfterUpdate(cardId, companyId, data);
		final long publishCardId = cardId;
		final long publishCompanyId = companyId;
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				couponEditEventDispatchPublisher.publish(
						publishCardId, publishCompanyId, KaquanDispatchEventNames.EVENT_COUPON_EDIT_NEW_GIFT);
			}
		});
		DiscountCards loaded = discountCardsMapper.selectById(cardId);
		if (loaded == null) {
			throw new ResourceException(KaquanDiscountCardMessages.NO_UPDATE_DATA_FOUND);
		}
		return rowMapperService.toSnakeCaseMap(loaded);
	}

	private void validateMemberScopeExpandOnlyAfterSetParams(DiscountCards existing, Map<String, Object> merged) {
		List<Long> detailGrades = parseCsvIdList(existing.getGradeIds());
		List<Long> detailVip = parseCsvIdList(existing.getVipGradeIds());
		boolean detailAllMembers = detailGrades.isEmpty() && detailVip.isEmpty();
		boolean requestGrades = merged.containsKey("grade_ids") && nonEmptyList(merged.get("grade_ids"));
		boolean requestVip = merged.containsKey("vip_grade_ids") && nonEmptyList(merged.get("vip_grade_ids"));
		if (!requestGrades && !requestVip) {
			return;
		}
		if (detailAllMembers && (requestGrades || requestVip)) {
			throw new ResourceException(KaquanDiscountCardMessages.MEMBERS_ONLY_EXPAND_NOT_REDUCE_SCOPE);
		}
		if (merged.containsKey("grade_ids")) {
			@SuppressWarnings("unchecked")
			List<Object> newGrades = (List<Object>) merged.get("grade_ids");
			List<Object> effectiveGrades = newGrades != null ? newGrades : List.of();
			for (Long gid : detailGrades) {
				if (!listContainsId(effectiveGrades, gid)) {
					throw new ResourceException(KaquanDiscountCardMessages.MEMBERS_ONLY_EXPAND_NOT_REDUCE_SCOPE);
				}
			}
		}
		if (merged.containsKey("vip_grade_ids")) {
			@SuppressWarnings("unchecked")
			List<Object> newVip = (List<Object>) merged.get("vip_grade_ids");
			List<Object> effectiveVip = newVip != null ? newVip : List.of();
			for (Long vgid : detailVip) {
				if (!listContainsId(effectiveVip, vgid)) {
					throw new ResourceException(KaquanDiscountCardMessages.MEMBERS_ONLY_EXPAND_NOT_REDUCE_SCOPE);
				}
			}
		}
	}

	private static boolean nonEmptyList(Object raw) {
		return raw instanceof List<?> l && !l.isEmpty();
	}

	private static boolean listContainsId(List<Object> list, long id) {
		for (Object o : list) {
			if (DiscountCardParamNormalize.longFromObject(o, -1L) == id) {
				return true;
			}
		}
		return false;
	}

	private static List<Long> parseCsvIdList(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		if (t.startsWith(",")) {
			t = t.substring(1);
		}
		if (t.endsWith(",")) {
			t = t.substring(0, t.length() - 1);
		}
		if (!StringUtils.hasText(t)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String p : t.split(",")) {
			if (StringUtils.hasText(p.trim())) {
				out.add(DiscountCardParamNormalize.longFromObject(p.trim(), 0L));
			}
		}
		return out;
	}

	private static boolean computeIsActive(DiscountCards detail, long nowEpochSeconds) {
		String dt = DiscountCardParamNormalize.stringVal(detail.getDateType());
		if (DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dt)) {
			int sbt = detail.getSendBeginTime() != null ? detail.getSendBeginTime() : 0;
			int bd = detail.getBeginDate() != null ? detail.getBeginDate() : 0;
			return (sbt + 86400L * bd) < nowEpochSeconds;
		}
		int bd = detail.getBeginDate() != null ? detail.getBeginDate() : 0;
		return bd < nowEpochSeconds;
	}

	private static double computeUsageHours(DiscountCards detail) {
		String dt = DiscountCardParamNormalize.stringVal(detail.getDateType());
		if (DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dt)) {
			int ft = detail.getFixedTerm() != null ? detail.getFixedTerm() : 0;
			return ft * 24.0;
		}
		int b = detail.getBeginDate() != null ? detail.getBeginDate() : 0;
		int e = detail.getEndDate() != null ? detail.getEndDate() : 0;
		return (e - b) / 3600.0;
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
