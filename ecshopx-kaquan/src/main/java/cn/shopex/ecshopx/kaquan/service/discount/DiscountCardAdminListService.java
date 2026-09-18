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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardAdminListService {

	private static final String DATE_TYPE_FIX_TIME_RANGE = "DATE_TYPE_FIX_TIME_RANGE";
	private static final String DATE_TYPE_FIX_TERM = "DATE_TYPE_FIX_TERM";

	private final DiscountCardsMapper discountCardsMapper;
	private final UserDiscountMapper userDiscountMapper;
	private final DiscountCardsAdminListFilterBuilder filterBuilder;
	private final DiscountCardsRowMapperService rowMapperService;
	private final DiscountCardSerializedFieldDecodeService decodeService;
	private final DiscountCardsMultiLangReadService multiLangReadService;
	private final DistributorMapper distributorMapper;

	public DiscountCardAdminListService(DiscountCardsMapper discountCardsMapper,
			UserDiscountMapper userDiscountMapper,
			DiscountCardsAdminListFilterBuilder filterBuilder,
			DiscountCardsRowMapperService rowMapperService,
			DiscountCardSerializedFieldDecodeService decodeService,
			DiscountCardsMultiLangReadService multiLangReadService,
			DistributorMapper distributorMapper) {
		this.discountCardsMapper = discountCardsMapper;
		this.userDiscountMapper = userDiscountMapper;
		this.filterBuilder = filterBuilder;
		this.rowMapperService = rowMapperService;
		this.decodeService = decodeService;
		this.multiLangReadService = multiLangReadService;
		this.distributorMapper = distributorMapper;
	}

	public Map<String, Object> query(long companyId, Map<String, Object> merged,
			@SuppressWarnings("unused") Map<String, Object> operatorJwt) {
		int pageNo = Math.max(1, DiscountCardParamNormalize.parseIntFlexible(merged.get("page_no"), 1));
		int pageSize = DiscountCardParamNormalize.parseIntFlexible(merged.get("page_size"), 20);
		if (pageSize < 1) {
			pageSize = 20;
		}
		long sourceId = parseSourceId(merged.get("distributor_id"));
		String from = DiscountCardParamNormalize.stringVal(merged.get("from"));
		if (!StringUtils.hasText(from)) {
			from = "menu";
		}
		long nowEpoch = Instant.now().getEpochSecond();

		LambdaQueryWrapper<DiscountCards> baseWrapper =
				filterBuilder.build(companyId, merged, from, sourceId, nowEpoch);
		long total = discountCardsMapper.selectCount(baseWrapper);
		Map<String, Object> out = new LinkedHashMap<>();
		Map<String, Object> pagers = new LinkedHashMap<>();
		pagers.put("total", total);
		out.put("pagers", pagers);
		out.put("total_count", total);
		if (total == 0L) {
			out.put("list", List.of());
			return out;
		}

		LambdaQueryWrapper<DiscountCards> pageWrapper =
				filterBuilder.build(companyId, merged, from, sourceId, nowEpoch);
		pageWrapper.orderByDesc(DiscountCards::getCreated);
		Page<DiscountCards> page = new Page<>(pageNo, pageSize, false);
		Page<DiscountCards> pageOut = discountCardsMapper.selectPage(page, pageWrapper);
		List<DiscountCards> records = pageOut.getRecords();
		List<Long> cardIds = new ArrayList<>(records.size());
		for (DiscountCards c : records) {
			if (c.getCardId() != null) {
				cardIds.add(c.getCardId());
			}
		}
		Map<Long, Integer> getNums = cardIds.isEmpty() ? Map.of() : toCountMap(
				userDiscountMapper.countReceivedGroupByCardId(companyId, cardIds));
		Map<Long, Integer> useNums = cardIds.isEmpty() ? Map.of() : toCountMap(
				userDiscountMapper.countVerifiedGroupByCardId(companyId, cardIds));

		int nowInt = (int) Math.min(nowEpoch, Integer.MAX_VALUE);
		List<Map<String, Object>> list = new ArrayList<>(records.size());
		for (DiscountCards entity : records) {
			Map<String, Object> row = new LinkedHashMap<>(rowMapperService.toSnakeCaseMap(entity));
			Long cid = entity.getCardId();
			row.put("get_num", cid == null ? 0 : getNums.getOrDefault(cid, 0));
			row.put("use_num", cid == null ? 0 : useNums.getOrDefault(cid, 0));

			String timeLimitRaw = entity.getTimeLimit();
			if (StringUtils.hasText(timeLimitRaw)) {
				List<Map<String, Object>> decodedTl = decodeService.decodeTimeLimit(timeLimitRaw);
				row.put("time_limit", decodedTl.isEmpty() ? null : decodedTl);
			} else {
				row.put("time_limit", null);
			}

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

			Object bForStatus = row.get("begin_date");
			Object eForStatus = row.get("end_date");
			int dateStatus = KaquanCardDateStatusService.getDateStatus(
					bForStatus == null ? "" : String.valueOf(bForStatus),
					eForStatus == null ? "" : String.valueOf(eForStatus));
			row.put("date_status", dateStatus);

			if (cid != null) {
				multiLangReadService.overlay(companyId, cid, row);
			}
			normalizeListRowScalarFields(row);
			normalizeListRowEmptyIdListsToNull(row);
			list.add(row);
		}

		if (!list.isEmpty()) {
			fillSourceNames(list);
			applyListPresentation(list, sourceId);
		}

		out.put("list", list);
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

	private void fillSourceNames(List<Map<String, Object>> list) {
		Set<Long> distributorIds = new LinkedHashSet<>();
		for (Map<String, Object> row : list) {
			if ("distributor".equals(String.valueOf(row.get("source_type")))) {
				long id = toLong(row.get("source_id"));
				if (id > 0L) {
					distributorIds.add(id);
				}
			}
		}
		Map<Long, String> idToName = new HashMap<>();
		if (!distributorIds.isEmpty()) {
			List<Distributor> distRows = distributorMapper.selectList(new LambdaQueryWrapper<Distributor>()
					.in(Distributor::getDistributorId, distributorIds));
			for (Distributor d : distRows) {
				if (d.getDistributorId() != null) {
					idToName.put(d.getDistributorId(), d.getName() != null ? d.getName() : "");
				}
			}
		}
		for (Map<String, Object> row : list) {
			if (!"distributor".equals(String.valueOf(row.get("source_type")))) {
				row.put("source_name", "");
				continue;
			}
			long id = toLong(row.get("source_id"));
			row.put("source_name", idToName.getOrDefault(id, ""));
		}
	}

	private static void applyListPresentation(List<Map<String, Object>> list, long sourceId) {
		for (Map<String, Object> row : list) {
			String dateType = String.valueOf(row.get("date_type"));
			if (DATE_TYPE_FIX_TIME_RANGE.equals(dateType)
					|| DiscountCardActionValidationService.DATE_TYPE_SHORT.equals(dateType)) {
				row.put("begin_time", row.get("begin_date"));
				row.put("end_time", row.get("end_date"));
			} else if (DATE_TYPE_FIX_TERM.equals(dateType)
					|| DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dateType)) {
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
				if (DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dateType)) {
					row.put("begin_time", null);
					row.put("end_time", null);
				}
			}
			row.put("operationType", "increase");
			row.put("storeValue", 1);
			row.put("storePop", false);

			long rowSourceId = toLong(row.get("source_id"));
			if (rowSourceId != sourceId) {
				if ("staff".equals(String.valueOf(row.get("source_type"))) && sourceId == 0L) {
					row.put("edit_btn", "Y");
				} else {
					row.put("edit_btn", "N");
				}
			} else {
				row.put("edit_btn", "Y");
			}
		}
	}

	private static long parseSourceId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		try {
			return (long) Double.parseDouble(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/** Raw list rows use empty string for empty {@code grade_ids}/{@code vip_grade_ids} (DB scalar shape). */
	private static void normalizeListRowScalarFields(Map<String, Object> row) {
		row.put("grade_ids", emptyListToEmptyString(row.get("grade_ids")));
		row.put("vip_grade_ids", emptyListToEmptyString(row.get("vip_grade_ids")));
	}

	/** 列表接口中空 {@code tag_ids}/{@code brand_ids} 与库表 null 一致，序列化为 JSON {@code null}。 */
	private static void normalizeListRowEmptyIdListsToNull(Map<String, Object> row) {
		for (String key : new String[] {"tag_ids", "brand_ids"}) {
			Object v = row.get(key);
			if (v instanceof List<?> list && list.isEmpty()) {
				row.put(key, null);
			}
		}
	}

	private static Object emptyListToEmptyString(Object raw) {
		if (raw instanceof List<?> list && list.isEmpty()) {
			return "";
		}
		return raw;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return (long) Double.parseDouble(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
