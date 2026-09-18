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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageReceivesPackageService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.point.domain.PointMember;
import cn.shopex.ecshopx.point.mapper.PointMemberMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import cn.shopex.ecshopx.point.service.TurntableDrawCostQueryResult;
import cn.shopex.ecshopx.promotions.domain.LuckyDrawActivity;
import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDayKeyResolver;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawResponseMapper;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawStatus;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawStructuredLog;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableErrorCodes;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableErrorMessages;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableGrantQueryResult;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntablePrizeSelector;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntablePrizeType;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableProcessStep;
import cn.shopex.ecshopx.promotions.mapper.LuckyDrawActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.TurntableLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TurntableFrontJoinTurntableService {

	private final LuckyDrawActivityMapper luckyDrawActivityMapper;
	private final TurntableLogMapper turntableLogMapper;
	private final PointMemberMapper pointMemberMapper;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final UserDiscountReceiveCardService userDiscountReceiveCardService;
	private final CardPackageReceivesPackageService cardPackageReceivesPackageService;
	private final MemberAccountService memberAccountService;
	private final DiscountCardsMapper discountCardsMapper;
	private final TurntableCountReserveService countReserve;
	private final TurntablePrizeDayStockReserveService stockReserve;
	private final TurntableCouponCandidateService couponCandidate;
	private final TurntableDayKeyResolver dayKeyResolver;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public TurntableFrontJoinTurntableService(
			LuckyDrawActivityMapper luckyDrawActivityMapper,
			TurntableLogMapper turntableLogMapper,
			PointMemberMapper pointMemberMapper,
			PointMemberAddPointService pointMemberAddPointService,
			UserDiscountReceiveCardService userDiscountReceiveCardService,
			CardPackageReceivesPackageService cardPackageReceivesPackageService,
			MemberAccountService memberAccountService,
			DiscountCardsMapper discountCardsMapper,
			TurntableCountReserveService countReserve,
			TurntablePrizeDayStockReserveService stockReserve,
			TurntableCouponCandidateService couponCandidate,
			TurntableDayKeyResolver dayKeyResolver,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.luckyDrawActivityMapper = luckyDrawActivityMapper;
		this.turntableLogMapper = turntableLogMapper;
		this.pointMemberMapper = pointMemberMapper;
		this.pointMemberAddPointService = pointMemberAddPointService;
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
		this.cardPackageReceivesPackageService = cardPackageReceivesPackageService;
		this.memberAccountService = memberAccountService;
		this.discountCardsMapper = discountCardsMapper;
		this.countReserve = countReserve;
		this.stockReserve = stockReserve;
		this.couponCandidate = couponCandidate;
		this.dayKeyResolver = dayKeyResolver;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> joinTurntable(long userId, long companyId, String activityIdRaw, Locale locale) {
		return joinTurntable(userId, companyId, activityIdRaw, null, locale);
	}

	public Map<String, Object> joinTurntable(
			long userId, long companyId, String activityIdRaw, String requestIdRaw, Locale locale) {
		if (activityIdRaw == null || !StringUtils.hasText(activityIdRaw.trim())) {
			throw new BadRequestException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.ACTIVITY_NOT_FOUND, locale));
		}
		long actId;
		try {
			actId = Long.parseLong(activityIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.ACTIVITY_NOT_FOUND, locale));
		}

		String requestId = TurntableDrawResponseMapper.normalizeRequestId(requestIdRaw);
		if (!TurntableDrawResponseMapper.isValidRequestId(requestId)) {
			throw new BadRequestException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.REQUEST_ID_INVALID, locale));
		}

		TurntableLog existing = findLogByRequest(companyId, userId, actId, requestId);
		if (existing != null) {
			return toDrawResponse(existing, userId, companyId);
		}

		LuckyDrawActivity act =
				luckyDrawActivityMapper.selectOne(
						new LambdaQueryWrapper<LuckyDrawActivity>()
								.eq(LuckyDrawActivity::getId, actId)
								.eq(LuckyDrawActivity::getCompanyId, companyId));
		if (act == null) {
			throw new ResourceException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.ACTIVITY_NOT_FOUND, locale));
		}
		if (act.getActivityType() != null
				&& StringUtils.hasText(act.getActivityType())
				&& !"wheel".equalsIgnoreCase(act.getActivityType().trim())) {
			throw new BadRequestException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.ACTIVITY_NOT_FOUND, locale));
		}

		long now = System.currentTimeMillis() / 1000L;
		Long end = act.getEndTime();
		if (end != null && end <= now) {
			throw new BadRequestException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.ENDED, locale));
		}
		Long begin = act.getBeginTime();
		if (begin != null && begin > now) {
			throw new BadRequestException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.NOT_STARTED, locale));
		}

		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		long currentPoint = (row == null || row.getPoint() == null) ? 0L : row.getPoint();
		long costValue = act.getCostValue() == null ? 0L : act.getCostValue();
		if (currentPoint < costValue) {
			throw new BadRequestException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.POINT_NOT_ENOUGH, locale));
		}

		Long configVersion = act.getConfigVersion() == null ? 1L : act.getConfigVersion();
		TurntableLog log;
		try {
			log = insertProcessingLog(userId, companyId, actId, requestId, configVersion, costValue);
		} catch (DuplicateKeyException dup) {
			TurntableLog again = findLogByRequest(companyId, userId, actId, requestId);
			if (again != null) {
				return toDrawResponse(again, userId, companyId);
			}
			throw dup;
		}

		TurntableDrawStructuredLog.infoDraw("draw_started", log);
		return continuePipeline(log.getId(), userId, companyId, locale, false);
	}

	/** 补偿任务续跑 PROCESSING 单。已结束活动上的在途单仍允许完成。 */
	public boolean resumeProcessing(TurntableLog stuck) {
		if (stuck == null || !TurntableDrawStatus.PROCESSING.equals(stuck.getStatus())) {
			return false;
		}
		String beforeStep = stuck.getProcessStep();
		try {
			continuePipeline(
					stuck.getId(),
					stuck.getUserId() == null ? 0L : stuck.getUserId(),
					stuck.getCompanyId() == null ? 0L : stuck.getCompanyId(),
					Locale.CHINA,
					true);
		} catch (BadRequestException e) {
			return true;
		} catch (Exception e) {
			TurntableDrawStructuredLog.warnRecover(stuck, e.getMessage());
			return false;
		}
		TurntableLog after = turntableLogMapper.selectById(stuck.getId());
		if (after == null) {
			return false;
		}
		if (!TurntableDrawStatus.PROCESSING.equals(after.getStatus())) {
			return true;
		}
		return beforeStep != null && !beforeStep.equals(after.getProcessStep());
	}

	private Map<String, Object> continuePipeline(
			long logId, long userId, long companyId, Locale locale, boolean recover) {
		TurntableLog log = turntableLogMapper.selectById(logId);
		if (log == null) {
			throw new ResourceException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.ACTIVITY_NOT_FOUND, locale));
		}
		if (!TurntableDrawStatus.PROCESSING.equals(log.getStatus())) {
			return toDrawResponse(log, userId, companyId);
		}
		String startStep = log.getProcessStep();
		LuckyDrawActivity act = loadActivity(companyId, log.getActId() == null ? 0L : log.getActId());
		if (act == null) {
			throw new ResourceException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.ACTIVITY_NOT_FOUND, locale));
		}
		List<Map<String, Object>> prizes = parsePrizes(act.getPrizeData(), locale);
		List<Map<String, Object>> sorted = TurntablePrizeSelector.sortedCopy(prizes);
		if (TurntablePrizeSelector.indexOfFirstThanks(sorted) < 0) {
			throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
		}
		String dayKey = dayKeyResolver.dayKey(companyId, log.getCreated() == null ? 0 : log.getCreated());
		if (!StringUtils.hasText(dayKey) || log.getCreated() == null || log.getCreated() <= 0) {
			dayKey = dayKeyResolver.dayKey(companyId);
		}
		String step = log.getProcessStep();
		if (!StringUtils.hasText(step) || TurntableProcessStep.CREATED.equals(step)) {
			applyPrizeSelection(log, sorted, locale);
			log = turntableLogMapper.selectById(logId);
			step = log.getProcessStep();
		}
		if (TurntableProcessStep.PRIZE_SELECTED.equals(step)) {
			applyStockOrThanks(log, act, sorted, dayKey, locale);
			log = turntableLogMapper.selectById(logId);
			step = log.getProcessStep();
		}
		if (TurntableProcessStep.DRAW_STOCK_RESERVED.equals(step)
				|| (TurntableProcessStep.PRIZE_SELECTED.equals(step)
						&& TurntablePrizeType.THANKS.equalsIgnoreCase(log.getPrizeType()))) {
			boolean stockReserved =
					TurntablePrizeType.COUPON.equalsIgnoreCase(log.getPrizeType())
							|| TurntablePrizeType.COUPONS.equalsIgnoreCase(log.getPrizeType());
			applyCountAndCost(
					log, act, sorted, dayKey, locale, stockReserved, stockReserved ? log.getPrizeId() : null);
			log = turntableLogMapper.selectById(logId);
			step = log.getProcessStep();
		}
		if (TurntableProcessStep.COUNT_RESERVED.equals(step)) {
			boolean queryOnly =
					recover
							&& (TurntableProcessStep.COUNT_RESERVED.equals(startStep)
									|| TurntableProcessStep.POINT_DEDUCTED.equals(startStep)
									|| TurntableProcessStep.GRANTING.equals(startStep));
			applyPointDeduct(log, act, sorted, dayKey, locale, queryOnly);
			log = turntableLogMapper.selectById(logId);
			step = log.getProcessStep();
		}
		if (TurntableProcessStep.POINT_DEDUCTED.equals(step) || TurntableProcessStep.GRANTING.equals(step)) {
			applyGrant(log, sorted, locale);
			log = turntableLogMapper.selectById(logId);
		}
		return toDrawResponse(log, userId, companyId);
	}

	private void applyPrizeSelection(TurntableLog log, List<Map<String, Object>> sorted, Locale locale) {
		TurntablePrizeSelector.DrawPick pick = TurntablePrizeSelector.pick(sorted);
		Map<String, Object> relData = new LinkedHashMap<>(pick.prize());
		relData = maybeDowngradeUnissuable(log, sorted, relData, pick.sectorIndex());
		try {
			updateLogPrizeFields(
					log.getId(),
					relData,
					sectorOf(sorted, relData, pick.sectorIndex()),
					log.getOriginalPrizeId(),
					TurntableProcessStep.PRIZE_SELECTED,
					pick.randomValue());
		} catch (JsonProcessingException e) {
			throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
		}
	}

	private Map<String, Object> maybeDowngradeUnissuable(
			TurntableLog log, List<Map<String, Object>> sorted, Map<String, Object> relData, int sectorIndex) {
		String type = firstType(relData);
		long companyId = log.getCompanyId() == null ? 0L : log.getCompanyId();
		long userId = log.getUserId() == null ? 0L : log.getUserId();
		boolean ok = true;
		if (TurntablePrizeType.COUPON.equals(type)) {
			ok = couponCandidate.isCouponIssuable(companyId, userId, prizeValueAsLong(relData));
		} else if (TurntablePrizeType.COUPONS.equals(type)) {
			ok = couponCandidate.isPackageIssuable(companyId, userId, prizeValueAsLong(relData));
		}
		if (ok) {
			return relData;
		}
		String original = TurntableDrawResponseMapper.prizeIdOf(relData);
		log.setOriginalPrizeId(original);
		int thanksIdx = TurntablePrizeSelector.indexOfFirstThanks(sorted);
		return new LinkedHashMap<>(sorted.get(thanksIdx));
	}

	private void applyStockOrThanks(
			TurntableLog log,
			LuckyDrawActivity act,
			List<Map<String, Object>> sorted,
			String dayKey,
			Locale locale) {
		long logId = log.getId();
		Map<String, Object> relData = prizeFromLog(log, sorted);
		String originalPrizeId = log.getOriginalPrizeId();
		int sectorIndex = log.getSectorIndex() == null ? 0 : log.getSectorIndex();
		String drawnType = firstType(relData);
		if (TurntablePrizeType.COUPON.equals(drawnType) || TurntablePrizeType.COUPONS.equals(drawnType)) {
			String prizeId = TurntableDrawResponseMapper.prizeIdOf(relData);
			int dailyStock = parseDailyStock(relData.containsKey("dailyStock") ? relData.get("dailyStock") : relData.get("stock"));
			long actId = act.getId() == null ? 0L : act.getId();
			if (!stockReserve.reserve(actId, prizeId, dayKey, dailyStock)) {
				originalPrizeId = prizeId;
				int thanksIdx = TurntablePrizeSelector.indexOfFirstThanks(sorted);
				relData = new LinkedHashMap<>(sorted.get(thanksIdx));
				sectorIndex = thanksIdx;
				try {
					updateLogPrizeFields(
							logId,
							relData,
							sectorIndex,
							originalPrizeId,
							TurntableProcessStep.PRIZE_SELECTED,
							log.getRandomValue());
				} catch (JsonProcessingException e) {
					throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
				}
				return;
			}
			updateLogStep(logId, TurntableProcessStep.DRAW_STOCK_RESERVED);
			return;
		}
		updateLogStep(logId, TurntableProcessStep.DRAW_STOCK_RESERVED);
	}

	private void applyCountAndCost(
			TurntableLog log,
			LuckyDrawActivity act,
			List<Map<String, Object>> sorted,
			String dayKey,
			Locale locale,
			boolean stockReserved,
			String reservedPrizeId) {
		long logId = log.getId();
		long companyId = log.getCompanyId() == null ? 0L : log.getCompanyId();
		long userId = log.getUserId() == null ? 0L : log.getUserId();
		long actId = log.getActId() == null ? 0L : log.getActId();
		Map<String, Object> relData = prizeFromLog(log, sorted);
		int sectorIndex = log.getSectorIndex() == null ? 0 : log.getSectorIndex();
		String originalPrizeId = log.getOriginalPrizeId();
		long limitTotal = act.getLimitTotal() == null ? 0L : act.getLimitTotal();
		long limitDay = act.getLimitDay() == null ? 0L : act.getLimitDay();
		TurntableCountReserveService.ReserveResult countResult =
				countReserve.reserve(companyId, userId, actId, dayKey, limitTotal, limitDay);
		if (!countResult.ok()) {
			if (stockReserved && StringUtils.hasText(reservedPrizeId)) {
				stockReserve.release(actId, reservedPrizeId, dayKey);
			}
			String limitCode =
					countResult.totalFailed() ? TurntableErrorCodes.TOTAL_LIMIT : TurntableErrorCodes.DAILY_LIMIT;
			try {
				finalizeLog(
						logId,
						relData,
						sectorIndex,
						TurntableDrawStatus.COST_FAILED,
						TurntableProcessStep.COUNT_RESERVED,
						originalPrizeId,
						limitCode,
						null);
			} catch (JsonProcessingException e) {
				throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
			}
			throw new BadRequestException(TurntableErrorMessages.message(messageSource, limitCode, locale));
		}
		updateLogStep(logId, TurntableProcessStep.COUNT_RESERVED);
	}

	private void applyPointDeduct(
			TurntableLog log,
			LuckyDrawActivity act,
			List<Map<String, Object>> sorted,
			String dayKey,
			Locale locale,
			boolean recover) {
		long logId = log.getId();
		long companyId = log.getCompanyId() == null ? 0L : log.getCompanyId();
		long userId = log.getUserId() == null ? 0L : log.getUserId();
		long actId = log.getActId() == null ? 0L : log.getActId();
		String requestId = log.getRequestId();
		Map<String, Object> relData = prizeFromLog(log, sorted);
		int sectorIndex = log.getSectorIndex() == null ? 0 : log.getSectorIndex();
		String originalPrizeId = log.getOriginalPrizeId();
		boolean stockReserved =
				TurntablePrizeType.COUPON.equalsIgnoreCase(log.getPrizeType())
						|| TurntablePrizeType.COUPONS.equalsIgnoreCase(log.getPrizeType());
		if (recover) {
			TurntableDrawCostQueryResult q =
					pointMemberAddPointService.queryTurntableDrawCostResult(companyId, userId, requestId);
			if (q == TurntableDrawCostQueryResult.SUCCESS) {
				updateLogStep(logId, TurntableProcessStep.POINT_DEDUCTED);
				return;
			}
			TurntableDrawStructuredLog.warnRecover(log, "point deduct UNKNOWN, keep PROCESSING");
			return;
		}
		long costValue = act.getCostValue() == null ? 0L : act.getCostValue();
		int costPoint = (int) Math.min(Integer.MAX_VALUE, costValue);
		try {
			pointMemberAddPointService.addPointForTurntableDrawCost(userId, companyId, costPoint, requestId);
		} catch (Exception e) {
			TurntableDrawCostQueryResult q =
					pointMemberAddPointService.queryTurntableDrawCostResult(companyId, userId, requestId);
			if (q == TurntableDrawCostQueryResult.SUCCESS) {
				TurntableDrawStructuredLog.infoDraw("point_deduct_ok_after_error", log, e.getMessage());
				updateLogStep(logId, TurntableProcessStep.POINT_DEDUCTED);
				return;
			}
			TurntableDrawStructuredLog.infoDraw("point_deduct_failed", log, e.getMessage());
			countReserve.release(companyId, userId, actId, dayKey);
			if (stockReserved && StringUtils.hasText(log.getPrizeId())) {
				stockReserve.release(actId, log.getPrizeId(), dayKey);
			}
			try {
				finalizeLog(
						logId,
						relData,
						sectorIndex,
						TurntableDrawStatus.COST_FAILED,
						TurntableProcessStep.COUNT_RESERVED,
						originalPrizeId,
						TurntableErrorCodes.COST_FAILED,
						e.getMessage());
			} catch (JsonProcessingException jpe) {
				throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
			}
			throw new BadRequestException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.COST_FAILED, locale));
		}
		updateLogStep(logId, TurntableProcessStep.POINT_DEDUCTED);
	}

	private void applyGrant(TurntableLog log, List<Map<String, Object>> sorted, Locale locale) {
		long logId = log.getId();
		Map<String, Object> relData = prizeFromLog(log, sorted);
		fillPrizeTitleIfEmpty(relData);
		int sectorIndex = log.getSectorIndex() == null ? 0 : log.getSectorIndex();
		String originalPrizeId = log.getOriginalPrizeId();
		String grantType = firstType(relData);
		if (TurntablePrizeType.THANKS.equalsIgnoreCase(grantType)
				|| TurntablePrizeType.THANKS.equalsIgnoreCase(log.getPrizeType())) {
			try {
				finalizeLog(
						logId,
						relData,
						sectorIndex,
						TurntableDrawStatus.SUCCESS,
						TurntableProcessStep.POINT_DEDUCTED,
						originalPrizeId,
						null,
						null);
			} catch (JsonProcessingException e) {
				throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
			}
			return;
		}
		if (TurntableProcessStep.GRANTING.equals(log.getProcessStep())) {
			TurntableGrantQueryResult queried = queryGrant(log, relData);
			if (queried == TurntableGrantQueryResult.SUCCESS) {
				try {
					finalizeLog(
							logId,
							relData,
							sectorIndex,
							TurntableDrawStatus.SUCCESS,
							TurntableProcessStep.GRANTING,
							originalPrizeId,
							null,
							null);
				} catch (JsonProcessingException e) {
					throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
				}
				return;
			}
			if (queried == TurntableGrantQueryResult.FAILED) {
				try {
					finalizeLog(
							logId,
							relData,
							sectorIndex,
							TurntableDrawStatus.GRANT_FAILED,
							TurntableProcessStep.GRANTING,
							originalPrizeId,
							TurntableErrorCodes.GRANT_FAILED,
							null);
				} catch (JsonProcessingException e) {
					throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
				}
				return;
			}
			TurntableDrawStructuredLog.warnRecover(log, "grant UNKNOWN, keep PROCESSING");
			return;
		}
		updateLogStep(logId, TurntableProcessStep.GRANTING);
		boolean granted = grantPrize(log, relData, grantType);
		try {
			finalizeLog(
					logId,
					relData,
					sectorIndex,
					granted ? TurntableDrawStatus.SUCCESS : TurntableDrawStatus.GRANT_FAILED,
					TurntableProcessStep.GRANTING,
					originalPrizeId,
					granted ? null : TurntableErrorCodes.GRANT_FAILED,
					null);
		} catch (JsonProcessingException e) {
			throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
		}
	}

	private TurntableGrantQueryResult queryGrant(TurntableLog log, Map<String, Object> relData) {
		long companyId = log.getCompanyId() == null ? 0L : log.getCompanyId();
		long userId = log.getUserId() == null ? 0L : log.getUserId();
		String requestId = log.getRequestId();
		String type = firstType(relData);
		if (TurntablePrizeType.POINTS.equals(type)) {
			TurntableDrawCostQueryResult q =
					pointMemberAddPointService.queryTurntableWinResult(companyId, userId, requestId);
			return q == TurntableDrawCostQueryResult.SUCCESS
					? TurntableGrantQueryResult.SUCCESS
					: TurntableGrantQueryResult.UNKNOWN;
		}
		if (TurntablePrizeType.COUPON.equals(type)) {
			return couponCandidate.queryCouponGrant(companyId, userId, requestId);
		}
		if (TurntablePrizeType.COUPONS.equals(type)) {
			return couponCandidate.queryPackageGrant(
					companyId, userId, prizeValueAsLong(relData), log.getCreated() == null ? 0 : log.getCreated());
		}
		return TurntableGrantQueryResult.UNKNOWN;
	}

	private LuckyDrawActivity loadActivity(long companyId, long actId) {
		return luckyDrawActivityMapper.selectOne(
				new LambdaQueryWrapper<LuckyDrawActivity>()
						.eq(LuckyDrawActivity::getId, actId)
						.eq(LuckyDrawActivity::getCompanyId, companyId));
	}

	private List<Map<String, Object>> parsePrizes(String prizeDataJson, Locale locale) {
		if (prizeDataJson == null || !StringUtils.hasText(prizeDataJson.trim())) {
			throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
		}
		try {
			List<Map<String, Object>> prizes =
					objectMapper.readValue(prizeDataJson.trim(), new TypeReference<List<Map<String, Object>>>() {});
			if (prizes == null || prizes.isEmpty()) {
				throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
			}
			List<Map<String, Object>> copy = new java.util.ArrayList<>();
			for (Map<String, Object> p : prizes) {
				copy.add(p == null ? new LinkedHashMap<>() : new LinkedHashMap<>(p));
			}
			return copy;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
		}
	}

	private Map<String, Object> prizeFromLog(TurntableLog log, List<Map<String, Object>> sorted) {
		Map<String, Object> found = TurntablePrizeSelector.findByPrizeId(sorted, log.getPrizeId());
		if (found != null) {
			return new LinkedHashMap<>(found);
		}
		if (log.getSectorIndex() != null && log.getSectorIndex() >= 0 && log.getSectorIndex() < sorted.size()) {
			return new LinkedHashMap<>(sorted.get(log.getSectorIndex()));
		}
		int thanks = TurntablePrizeSelector.indexOfFirstThanks(sorted);
		return new LinkedHashMap<>(sorted.get(Math.max(thanks, 0)));
	}

	private static int sectorOf(List<Map<String, Object>> sorted, Map<String, Object> relData, int fallback) {
		String pid = TurntableDrawResponseMapper.prizeIdOf(relData);
		for (int i = 0; i < sorted.size(); i++) {
			if (pid.equals(TurntableDrawResponseMapper.prizeIdOf(sorted.get(i)))) {
				return i;
			}
		}
		return fallback;
	}

	private static long prizeValueAsLong(Map<String, Object> relData) {
		Object raw = relData.containsKey("value") ? relData.get("value") : relData.get("prize_value");
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw == null ? "0" : raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private TurntableLog findLogByRequest(long companyId, long userId, long actId, String requestId) {
		return turntableLogMapper.selectOne(
				new LambdaQueryWrapper<TurntableLog>()
						.eq(TurntableLog::getCompanyId, companyId)
						.eq(TurntableLog::getUserId, userId)
						.eq(TurntableLog::getActId, actId)
						.eq(TurntableLog::getRequestId, requestId)
						.last("LIMIT 1"));
	}

	private TurntableLog insertProcessingLog(
			long userId,
			long companyId,
			long actId,
			String requestId,
			Long configVersion,
			long costPoints) {
		TurntableLog log = new TurntableLog();
		log.setCompanyId(companyId);
		log.setUserId(userId);
		log.setActId(actId);
		log.setRequestId(requestId);
		log.setStatus(TurntableDrawStatus.PROCESSING);
		log.setProcessStep(TurntableProcessStep.CREATED);
		log.setConfigVersion(configVersion);
		log.setCostPoints(costPoints);
		// 旧表 prize_title/prize_type 为 NOT NULL 无默认值；PROCESSING 落库先占位，定奖后再回写
		log.setPrizeTitle("");
		log.setPrizeType("");
		int created = (int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE);
		log.setCreated(created);
		log.setUpdated(created);
		turntableLogMapper.insert(log);
		return log;
	}

	private void updateLogStep(long logId, String step) {
		TurntableLog patch = new TurntableLog();
		patch.setId(logId);
		patch.setProcessStep(step);
		patch.setUpdated((int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE));
		turntableLogMapper.updateById(patch);
	}

	private void updateLogPrizeFields(
			long logId,
			Map<String, Object> relData,
			int sectorIndex,
			String originalPrizeId,
			String processStep,
			Integer randomValue)
			throws JsonProcessingException {
		TurntableLog patch = new TurntableLog();
		patch.setId(logId);
		patch.setProcessStep(processStep);
		patch.setSectorIndex(sectorIndex);
		patch.setPrizeId(TurntableDrawResponseMapper.prizeIdOf(relData));
		patch.setPrizeType(firstType(relData));
		patch.setPrizeTitle(resolvePrizeTitle(relData));
		patch.setPrizeValue(encodePrizeValue(firstType(relData), relData));
		if (randomValue != null) {
			patch.setRandomValue(randomValue);
		}
		if (StringUtils.hasText(originalPrizeId)) {
			patch.setOriginalPrizeId(originalPrizeId);
		}
		patch.setUpdated((int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE));
		turntableLogMapper.updateById(patch);
	}

	private TurntableLog finalizeLog(
			long logId,
			Map<String, Object> relData,
			int sectorIndex,
			String status,
			String processStep,
			String originalPrizeId,
			String errorCode,
			String errorMessage)
			throws JsonProcessingException {
		TurntableLog patch = new TurntableLog();
		patch.setId(logId);
		patch.setStatus(status);
		patch.setProcessStep(processStep);
		patch.setSectorIndex(sectorIndex);
		patch.setPrizeId(TurntableDrawResponseMapper.prizeIdOf(relData));
		patch.setPrizeType(firstType(relData));
		patch.setPrizeTitle(resolvePrizeTitle(relData));
		patch.setPrizeValue(encodePrizeValue(firstType(relData), relData));
		if (StringUtils.hasText(originalPrizeId)) {
			patch.setOriginalPrizeId(originalPrizeId);
		}
		if (StringUtils.hasText(errorCode)) {
			patch.setErrorCode(errorCode);
		}
		if (StringUtils.hasText(errorMessage)) {
			patch.setErrorMessage(errorMessage);
		}
		patch.setUpdated((int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE));
		turntableLogMapper.updateById(patch);
		return turntableLogMapper.selectById(logId);
	}

	private String resolvePrizeTitle(Map<String, Object> relData) {
		Object fontsObj = relData.get("fonts");
		if (fontsObj instanceof Map<?, ?> fm) {
			Object t = fm.get("text");
			if (t != null && StringUtils.hasText(String.valueOf(t))) {
				return String.valueOf(t);
			}
		}
		Object title = relData.containsKey("name") ? relData.get("name") : relData.get("prize_title");
		return title == null ? "" : String.valueOf(title);
	}

	private String encodePrizeValue(String prizeType, Map<String, Object> relData) throws JsonProcessingException {
		Object rawValue = relData.containsKey("value") ? relData.get("value") : relData.get("prize_value");
		if ("thanks".equals(prizeType)) {
			return "0";
		}
		if ("points".equals(prizeType)) {
			return rawValue == null ? null : String.valueOf(rawValue);
		}
		if ("coupon".equals(prizeType)) {
			return objectMapper.writeValueAsString(Collections.singletonList(rawValue));
		}
		if ("coupons".equals(prizeType)) {
			return objectMapper.writeValueAsString(rawValue);
		}
		return rawValue == null ? null : String.valueOf(rawValue);
	}

	private Map<String, Object> toDrawResponse(TurntableLog log, long userId, long companyId) {
		Long remainPoints = null;
		PointMember pm =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (pm != null) {
			remainPoints = pm.getPoint();
		}
		if (TurntableDrawStatus.PROCESSING.equals(log.getStatus())) {
			return TurntableDrawResponseMapper.processing(
					log.getActId() == null ? 0L : log.getActId(), log.getId(), log.getRequestId());
		}
		return TurntableDrawResponseMapper.fromLog(log, remainPoints);
	}

	private static String firstType(Map<String, Object> relData) {
		if (relData == null) {
			return "";
		}
		Object t = relData.containsKey("type") ? relData.get("type") : relData.get("prize_type");
		return t == null ? "" : String.valueOf(t);
	}

	private boolean grantPrize(TurntableLog log, Map<String, Object> relData, String prizeType) {
		long userId = log.getUserId() == null ? 0L : log.getUserId();
		long companyId = log.getCompanyId() == null ? 0L : log.getCompanyId();
		String requestId = log.getRequestId();
		if (TurntablePrizeType.THANKS.equals(prizeType)) {
			return true;
		}
		Object rawValue = relData.containsKey("value") ? relData.get("value") : relData.get("prize_value");
		if (TurntablePrizeType.POINTS.equals(prizeType)) {
			try {
				int pts = Integer.parseInt(String.valueOf(rawValue == null ? "0" : rawValue));
				pointMemberAddPointService.addPointForTurntableWin(userId, companyId, pts, requestId);
				return true;
			} catch (Exception e) {
				TurntableDrawStructuredLog.infoDraw("grant_points_failed", log, e.getMessage());
				return false;
			}
		}
		if (TurntablePrizeType.COUPON.equals(prizeType)) {
			return grantCoupon(userId, companyId, rawValue, requestId);
		}
		if (TurntablePrizeType.COUPONS.equals(prizeType)) {
			return grantCoupons(userId, companyId, rawValue);
		}
		return false;
	}

	private boolean grantCoupon(long userId, long companyId, Object prizeValue, String requestId) {
		long cardId;
		try {
			cardId = Long.parseLong(String.valueOf(prizeValue));
		} catch (NumberFormatException e) {
			return false;
		}
		DiscountCards card =
				discountCardsMapper.selectOne(
						new LambdaQueryWrapper<DiscountCards>()
								.eq(DiscountCards::getCompanyId, companyId)
								.eq(DiscountCards::getCardId, cardId)
								.last("LIMIT 1"));
		if (card == null) {
			return false;
		}
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
		if (!StringUtils.hasText(mobile)) {
			return false;
		}
		try {
			userDiscountReceiveCardService.receiveCard(
					companyId, userId, mobile, cardId, 0L, "", "大转盘中奖领取", requestId);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean grantCoupons(long userId, long companyId, Object pv) {
		if (pv == null) {
			return false;
		}
		if (pv instanceof Collection<?> || pv instanceof Map<?, ?> || (pv.getClass().isArray())) {
			return false;
		}
		long packageId;
		if (pv instanceof Number n) {
			packageId = n.longValue();
			if (packageId <= 0L) {
				return false;
			}
		} else if (pv instanceof String str) {
			String t = str.trim();
			if (!StringUtils.hasText(t)) {
				return false;
			}
			try {
				packageId = Long.parseLong(t);
			} catch (NumberFormatException e) {
				return false;
			}
		} else {
			return false;
		}
		try {
			cardPackageReceivesPackageService.receivesPackage(companyId, packageId, userId, "大转盘中奖领取", 0L);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static int parseDailyStock(Object stockObj) {
		if (stockObj == null) {
			return 0;
		}
		if (stockObj instanceof String s && s.isBlank()) {
			return 0;
		}
		if (stockObj instanceof Number n && n.intValue() == 0) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(stockObj));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static boolean isEmptyPrizeTitle(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return s.isEmpty() || "0".equals(s);
		}
		if (v instanceof Boolean b) {
			return Boolean.FALSE.equals(b);
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}

	private static void fillPrizeTitleIfEmpty(Map<String, Object> relData) {
		Object v = relData.get("prize_title");
		if (!isEmptyPrizeTitle(v)) {
			return;
		}
		String fontsText;
		Object fonts = relData.get("fonts");
		if (fonts instanceof Map<?, ?> fm) {
			Object t = fm.get("text");
			fontsText = (t == null) ? "" : String.valueOf(t);
		} else {
			fontsText = "";
		}
		relData.put("prize_title", fontsText);
	}

	private String msg(String code, String defaultMessage, Locale locale) {
		return messageSource.getMessage(code, null, defaultMessage, locale);
	}
}
