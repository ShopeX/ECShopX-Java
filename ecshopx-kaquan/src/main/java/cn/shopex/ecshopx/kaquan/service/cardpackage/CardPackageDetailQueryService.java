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
import cn.shopex.ecshopx.kaquan.domain.CardPackage;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardActionValidationService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsMultiLangReadService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsRowMapperService;
import cn.shopex.ecshopx.kaquan.service.discount.KaquanDiscountCardMessages;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CardPackageDetailQueryService {

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

	private final CardPackageMapper cardPackageMapper;
	private final CardPackageDiscountCardsLoadService cardPackageDiscountCardsLoadService;
	private final UserDiscountMapper userDiscountMapper;
	private final DiscountCardsRowMapperService rowMapperService;
	private final DiscountCardsMultiLangReadService discountCardsMultiLangReadService;
	private final CardPackageMultiLangReadService cardPackageMultiLangReadService;

	public CardPackageDetailQueryService(CardPackageMapper cardPackageMapper,
			CardPackageDiscountCardsLoadService cardPackageDiscountCardsLoadService,
			UserDiscountMapper userDiscountMapper,
			DiscountCardsRowMapperService rowMapperService,
			DiscountCardsMultiLangReadService discountCardsMultiLangReadService,
			CardPackageMultiLangReadService cardPackageMultiLangReadService) {
		this.cardPackageMapper = cardPackageMapper;
		this.cardPackageDiscountCardsLoadService = cardPackageDiscountCardsLoadService;
		this.userDiscountMapper = userDiscountMapper;
		this.rowMapperService = rowMapperService;
		this.discountCardsMultiLangReadService = discountCardsMultiLangReadService;
		this.cardPackageMultiLangReadService = cardPackageMultiLangReadService;
	}

	public Map<String, Object> getDetails(long companyId, long packageId, String countryCode) {
		CardPackage pkg = cardPackageMapper.selectOne(new LambdaQueryWrapper<CardPackage>()
				.eq(CardPackage::getCompanyId, companyId)
				.eq(CardPackage::getPackageId, packageId)
				.eq(CardPackage::getRowStatus, 1));
		if (pkg == null) {
			throw new ResourceException(KaquanDiscountCardMessages.PACKAGE_NOT_FOUND);
		}

		Map<String, Object> top = new LinkedHashMap<>();
		top.put("package_id", pkg.getPackageId() != null ? pkg.getPackageId().intValue() : 0);
		top.put("company_id", pkg.getCompanyId() != null ? pkg.getCompanyId() : 0L);
		top.put("title", pkg.getTitle() != null ? pkg.getTitle() : "");
		top.put("package_describe", pkg.getPackageDescribe() != null ? pkg.getPackageDescribe() : "");
		top.put("limit_count", pkg.getLimitCount() != null ? pkg.getLimitCount() : 0);
		top.put("get_num", pkg.getGetNum() != null ? pkg.getGetNum() : 0);

		cardPackageMultiLangReadService.overlayForDetail(companyId, packageId, top, countryCode);

		CardPackageDiscountCardsLoadResult loadResult =
				cardPackageDiscountCardsLoadService.load(companyId, List.of(packageId));
		List<DiscountCards> cards = loadResult.cards();
		Map<Long, Long> giveNumByCardId = loadResult.giveNumByCardId();

		if (cards.isEmpty()) {
			top.put("discount_cards", List.of());
			return top;
		}

		List<Long> cardIds = new ArrayList<>(cards.size());
		for (DiscountCards c : cards) {
			if (c.getCardId() != null) {
				cardIds.add(c.getCardId());
			}
		}
		Map<Long, Integer> getNumByCardId = toCountMap(userDiscountMapper.countReceivedGroupByCardId(companyId, cardIds));

		long nowEpoch = Instant.now().getEpochSecond();
		int nowInt = (int) Math.min(nowEpoch, Integer.MAX_VALUE);

		List<Map<String, Object>> discountRows = new ArrayList<>(cards.size());
		for (DiscountCards entity : cards) {
			Map<String, Object> row = new LinkedHashMap<>(rowMapperService.toSnakeCaseMap(entity));
			Long cid = entity.getCardId();

			String dateType = entity.getDateType();
			if (DATE_TYPE_FIX_TERM.equals(dateType) || DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dateType)) {
				Integer origBegin = entity.getBeginDate();
				row.put("begin_day_type", origBegin != null ? origBegin : 0);
				row.put("begin_date", nowInt);
				Integer origEnd = entity.getEndDate();
				int endVal = origEnd != null ? origEnd : 0;
				if (endVal <= 0) {
					int ft = entity.getFixedTerm() != null ? entity.getFixedTerm() : 0;
					row.put("end_date", nowInt + ft * 86400);
				} else {
					row.put("end_date", endVal);
				}
			}

			row.put("get_num", cid == null ? 0 : getNumByCardId.getOrDefault(cid, 0));

			if (cid != null) {
				discountCardsMultiLangReadService.overlay(companyId, cid, row);
			}

			enrichOneDiscountCardRow(entity, row, giveNumByCardId);
			discountRows.add(orderDiscountCardRow(row));
		}

		top.put("discount_cards", discountRows);
		return top;
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

	private static void enrichOneDiscountCardRow(
			DiscountCards entity, Map<String, Object> row, Map<Long, Long> giveNumByCardId) {
		row.put("takeEffect", "");

		String dateType = entity.getDateType();
		if (DATE_TYPE_FIX_TERM.equals(dateType) || DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dateType)) {
			Object bdt = row.get("begin_day_type");
			String beginPart;
			if (bdt instanceof Number n && n.intValue() == 0) {
				beginPart = "当";
			} else if ("0".equals(String.valueOf(bdt))) {
				beginPart = "当";
			} else {
				beginPart = String.valueOf(bdt);
			}
			Object ft = row.get("fixed_term");
			row.put("takeEffect", "领取后" + beginPart + "天生效," + ft + "天有效");
		}

		Object gids = row.get("grade_ids");
		if (gids instanceof List<?> list && list.isEmpty()) {
			row.put("grade_ids", "");
		}
		Object vgids = row.get("vip_grade_ids");
		if (vgids instanceof List<?> vlist && vlist.isEmpty()) {
			row.put("vip_grade_ids", "");
		}

		int beginEpoch = epochFromRow(row.get("begin_date"));
		int endEpoch = epochFromRow(row.get("end_date"));
		row.put("begin_date", DATE_TIME_FMT.format(Instant.ofEpochSecond(beginEpoch)));
		row.put("end_date", DATE_TIME_FMT.format(Instant.ofEpochSecond(endEpoch)));
		row.put("begin_time", String.valueOf(beginEpoch));
		row.put("end_time", String.valueOf(endEpoch));

		Long cardId = entity.getCardId();
		int giveNum = cardId == null ? 0 : giveNumByCardId.getOrDefault(cardId, 0L).intValue();
		row.put("give_num", giveNum);

		Integer getLimit = entity.getGetLimit();
		row.put("get_limit", getLimit != null ? getLimit : 0);

		int sendBegin = entity.getSendBeginTime() != null ? entity.getSendBeginTime() : 0;
		row.put("send_begin_time", sendBegin);
		row.put("send_end_time", sendBegin);

		putScalarString(row, "kq_status");
		putScalarString(row, "receive");
		putScalarString(row, "card_type");
		putScalarString(row, "date_type");
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
}
