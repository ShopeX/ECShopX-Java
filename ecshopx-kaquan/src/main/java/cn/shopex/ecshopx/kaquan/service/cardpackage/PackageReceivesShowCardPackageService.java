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
import cn.shopex.ecshopx.common.util.ElementJoiner;
import cn.shopex.ecshopx.kaquan.domain.CardPackageReceive;
import cn.shopex.ecshopx.kaquan.domain.CardPackageReceiveDetails;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageReceiveDetailsMapper;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageReceiveMapper;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardActionValidationService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsMultiLangReadService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsRowMapperService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PackageReceivesShowCardPackageService {

	private static final String DATE_TYPE_FIX_TERM = "DATE_TYPE_FIX_TERM";

	private static final DateTimeFormatter RECEIVE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static final DateTimeFormatter DATE_ONLY_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

	private final CardPackageReceiveMapper cardPackageReceiveMapper;
	private final CardPackageReceiveDetailsMapper cardPackageReceiveDetailsMapper;
	private final DiscountCardsMapper discountCardsMapper;
	private final DiscountCardsRowMapperService discountCardsRowMapperService;
	private final DiscountCardsMultiLangReadService discountCardsMultiLangReadService;

	public PackageReceivesShowCardPackageService(
			CardPackageReceiveMapper cardPackageReceiveMapper,
			CardPackageReceiveDetailsMapper cardPackageReceiveDetailsMapper,
			DiscountCardsMapper discountCardsMapper,
			DiscountCardsRowMapperService discountCardsRowMapperService,
			DiscountCardsMultiLangReadService discountCardsMultiLangReadService) {
		this.cardPackageReceiveMapper = cardPackageReceiveMapper;
		this.cardPackageReceiveDetailsMapper = cardPackageReceiveDetailsMapper;
		this.discountCardsMapper = discountCardsMapper;
		this.discountCardsRowMapperService = discountCardsRowMapperService;
		this.discountCardsMultiLangReadService = discountCardsMultiLangReadService;
	}

	public Map<String, Object> showCardPackage(long companyId, long userId, String receiveType) {
		List<CardPackageReceive> mainRows = cardPackageReceiveMapper.selectList(
				new LambdaQueryWrapper<CardPackageReceive>()
						.eq(CardPackageReceive::getCompanyId, companyId)
						.eq(CardPackageReceive::getUserId, userId)
						.eq(CardPackageReceive::getFrontShow, 0)
						.eq(CardPackageReceive::getReceiveType, receiveType)
						.in(CardPackageReceive::getReceiveStatus, List.of(1, 2)));
		if (mainRows == null || mainRows.isEmpty()) {
			return emptyPayload();
		}
		List<Long> receiveIdList = new ArrayList<>(mainRows.size());
		for (CardPackageReceive r : mainRows) {
			if (r.getReceiveId() != null) {
				receiveIdList.add(r.getReceiveId());
			}
		}
		if (receiveIdList.isEmpty()) {
			return emptyPayload();
		}
		List<CardPackageReceiveDetails> details = cardPackageReceiveDetailsMapper.selectList(
				new LambdaQueryWrapper<CardPackageReceiveDetails>()
						.in(CardPackageReceiveDetails::getReceiveId, receiveIdList)
						.eq(CardPackageReceiveDetails::getCompanyId, companyId)
						.eq(CardPackageReceiveDetails::getUserId, userId)
						.in(CardPackageReceiveDetails::getReceiveStatus, List.of(1, 2)));
		if (details == null || details.isEmpty()) {
			return emptyPayload();
		}
		Set<Long> cardIdSet = new LinkedHashSet<>();
		for (CardPackageReceiveDetails d : details) {
			if (d.getCardId() != null) {
				cardIdSet.add(d.getCardId());
			}
		}
		if (cardIdSet.isEmpty()) {
			return emptyPayload();
		}
		List<DiscountCards> discountCards = discountCardsMapper.selectList(
				new LambdaQueryWrapper<DiscountCards>()
						.eq(DiscountCards::getCompanyId, companyId)
						.in(DiscountCards::getCardId, cardIdSet));
		Map<Long, DiscountCards> cardEntityById = new HashMap<>();
		for (DiscountCards c : discountCards) {
			if (c.getCardId() != null) {
				cardEntityById.put(c.getCardId(), c);
			}
		}
		int nowInt = (int) Math.min(Instant.now().getEpochSecond(), Integer.MAX_VALUE);
		Map<Long, Map<String, Object>> discountCardsIndex = new LinkedHashMap<>();
		for (Long cardId : cardIdSet) {
			DiscountCards entity = cardEntityById.get(cardId);
			if (entity == null) {
				continue;
			}
			discountCardsIndex.put(cardId, buildDiscountCardIndexEntry(companyId, entity, nowInt));
		}
		for (CardPackageReceiveDetails d : details) {
			Long cid = d.getCardId();
			if (cid != null && !discountCardsIndex.containsKey(cid)) {
				throw new ResourceException("卡券数据异常，请稍后重试");
			}
		}
		Map<Long, List<Map<String, Object>>> detailsCardList = new LinkedHashMap<>();
		for (CardPackageReceiveDetails item : details) {
			Long cid = item.getCardId();
			Long rid = item.getReceiveId();
			if (cid == null || rid == null) {
				continue;
			}
			Map<String, Object> base = discountCardsIndex.get(cid);
			Map<String, Object> cardInfo = new LinkedHashMap<>(base);
			cardInfo.put("receive_status", receiveStatusToString(item.getReceiveStatus()));
			detailsCardList.computeIfAbsent(rid, k -> new ArrayList<>()).add(cardInfo);
		}
		List<Map<String, Object>> receiveRecordList = new ArrayList<>();
		List<Map<String, Object>> allCardList = new ArrayList<>();
		for (CardPackageReceive item : mainRows) {
			Map<String, Object> record = new LinkedHashMap<>();
			record.put("receive_id", item.getReceiveId() != null ? item.getReceiveId().intValue() : 0);
			record.put("package_id", item.getPackageId() != null ? item.getPackageId().intValue() : 0);
			record.put("receive_type", item.getReceiveType() != null ? item.getReceiveType() : "");
			record.put("receive_status", receiveStatusToString(item.getReceiveStatus()));
			record.put("front_show", item.getFrontShow() != null ? item.getFrontShow() : 0);
			int rt = item.getReceiveTime() != null ? item.getReceiveTime() : 0;
			record.put("receive_time", RECEIVE_TIME_FMT.format(Instant.ofEpochSecond(rt)));
			record.put("success_count", item.getSuccessCount() != null ? item.getSuccessCount() : 0);
			List<Map<String, Object>> rcl = detailsCardList.getOrDefault(item.getReceiveId(), List.of());
			record.put("receive_card_list", rcl);
			receiveRecordList.add(record);
			allCardList.addAll(rcl);
		}
		List<Long> idsToConfirm = new ArrayList<>();
		for (CardPackageReceive r : mainRows) {
			if (r.getReceiveId() != null) {
				idsToConfirm.add(r.getReceiveId());
			}
		}
		confirmPackageReceivesShow(companyId, userId, idsToConfirm);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("receive_record_list", receiveRecordList);
		out.put("all_card_list", allCardList);
		return out;
	}

	private Map<String, Object> buildDiscountCardIndexEntry(long companyId, DiscountCards entity, int nowInt) {
		Map<String, Object> row = new LinkedHashMap<>(discountCardsRowMapperService.toSnakeCaseMap(entity));
		Long cardId = entity.getCardId();
		String dateType = entity.getDateType();
		int displayBegin;
		int displayEnd;
		if (DATE_TYPE_FIX_TERM.equals(dateType) || DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dateType)) {
			Integer origBegin = entity.getBeginDate();
			int beginDayType = origBegin != null ? origBegin : 0;
			row.put("begin_day_type", beginDayType);
			displayBegin = nowInt;
			Integer origEnd = entity.getEndDate();
			int endVal = origEnd != null ? origEnd : 0;
			if (endVal <= 0) {
				int ft = entity.getFixedTerm() != null ? entity.getFixedTerm() : 0;
				displayEnd = nowInt + ft * 86400;
			} else {
				displayEnd = endVal;
			}
			row.put("begin_date", displayBegin);
			row.put("end_date", displayEnd);
			Object bdt = row.get("begin_day_type");
			String beginPart;
			if (bdt instanceof Number n && n.intValue() == 0) {
				beginPart = "当";
			} else if ("0".equals(String.valueOf(bdt))) {
				beginPart = "当";
			} else {
				beginPart = String.valueOf(bdt);
			}
			Object ftObj = row.get("fixed_term");
			row.put("takeEffect", "领取后" + beginPart + "天生效," + ftObj + "天有效");
		} else {
			displayBegin = entity.getBeginDate() != null ? entity.getBeginDate() : 0;
			displayEnd = entity.getEndDate() != null ? entity.getEndDate() : 0;
			row.put("takeEffect", "");
		}
		if (cardId != null) {
			discountCardsMultiLangReadService.overlay(companyId, cardId, row);
		}
		Object gids = row.get("grade_ids");
		if (gids instanceof List<?> list && list.isEmpty()) {
			row.put("grade_ids", "");
		}
		Object vgids = row.get("vip_grade_ids");
		if (vgids instanceof List<?> vlist && vlist.isEmpty()) {
			row.put("vip_grade_ids", "");
		}
		return toCardSubset(row, entity, displayBegin, displayEnd);
	}

	private Map<String, Object> toCardSubset(Map<String, Object> row, DiscountCards entity, int displayBegin, int displayEnd) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("takeEffect", row.get("takeEffect") != null ? String.valueOf(row.get("takeEffect")) : "");
		out.put("card_id", entity.getCardId() != null ? entity.getCardId().intValue() : 0);
		out.put("card_type", stringify(row.get("card_type")));
		out.put("date_type", stringify(row.get("date_type")));
		out.put("description", stringify(row.get("description")));
		out.put("begin_date", DATE_ONLY_FMT.format(Instant.ofEpochSecond(displayBegin)));
		out.put("end_date", DATE_ONLY_FMT.format(Instant.ofEpochSecond(displayEnd)));
		out.put("fixed_term", intField(row.get("fixed_term"), entity.getFixedTerm()));
		out.put("quantity", intField(row.get("quantity"), entity.getQuantity()));
		out.put("receive", stringify(row.get("receive")));
		out.put("title", stringify(row.get("title")));
		out.put("kq_status", stringify(row.get("kq_status")));
		out.put("grade_ids", gradeVipString(row.get("grade_ids")));
		out.put("vip_grade_ids", gradeVipString(row.get("vip_grade_ids")));
		out.put("get_limit", intField(row.get("get_limit"), entity.getGetLimit()));
		out.put("gift", stringify(row.get("gift")));
		out.put("default_detail", stringify(row.get("default_detail")));
		out.put("discount", intField(row.get("discount"), entity.getDiscount()));
		out.put("least_cost", intField(row.get("least_cost"), entity.getLeastCost()));
		out.put("reduce_cost", intField(row.get("reduce_cost"), entity.getReduceCost()));
		return out;
	}

	private static String gradeVipString(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				return "";
			}
			return ElementJoiner.joinComma(list);
		}
		return String.valueOf(raw);
	}

	private static String stringify(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int intField(Object fromRow, Integer entityFallback) {
		if (fromRow instanceof Number n) {
			return n.intValue();
		}
		if (fromRow != null) {
			try {
				return Integer.parseInt(String.valueOf(fromRow).trim());
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		return entityFallback != null ? entityFallback : 0;
	}

	private static String receiveStatusToString(Integer status) {
		if (status == null) {
			return "";
		}
		return switch (status) {
			case 1 -> "in_progress";
			case 2 -> "success";
			case 3 -> "fail";
			default -> String.valueOf(status);
		};
	}

	private static Map<String, Object> emptyPayload() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("receive_record_list", List.of());
		m.put("all_card_list", List.of());
		return m;
	}

	public void confirmPackageReceivesShow(long companyId, long userId, List<Long> receiveIds) {
		if (receiveIds == null || receiveIds.isEmpty()) {
			return;
		}
		cardPackageReceiveMapper.update(
				null,
				new LambdaUpdateWrapper<CardPackageReceive>()
						.set(CardPackageReceive::getFrontShow, 1)
						.eq(CardPackageReceive::getCompanyId, companyId)
						.eq(CardPackageReceive::getUserId, userId)
						.in(CardPackageReceive::getReceiveId, receiveIds));
	}
}
