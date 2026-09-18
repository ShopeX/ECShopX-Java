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

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageDiscountCardsLoadResult;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageDiscountCardsLoadService;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageTriggerPackageIdsService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserDiscountBindCardListQueryService {

	private static final String DATE_TYPE_FIX_TERM = "DATE_TYPE_FIX_TERM";

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static final String[] DISCOUNT_CARD_FIELD_ORDER = {
		"give_num",
		"takeEffect",
		"card_id",
		"title",
		"card_type",
		"date_type",
		"description",
		"begin_date",
		"end_date",
		"begin_time",
		"end_time",
		"fixed_term",
		"quantity",
		"receive",
		"kq_status",
		"grade_ids",
		"vip_grade_ids",
		"get_limit",
		"gift",
		"default_detail",
		"discount",
		"least_cost",
		"reduce_cost",
		"get_num",
		"lock_time",
		"deal_detail",
		"accept_category",
		"reject_category",
		"object_use_for",
		"can_use_with_other_discount",
		"use_platform",
		"use_bound",
		"send_begin_time",
		"send_end_time"
	};

	private final CardPackageTriggerPackageIdsService cardPackageTriggerPackageIdsService;
	private final CardPackageDiscountCardsLoadService cardPackageDiscountCardsLoadService;
	private final UserDiscountMapper userDiscountMapper;
	private final DiscountCardsRowMapperService discountCardsRowMapperService;
	private final DiscountCardSerializedFieldDecodeService discountCardSerializedFieldDecodeService;
	private final DiscountCardsMultiLangReadService discountCardsMultiLangReadService;

	public UserDiscountBindCardListQueryService(CardPackageTriggerPackageIdsService cardPackageTriggerPackageIdsService,
			CardPackageDiscountCardsLoadService cardPackageDiscountCardsLoadService,
			UserDiscountMapper userDiscountMapper,
			DiscountCardsRowMapperService discountCardsRowMapperService,
			DiscountCardSerializedFieldDecodeService discountCardSerializedFieldDecodeService,
			DiscountCardsMultiLangReadService discountCardsMultiLangReadService) {
		this.cardPackageTriggerPackageIdsService = cardPackageTriggerPackageIdsService;
		this.cardPackageDiscountCardsLoadService = cardPackageDiscountCardsLoadService;
		this.userDiscountMapper = userDiscountMapper;
		this.discountCardsRowMapperService = discountCardsRowMapperService;
		this.discountCardSerializedFieldDecodeService = discountCardSerializedFieldDecodeService;
		this.discountCardsMultiLangReadService = discountCardsMultiLangReadService;
	}

	public Map<String, Object> buildBindCardList(long companyId, long gradeId, String type) {
		List<Long> packageIds = cardPackageTriggerPackageIdsService.listPackageIds(companyId, gradeId, type);
		CardPackageDiscountCardsLoadResult load = cardPackageDiscountCardsLoadService.load(companyId, packageIds);
		List<DiscountCards> cards = load.cards();
		if (cards.isEmpty()) {
			return Map.of("list", List.of(), "total_count", 0);
		}

		List<Long> cardIds = new ArrayList<>(cards.size());
		for (DiscountCards c : cards) {
			if (c.getCardId() != null) {
				cardIds.add(c.getCardId());
			}
		}
		Map<Long, Integer> getNumByCardId = toCountMap(userDiscountMapper.countReceivedGroupByCardId(companyId, cardIds));
		Map<Long, Integer> useNumByCardId = toCountMap(userDiscountMapper.countVerifiedGroupByCardId(companyId, cardIds));

		long nowEpoch = Instant.now().getEpochSecond();
		int nowInt = (int) Math.min(nowEpoch, Integer.MAX_VALUE);
		Map<Long, Long> giveNumByCardId = load.giveNumByCardId();

		List<Map<String, Object>> list = new ArrayList<>(cards.size());
		for (DiscountCards entity : cards) {
			Map<String, Object> row = new LinkedHashMap<>(discountCardsRowMapperService.toSnakeCaseMap(entity));
			Long cardId = entity.getCardId();

			if (cardId != null) {
				discountCardsMultiLangReadService.overlay(companyId, cardId, row);
			}

			row.put("get_num", cardId == null ? 0 : getNumByCardId.getOrDefault(cardId, 0));
			row.put("use_num", cardId == null ? 0 : useNumByCardId.getOrDefault(cardId, 0));

			String timeLimitRaw = entity.getTimeLimit();
			if (StringUtils.hasText(timeLimitRaw)) {
				List<Map<String, Object>> decodedTl = discountCardSerializedFieldDecodeService.decodeTimeLimit(timeLimitRaw);
				row.put("time_limit", decodedTl.isEmpty() ? null : decodedTl);
			} else {
				row.put("time_limit", null);
			}

			applyFixTermDates(row, entity, nowInt);

			Object bForStatus = row.get("begin_date");
			Object eForStatus = row.get("end_date");
			row.put("date_status", KaquanCardDateStatusService.getDateStatus(
					bForStatus == null ? "" : String.valueOf(bForStatus),
					eForStatus == null ? "" : String.valueOf(eForStatus)));

			row.put("takeEffect", "");
			String dateType = entity.getDateType();
			if (DATE_TYPE_FIX_TERM.equals(dateType) || DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dateType)) {
				Integer origBegin = entity.getBeginDate();
				String beginPart;
				if (origBegin == null || origBegin == 0) {
					beginPart = "当";
				} else {
					beginPart = String.valueOf(origBegin);
				}
				Object ft = row.get("fixed_term");
				row.put("takeEffect", "领取后" + beginPart + "天生效," + ft + "天有效");
			}

			int giveNum = cardId == null ? 0 : giveNumByCardId.getOrDefault(cardId, 0L).intValue();
			row.put("give_num", giveNum);

			int beginEpoch = epochFromRow(row.get("begin_date"));
			int endEpoch = epochFromRow(row.get("end_date"));
			row.put("begin_date", DATE_TIME_FMT.format(Instant.ofEpochSecond(beginEpoch)));
			row.put("end_date", DATE_TIME_FMT.format(Instant.ofEpochSecond(endEpoch)));
			row.put("begin_time", String.valueOf(beginEpoch));
			row.put("end_time", String.valueOf(endEpoch));

			Object gids = row.get("grade_ids");
			if (gids instanceof List<?> listG && listG.isEmpty()) {
				row.put("grade_ids", "");
			}
			Object vgids = row.get("vip_grade_ids");
			if (vgids instanceof List<?> vlist && vlist.isEmpty()) {
				row.put("vip_grade_ids", "");
			}

			Integer getLimit = entity.getGetLimit();
			row.put("get_limit", getLimit != null ? getLimit : 0);

			int sendBegin = entity.getSendBeginTime() != null ? entity.getSendBeginTime() : 0;
			row.put("send_begin_time", sendBegin);
			row.put("send_end_time", sendBegin);

			putScalarString(row, "kq_status");
			putScalarString(row, "receive");
			putScalarString(row, "card_type");
			putScalarString(row, "date_type");

			list.add(orderDiscountCardRow(row));
		}

		int totalCount = list.stream().mapToInt(r -> {
			Object gn = r.get("give_num");
			return gn instanceof Number n ? n.intValue() : 0;
		}).sum();

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", totalCount);
		return out;
	}

	private static void applyFixTermDates(Map<String, Object> row, DiscountCards entity, int nowInt) {
		String dateType = entity.getDateType();
		if (!DATE_TYPE_FIX_TERM.equals(dateType) && !DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dateType)) {
			return;
		}
		Integer origBegin = entity.getBeginDate();
		int beginOffsetDays = origBegin != null ? origBegin : 0;
		long beginEpoch = (long) nowInt + beginOffsetDays * 86400L;
		int beginEpochInt = (int) Math.min(beginEpoch, Integer.MAX_VALUE);
		row.put("begin_date", beginEpochInt);
		Integer origEnd = entity.getEndDate();
		int endVal = origEnd != null ? origEnd : 0;
		if (endVal <= 0) {
			int ft = entity.getFixedTerm() != null ? entity.getFixedTerm() : 0;
			long endEpoch = beginEpochInt + (long) ft * 86400L;
			row.put("end_date", (int) Math.min(endEpoch, Integer.MAX_VALUE));
		} else {
			row.put("end_date", endVal);
		}
	}

	private static Map<String, Object> orderDiscountCardRow(Map<String, Object> enriched) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (String k : DISCOUNT_CARD_FIELD_ORDER) {
			if (enriched.containsKey(k)) {
				out.put(k, enriched.get(k));
			}
		}
		for (Map.Entry<String, Object> e : enriched.entrySet()) {
			if (!out.containsKey(e.getKey())) {
				out.put(e.getKey(), e.getValue());
			}
		}
		return out;
	}

	private static Map<Long, Integer> toCountMap(List<UserDiscountCardAggRow> rows) {
		Map<Long, Integer> m = new HashMap<>();
		if (rows == null) {
			return m;
		}
		for (UserDiscountCardAggRow r : rows) {
			if (r.getCardId() != null && r.getNum() != null) {
				m.put(r.getCardId(), r.getNum().intValue());
			}
		}
		return m;
	}

	private static void putScalarString(Map<String, Object> row, String key) {
		Object v = row.get(key);
		row.put(key, v == null ? "" : String.valueOf(v));
	}

	private static int epochFromRow(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return (int) Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
