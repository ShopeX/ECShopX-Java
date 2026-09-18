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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.CardPackageReceive;
import cn.shopex.ecshopx.kaquan.domain.CardPackageReceiveDetails;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageReceiveDetailsMapper;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageReceiveMapper;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsRowMapperService;
import cn.shopex.ecshopx.kaquan.service.discount.KaquanDiscountCardMessages;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountCardPackageGrantService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CardPackageReceivesPackageService {

	private static final int ST_IN_PROGRESS = 1;
	private static final int ST_SUCCESS = 2;
	private static final int ST_FAIL = 3;

	private final CardPackageDetailQueryService cardPackageDetailQueryService;
	private final CardPackageReceiveMapper cardPackageReceiveMapper;
	private final CardPackageReceiveDetailsMapper cardPackageReceiveDetailsMapper;
	private final DiscountCardsMapper discountCardsMapper;
	private final UserDiscountCardPackageGrantService userDiscountCardPackageGrantService;
	private final CardPackageEditService cardPackageEditService;
	private final CardPackageReceivesPackageService self;

	public CardPackageReceivesPackageService(CardPackageDetailQueryService cardPackageDetailQueryService,
			CardPackageReceiveMapper cardPackageReceiveMapper,
			CardPackageReceiveDetailsMapper cardPackageReceiveDetailsMapper,
			DiscountCardsMapper discountCardsMapper,
			UserDiscountCardPackageGrantService userDiscountCardPackageGrantService,
			CardPackageEditService cardPackageEditService,
			@Lazy CardPackageReceivesPackageService self) {
		this.cardPackageDetailQueryService = cardPackageDetailQueryService;
		this.cardPackageReceiveMapper = cardPackageReceiveMapper;
		this.cardPackageReceiveDetailsMapper = cardPackageReceiveDetailsMapper;
		this.discountCardsMapper = discountCardsMapper;
		this.userDiscountCardPackageGrantService = userDiscountCardPackageGrantService;
		this.cardPackageEditService = cardPackageEditService;
		this.self = self;
	}

	public void receivesPackage(long companyId, long packageId, long userId, String from, long salespersonId) {
		Map<String, Object> packageDetails = cardPackageDetailQueryService.getDetails(companyId, packageId, null);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> cards =
				(List<Map<String, Object>>) packageDetails.getOrDefault("discount_cards", List.of());
		int limitCount = toInt(packageDetails.get("limit_count"), 0);
		long existCount = cardPackageReceiveMapper.selectCount(new LambdaQueryWrapper<CardPackageReceive>()
				.eq(CardPackageReceive::getPackageId, packageId)
				.eq(CardPackageReceive::getCompanyId, companyId)
				.eq(CardPackageReceive::getUserId, userId));
		if (existCount >= limitCount) {
			throw new ResourceException(KaquanDiscountCardMessages.EXCEED_LIMIT_COUNT);
		}
		List<Map<String, Object>> checkCardsResult =
				userDiscountCardPackageGrantService.checkCardList(companyId, userId, cards, from);
		int nowTime = (int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE);
		Long receiveId = self.insertReceiveAndDetailsInTransaction(companyId, packageId, userId, from, checkCardsResult,
				nowTime);
		if (receiveId == null) {
			throw new ResourceException(KaquanDiscountCardMessages.DATA_CREATE_FAILED_RETRY);
		}
		sendCouponsToUsers(companyId, userId, packageId, receiveId, from, salespersonId);
		assertReceiveSucceeded(companyId, userId, receiveId);
	}

	private void assertReceiveSucceeded(long companyId, long userId, long receiveId) {
		CardPackageReceive done = cardPackageReceiveMapper.selectById(receiveId);
		if (done != null && done.getReceiveStatus() != null && done.getReceiveStatus() == ST_SUCCESS) {
			return;
		}
		throw new ResourceException(resolveReceiveFailMessage(companyId, userId, receiveId));
	}

	private String resolveReceiveFailMessage(long companyId, long userId, long receiveId) {
		List<CardPackageReceiveDetails> fails =
				cardPackageReceiveDetailsMapper.selectList(
						new LambdaQueryWrapper<CardPackageReceiveDetails>()
								.eq(CardPackageReceiveDetails::getCompanyId, companyId)
								.eq(CardPackageReceiveDetails::getUserId, userId)
								.eq(CardPackageReceiveDetails::getReceiveId, receiveId)
								.eq(CardPackageReceiveDetails::getReceiveStatus, ST_FAIL)
								.last("LIMIT 1"));
		if (fails != null && !fails.isEmpty() && StringUtils.hasText(fails.get(0).getMessage())) {
			return fails.get(0).getMessage();
		}
		return KaquanDiscountCardMessages.DATA_CREATE_FAILED_RETRY;
	}

	@Transactional(rollbackFor = Exception.class)
	public Long insertReceiveAndDetailsInTransaction(long companyId, long packageId, long userId, String from,
			List<Map<String, Object>> checkCardsResult, int nowTime) {
		CardPackageReceive recv = new CardPackageReceive();
		recv.setPackageId(packageId);
		recv.setCompanyId(companyId);
		recv.setUserId(userId);
		recv.setReceiveType(from);
		recv.setReceiveStatus(ST_IN_PROGRESS);
		recv.setFrontShow(0);
		recv.setReceiveTime(nowTime);
		recv.setSuccessCount(0);
		recv.setCreated(nowTime);
		recv.setUpdated(nowTime);
		cardPackageReceiveMapper.insert(recv);
		Long rid = recv.getReceiveId();
		if (rid == null) {
			throw new ResourceException(KaquanDiscountCardMessages.DATA_CREATE_FAILED_RETRY);
		}
		for (Map<String, Object> item : checkCardsResult) {
			@SuppressWarnings("unchecked")
			Map<String, Object> cinfo = (Map<String, Object>) item.get("card_info");
			CardPackageReceiveDetails d = new CardPackageReceiveDetails();
			d.setReceiveId(rid);
			d.setPackageId(packageId);
			d.setCompanyId(companyId);
			d.setUserId(userId);
			d.setCardId(toLong(cinfo.get("card_id")));
			d.setMessage(str(item.get("message")));
			d.setReceiveStatus(Boolean.TRUE.equals(item.get("success")) ? ST_IN_PROGRESS : ST_FAIL);
			d.setCreated(nowTime);
			d.setUpdated(nowTime);
			cardPackageReceiveDetailsMapper.insert(d);
		}
		return rid;
	}

	private void sendCouponsToUsers(long companyId, long userId, long packageId, long receiveId, String from,
			long salespersonId) {
		List<CardPackageReceiveDetails> detailRows = cardPackageReceiveDetailsMapper.selectList(
				new LambdaQueryWrapper<CardPackageReceiveDetails>()
						.eq(CardPackageReceiveDetails::getCompanyId, companyId)
						.eq(CardPackageReceiveDetails::getUserId, userId)
						.eq(CardPackageReceiveDetails::getPackageId, packageId)
						.eq(CardPackageReceiveDetails::getReceiveId, receiveId)
						.eq(CardPackageReceiveDetails::getReceiveStatus, ST_IN_PROGRESS));
		int now = (int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE);
		if (detailRows.isEmpty()) {
			CardPackageReceive patch = new CardPackageReceive();
			patch.setReceiveStatus(ST_FAIL);
			patch.setReceiveTime(now);
			cardPackageReceiveMapper.update(patch, new LambdaQueryWrapper<CardPackageReceive>()
					.eq(CardPackageReceive::getReceiveId, receiveId)
					.eq(CardPackageReceive::getCompanyId, companyId)
					.eq(CardPackageReceive::getUserId, userId));
			return;
		}
		List<Long> cardIds = detailRows.stream()
				.map(CardPackageReceiveDetails::getCardId)
				.distinct()
				.toList();
		List<DiscountCards> discountEntities = cardIds.isEmpty()
				? List.of()
				: discountCardsMapper.selectList(new LambdaQueryWrapper<DiscountCards>()
						.eq(DiscountCards::getCompanyId, companyId)
						.in(DiscountCards::getCardId, cardIds));
		Map<Long, DiscountCards> byCardId = new HashMap<>();
		for (DiscountCards dc : discountEntities) {
			if (dc.getCardId() != null) {
				byCardId.put(dc.getCardId(), dc);
			}
		}
		for (CardPackageReceiveDetails d : detailRows) {
			if (!byCardId.containsKey(d.getCardId())) {
				CardPackageReceiveDetails dp = new CardPackageReceiveDetails();
				dp.setMessage(KaquanDiscountCardMessages.COUPON_INVALID);
				dp.setReceiveStatus(ST_FAIL);
				cardPackageReceiveDetailsMapper.update(dp, new LambdaQueryWrapper<CardPackageReceiveDetails>()
						.eq(CardPackageReceiveDetails::getId, d.getId()));
			}
		}
		List<CardPackageReceiveDetails> toSend = detailRows.stream()
				.filter(d -> byCardId.containsKey(d.getCardId()))
				.toList();
		if (toSend.isEmpty()) {
			CardPackageReceive patch = new CardPackageReceive();
			patch.setReceiveStatus(ST_FAIL);
			patch.setReceiveTime(now);
			cardPackageReceiveMapper.update(patch, new LambdaQueryWrapper<CardPackageReceive>()
					.eq(CardPackageReceive::getReceiveId, receiveId)
					.eq(CardPackageReceive::getCompanyId, companyId)
					.eq(CardPackageReceive::getUserId, userId));
			return;
		}
		List<Map<String, Object>> merged = new ArrayList<>();
		for (CardPackageReceiveDetails detail : toSend) {
			DiscountCards dc = byCardId.get(detail.getCardId());
			Map<String, Object> row = new LinkedHashMap<>(DiscountCardsRowMapperService.snakeCaseMapFrom(dc));
			row.put("id", detail.getId());
			row.put("receive_id", receiveId);
			row.put("give_num", 1);
			merged.add(row);
		}
		try {
			self.finalizeCouponGrantInTransaction(companyId, userId, packageId, receiveId, from, salespersonId, now,
					merged);
		} catch (Exception e) {
			self.markReceiveFailedInNewTransaction(companyId, userId, receiveId, now);
			throw e;
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void finalizeCouponGrantInTransaction(long companyId, long userId, long packageId, long receiveId,
			String from, long salespersonId, int now, List<Map<String, Object>> merged) {
		List<Map<String, Object>> userGetResult =
				userDiscountCardPackageGrantService.userGetCardList(companyId, userId, merged, from, salespersonId);
		List<Long> successIds = new ArrayList<>();
		for (Map<String, Object> item : userGetResult) {
			boolean ok = Boolean.TRUE.equals(item.get("success"));
			long detailPk = toLong(item.get("id"));
			if (ok) {
				successIds.add(detailPk);
			} else {
				CardPackageReceiveDetails dp = new CardPackageReceiveDetails();
				dp.setMessage(str(item.get("message")));
				dp.setReceiveStatus(ST_FAIL);
				cardPackageReceiveDetailsMapper.update(dp, new LambdaQueryWrapper<CardPackageReceiveDetails>()
						.eq(CardPackageReceiveDetails::getReceiveId, receiveId)
						.eq(CardPackageReceiveDetails::getPackageId, packageId)
						.eq(CardPackageReceiveDetails::getCompanyId, companyId)
						.eq(CardPackageReceiveDetails::getUserId, userId)
						.eq(CardPackageReceiveDetails::getCardId, toLong(item.get("card_id")))
						.eq(CardPackageReceiveDetails::getId, detailPk));
			}
		}
		if (successIds.isEmpty()) {
			CardPackageReceive recvPatch = new CardPackageReceive();
			recvPatch.setReceiveStatus(ST_FAIL);
			recvPatch.setReceiveTime(now);
			recvPatch.setSuccessCount(0);
			cardPackageReceiveMapper.update(recvPatch, new LambdaQueryWrapper<CardPackageReceive>()
					.eq(CardPackageReceive::getReceiveId, receiveId)
					.eq(CardPackageReceive::getCompanyId, companyId)
					.eq(CardPackageReceive::getUserId, userId));
			return;
		}
		CardPackageReceiveDetails okPatch = new CardPackageReceiveDetails();
		okPatch.setMessage("");
		okPatch.setReceiveStatus(ST_SUCCESS);
		cardPackageReceiveDetailsMapper.update(okPatch, new LambdaQueryWrapper<CardPackageReceiveDetails>()
				.eq(CardPackageReceiveDetails::getReceiveId, receiveId)
				.eq(CardPackageReceiveDetails::getPackageId, packageId)
				.eq(CardPackageReceiveDetails::getCompanyId, companyId)
				.eq(CardPackageReceiveDetails::getUserId, userId)
				.in(CardPackageReceiveDetails::getId, successIds));
		CardPackageReceive recvPatch = new CardPackageReceive();
		recvPatch.setReceiveStatus(ST_SUCCESS);
		recvPatch.setReceiveTime(now);
		recvPatch.setSuccessCount(successIds.size());
		cardPackageReceiveMapper.update(recvPatch, new LambdaQueryWrapper<CardPackageReceive>()
				.eq(CardPackageReceive::getReceiveId, receiveId)
				.eq(CardPackageReceive::getCompanyId, companyId)
				.eq(CardPackageReceive::getUserId, userId));
		cardPackageEditService.incrCardPackageGetNum(companyId, packageId);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void markReceiveFailedInNewTransaction(long companyId, long userId, long receiveId, int now) {
		CardPackageReceive failPatch = new CardPackageReceive();
		failPatch.setReceiveStatus(ST_FAIL);
		failPatch.setReceiveTime(now);
		cardPackageReceiveMapper.update(failPatch, new LambdaQueryWrapper<CardPackageReceive>()
				.eq(CardPackageReceive::getReceiveId, receiveId)
				.eq(CardPackageReceive::getCompanyId, companyId)
				.eq(CardPackageReceive::getUserId, userId));
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toInt(Object o, int def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
