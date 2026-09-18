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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v1;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardActionValidationService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardParamNormalize;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardSerializedFieldDecodeService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsMultiLangReadService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsRowMapperService;
import cn.shopex.ecshopx.kaquan.service.discount.KaquanCardDateStatusService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV1DiscountCardGetDiscountCardListService {

	private static final String DATE_TYPE_FIX_TERM = "DATE_TYPE_FIX_TERM";

	private final DiscountCardsMapper discountCardsMapper;
	private final UserDiscountMapper userDiscountMapper;
	private final OpenapiDiscountCardListFilterBuilder filterBuilder;
	private final DiscountCardsRowMapperService rowMapperService;
	private final DiscountCardSerializedFieldDecodeService decodeService;
	private final DiscountCardsMultiLangReadService multiLangReadService;
	private final OpenapiDiscountCardListPresentationService presentationService;

	public OpenapiThirdApiV1DiscountCardGetDiscountCardListService(
			DiscountCardsMapper discountCardsMapper,
			UserDiscountMapper userDiscountMapper,
			OpenapiDiscountCardListFilterBuilder filterBuilder,
			DiscountCardsRowMapperService rowMapperService,
			DiscountCardSerializedFieldDecodeService decodeService,
			DiscountCardsMultiLangReadService multiLangReadService,
			OpenapiDiscountCardListPresentationService presentationService) {
		this.discountCardsMapper = discountCardsMapper;
		this.userDiscountMapper = userDiscountMapper;
		this.filterBuilder = filterBuilder;
		this.rowMapperService = rowMapperService;
		this.decodeService = decodeService;
		this.multiLangReadService = multiLangReadService;
		this.presentationService = presentationService;
	}

	public Map<String, Object> execute(long companyId, Map<String, Object> merged) {
		int page = DiscountCardParamNormalize.parseIntFlexible(merged.get("page"), 1);
		int pageSize = DiscountCardParamNormalize.parseIntFlexible(merged.get("pageSize"), 20);
		long nowEpoch = Instant.now().getEpochSecond();

		LambdaQueryWrapper<DiscountCards> wrapper = filterBuilder.build(companyId, merged, nowEpoch);
		long total = discountCardsMapper.selectCount(wrapper);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);

		List<Map<String, Object>> list;
		if (total == 0L) {
			list = new ArrayList<>();
		} else {
			wrapper.orderByDesc(DiscountCards::getCreated);
			Page<DiscountCards> pageOut = discountCardsMapper.selectPage(new Page<>(page, pageSize, false), wrapper);
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
			list = new ArrayList<>(records.size());
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
				if (DATE_TYPE_FIX_TERM.equals(dateType)
						|| DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dateType)) {
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
		}

		presentationService.apply(out, list);
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

	private static void normalizeListRowScalarFields(Map<String, Object> row) {
		row.put("grade_ids", emptyListToEmptyString(row.get("grade_ids")));
		row.put("vip_grade_ids", emptyListToEmptyString(row.get("vip_grade_ids")));
	}

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
}
