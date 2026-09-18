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

import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCompanyProductModelReader;
import cn.shopex.ecshopx.common.core.domain.PageResult;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardKaquanListByItemIdQueryService {

	private static final String DATE_TYPE_FIX_TERM = "DATE_TYPE_FIX_TERM";
	private static final int STATUS_NORMAL = DiscountNewGiftCardUpdateService.STATUS_NORMAL;

	private final JdbcTemplate jdbcTemplate;
	private final DiscountCardCardIdsByGoodsForListService cardIdsByGoodsForListService;
	private final OperatorCartCompanyProductModelReader productModelReader;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final UserDiscountMapper userDiscountMapper;
	private final DiscountCardsMapper discountCardsMapper;
	private final DiscountCardsRowMapperService rowMapperService;
	private final DiscountCardSerializedFieldDecodeService serializedFieldDecodeService;

	public DiscountCardKaquanListByItemIdQueryService(
			JdbcTemplate jdbcTemplate,
			DiscountCardCardIdsByGoodsForListService cardIdsByGoodsForListService,
			OperatorCartCompanyProductModelReader productModelReader,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			UserDiscountMapper userDiscountMapper,
			DiscountCardsMapper discountCardsMapper,
			DiscountCardsRowMapperService rowMapperService,
			DiscountCardSerializedFieldDecodeService serializedFieldDecodeService) {
		this.jdbcTemplate = jdbcTemplate;
		this.cardIdsByGoodsForListService = cardIdsByGoodsForListService;
		this.productModelReader = productModelReader;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.userDiscountMapper = userDiscountMapper;
		this.discountCardsMapper = discountCardsMapper;
		this.rowMapperService = rowMapperService;
		this.serializedFieldDecodeService = serializedFieldDecodeService;
	}

	public PageResult<Map<String, Object>> query(Map<String, Object> filter, int pageNo, int pageSize) {
		pageNo = Math.max(1, pageNo);
		long companyId = toLong(filter.get("company_id"));
		long now = System.currentTimeMillis() / 1000L;
		int nowInt = (int) Math.min(now, Integer.MAX_VALUE);

		List<Long> itemCardIds = null;
		if (truthyItemId(filter)) {
			Map<String, Object> goodsFilter = new LinkedHashMap<>(filter);
			itemCardIds = cardIdsByGoodsForListService.resolveCardIdsByGoods(goodsFilter);
			if (itemCardIds == null || itemCardIds.isEmpty()) {
				return PageResult.of(0L, List.of());
			}
		}

		Long userId = toNullableLong(filter.get("user_id"));
		Integer gradeId = filter.get("grade_id") instanceof Number n ? n.intValue() : null;
		Long vipGradeIdForLike = null;
		if (userId != null) {
			Map<String, Object> vip = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
			if (Boolean.TRUE.equals(vip.get("is_open"))) {
				Object vg = vip.get("vip_grade_id");
				if (vg instanceof Number n && n.longValue() > 0L) {
					vipGradeIdForLike = n.longValue();
				}
			}
		}

		String productModel = productModelReader.getProductModel(companyId);

		List<String> cardTypes = normalizeCardTypes(filter.get("card_type"));
		if (cardTypes.isEmpty()) {
			cardTypes = List.of("cash", "discount", "new_gift", "money");
		}

		StringBuilder where = new StringBuilder(" WHERE kdc.company_id = ? ");
		List<Object> args = new ArrayList<>();
		args.add(companyId);

		if (itemCardIds != null) {
			where.append(" AND kdc.card_id IN (");
			for (int i = 0; i < itemCardIds.size(); i++) {
				if (i > 0) {
					where.append(",");
				}
				where.append("?");
				args.add(itemCardIds.get(i));
			}
			where.append(") ");
		}

		Object rawCid = filter.get("card_id");
		if (rawCid != null) {
			List<Long> cids = normalizeCardIdList(rawCid);
			if (cids != null && cids.isEmpty()) {
				return PageResult.of(0L, List.of());
			}
			if (cids != null && cids.size() == 1) {
				where.append(" AND kdc.card_id = ? ");
				args.add(cids.get(0));
			} else if (cids != null && !cids.isEmpty()) {
				where.append(" AND kdc.card_id IN (");
				for (int i = 0; i < cids.size(); i++) {
					if (i > 0) {
						where.append(",");
					}
					where.append("?");
					args.add(cids.get(i));
				}
				where.append(") ");
			}
		}

		if (filter.get("coupon_type") != null && StringUtils.hasText(String.valueOf(filter.get("coupon_type")))) {
			where.append(" AND kdc.coupon_type = ? ");
			args.add(String.valueOf(filter.get("coupon_type")).trim());
		}

		appendMemberOr(where, args, userId, gradeId, vipGradeIdForLike);

		where.append(" AND kdc.quantity >= 0 ");

		if (filter.containsKey("receive")) {
			where.append(" AND kdc.receive = ? ");
			args.add(String.valueOf(filter.get("receive")));
		}

		appendDistributorFilter(where, args, filter.get("distributor_id"), productModel);

		appendValidityOr(where, args, nowInt);

		where.append(" AND kdc.card_type IN (");
		for (int i = 0; i < cardTypes.size(); i++) {
			if (i > 0) {
				where.append(",");
			}
			where.append("?");
			args.add(cardTypes.get(i));
		}
		where.append(") ");

		String fromSql = " FROM kaquan_discount_cards kdc ";

		String countSql = "SELECT COUNT(*) " + fromSql + where;
		Long total = jdbcTemplate.queryForObject(countSql, Long.class, args.toArray());
		long totalCount = total == null ? 0L : total;

		if (totalCount == 0L || pageSize <= 0) {
			return PageResult.of(totalCount, List.of());
		}

		boolean sortByUserReceive = !filter.containsKey("item_id") && userId != null;

		int offset = (pageNo - 1) * pageSize;
		List<Long> orderedCardIds;
		if (sortByUserReceive) {
			String joinSql = " FROM kaquan_discount_cards kdc LEFT JOIN "
					+ "(SELECT card_id, COUNT(*) AS total FROM kaquan_user_discount WHERE user_id = ? GROUP BY card_id) kud "
					+ "ON kud.card_id = kdc.card_id ";
			List<Object> listArgs = new ArrayList<>();
			listArgs.add(userId);
			listArgs.addAll(args);
			String orderCase = "CASE WHEN (kdc.end_date <= " + nowInt + " AND kdc.end_date != 0) THEN 1 "
					+ "WHEN (kud.total IS NOT NULL AND kud.total >= kdc.get_limit) THEN 2 ELSE 3 END";
			String listSql = "SELECT kdc.card_id, " + orderCase + " AS lv " + joinSql + where
					+ " ORDER BY lv DESC, kdc.created DESC LIMIT ? OFFSET ?";
			listArgs.add(pageSize);
			listArgs.add(offset);
			orderedCardIds = jdbcTemplate.query(listSql, (rs, rowNum) -> rs.getLong("card_id"), listArgs.toArray());
		} else {
			List<Object> listArgs = new ArrayList<>(args);
			String listSql = "SELECT kdc.card_id " + fromSql + where + " ORDER BY kdc.created DESC LIMIT ? OFFSET ?";
			listArgs.add(pageSize);
			listArgs.add(offset);
			orderedCardIds = jdbcTemplate.query(listSql, (rs, rowNum) -> rs.getLong("card_id"), listArgs.toArray());
		}

		if (orderedCardIds.isEmpty()) {
			return PageResult.of(totalCount, List.of());
		}

		List<DiscountCards> entities = discountCardsMapper.selectBatchIds(orderedCardIds);
		Map<Long, DiscountCards> byId = entities.stream()
				.filter(e -> e.getCardId() != null)
				.collect(Collectors.toMap(DiscountCards::getCardId, e -> e, (a, b) -> a));

		Map<Long, Integer> getNums = toNumMap(
				userDiscountMapper.countIssuedGroupByCardId(companyId, orderedCardIds));
		Map<Long, Integer> useNums = toNumMap(
				userDiscountMapper.countVerifiedGroupByCardId(companyId, orderedCardIds));

		List<Map<String, Object>> list = new ArrayList<>();
		for (Long cid : orderedCardIds) {
			DiscountCards entity = byId.get(cid);
			if (entity == null) {
				continue;
			}
			Map<String, Object> row = new LinkedHashMap<>(rowMapperService.toSnakeCaseMap(entity));
			row.put("tag_ids", entity.getTagIds());
			row.put("brand_ids", entity.getBrandIds());
			row.put("get_num", getNums.getOrDefault(cid, 0));
			row.put("use_num", useNums.getOrDefault(cid, 0));

			String timeLimitRaw = entity.getTimeLimit();
			if (StringUtils.hasText(timeLimitRaw)) {
				List<Map<String, Object>> decodedTl = serializedFieldDecodeService.decodeTimeLimit(timeLimitRaw);
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

			list.add(row);
		}

		return PageResult.of(totalCount, list);
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

	private static void appendMemberOr(
			StringBuilder where, List<Object> args, Long userId, Integer gradeId, Long vipGradeIdForLike) {
		where.append(" AND ( ");
		where.append(" (IFNULL(kdc.grade_ids,'') = '' AND IFNULL(kdc.vip_grade_ids,'') = '') ");
		if (userId != null) {
			if (gradeId != null && gradeId > 0) {
				where.append(" OR kdc.grade_ids LIKE ? ");
				args.add("%," + gradeId + ",%");
			}
			if (vipGradeIdForLike != null) {
				where.append(" OR kdc.vip_grade_ids LIKE ? ");
				args.add("%," + vipGradeIdForLike + ",%");
			}
		}
		where.append(" ) ");
	}

	private static void appendDistributorFilter(
			StringBuilder where, List<Object> args, Object distributorRaw, String productModel) {
		if (distributorRaw == null) {
			return;
		}
		String distStr = String.valueOf(distributorRaw).trim();
		long distInt;
		try {
			distInt = Long.parseLong(distStr);
		} catch (NumberFormatException e) {
			return;
		}
		if (distInt != 0L) {
			String likePat = "%," + distInt + ",%";
			if ("platform".equals(productModel)) {
				where.append(" AND kdc.distributor_id LIKE ? ");
				args.add(likePat);
			} else {
				where.append(" AND (kdc.use_all_shops = '1' OR kdc.distributor_id LIKE ?) ");
				args.add(likePat);
			}
		} else {
			where.append(" AND kdc.distributor_id = ? ");
			args.add(",");
		}
	}

	private static void appendValidityOr(StringBuilder where, List<Object> args, int nowInt) {
		where.append(" AND ( ");
		where.append(" (kdc.card_type = 'new_gift' AND kdc.kq_status = ? ");
		args.add(STATUS_NORMAL);
		where.append(" AND ( (kdc.send_end_time IS NOT NULL AND kdc.send_end_time >= ?) OR kdc.send_end_time IS NULL ) ");
		args.add(nowInt);
		where.append(" AND kdc.send_begin_time <= ? ) ");
		args.add(nowInt);

		where.append(" OR (kdc.card_type = 'new_gift' AND kdc.send_begin_time <= ? ");
		args.add(nowInt);
		where.append(" AND kdc.fixed_term > 0 AND (kdc.send_end_time = 0 OR kdc.send_end_time IS NULL) ");
		where.append(" AND (kdc.end_date = 0 OR (kdc.end_date != 0 AND kdc.end_date >= ?)) ");
		args.add(nowInt);
		where.append(" AND kdc.kq_status = ? ) ");
		args.add(STATUS_NORMAL);

		where.append(" OR (kdc.card_type <> 'new_gift' AND kdc.end_date >= ?) ");
		args.add(nowInt);

		where.append(" OR (kdc.card_type <> 'new_gift' AND kdc.fixed_term > 0 AND kdc.end_date = 0) ");

		where.append(" ) ");
	}

	private static boolean truthyItemId(Map<String, Object> filter) {
		if (!filter.containsKey("item_id")) {
			return false;
		}
		Object v = filter.get("item_id");
		if (v instanceof List<?> list) {
			return !list.isEmpty();
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		return v != null && StringUtils.hasText(String.valueOf(v));
	}

	private static List<Long> normalizeCardIdList(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Number n) {
					out.add(n.longValue());
				} else if (o != null && StringUtils.hasText(o.toString())) {
					try {
						out.add(Long.parseLong(o.toString().trim()));
					} catch (NumberFormatException ignored) {
					}
				}
			}
			return out;
		}
		if (raw instanceof Number n) {
			return List.of(n.longValue());
		}
		try {
			return List.of(Long.parseLong(String.valueOf(raw).trim()));
		} catch (NumberFormatException e) {
			return List.of();
		}
	}

	private static List<String> normalizeCardTypes(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o != null && StringUtils.hasText(o.toString())) {
					out.add(o.toString().trim());
				}
			}
			return out;
		}
		String s = String.valueOf(raw).trim();
		return s.isEmpty() ? List.of() : List.of(s);
	}

	private static Map<Long, Integer> toNumMap(List<UserDiscountCardAggRow> rows) {
		Map<Long, Integer> m = new LinkedHashMap<>();
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

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o).trim());
	}

	private static Long toNullableLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		try {
			long v = Long.parseLong(o.toString().trim());
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
