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
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardEffectiveListService {

	private static final String DATE_TYPE_FIX_TERM = "DATE_TYPE_FIX_TERM";
	private static final String DATE_TYPE_FIX_TIME_RANGE = "DATE_TYPE_FIX_TIME_RANGE";

	private final DiscountCardsMapper discountCardsMapper;
	private final DiscountCardsRowMapperService rowMapperService;
	private final DiscountCardsMultiLangReadService multiLangReadService;
	private final DiscountCardSerializedFieldDecodeService serializedFieldDecodeService;

	public DiscountCardEffectiveListService(
			DiscountCardsMapper discountCardsMapper,
			DiscountCardsRowMapperService rowMapperService,
			DiscountCardsMultiLangReadService multiLangReadService,
			DiscountCardSerializedFieldDecodeService serializedFieldDecodeService) {
		this.discountCardsMapper = discountCardsMapper;
		this.rowMapperService = rowMapperService;
		this.multiLangReadService = multiLangReadService;
		this.serializedFieldDecodeService = serializedFieldDecodeService;
	}

	public Map<String, Object> query(long companyId, long sourceId, String cardTypeParam, int pageNo, int pageSize) {
		pageNo = Math.max(1, pageNo);
		if (pageSize < 1) {
			pageSize = 20;
		}
		long nowEpoch = Instant.now().getEpochSecond();
		int endDateFilterInt = (int) Math.min(nowEpoch, Integer.MAX_VALUE);
		List<String> fixTermAndLong = List.of(DATE_TYPE_FIX_TERM, DiscountCardActionValidationService.DATE_TYPE_LONG);

		LambdaQueryWrapper<DiscountCards> countWrapper = buildBaseWrapper(
				companyId, sourceId, cardTypeParam, endDateFilterInt, fixTermAndLong);
		long total = discountCardsMapper.selectCount(countWrapper);

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("total_count", total);
		if (total == 0L) {
			res.put("list", List.of());
			return res;
		}

		LambdaQueryWrapper<DiscountCards> pageWrapper = buildBaseWrapper(
				companyId, sourceId, cardTypeParam, endDateFilterInt, fixTermAndLong);
		pageWrapper.orderByDesc(DiscountCards::getCreated);
		Page<DiscountCards> page = new Page<>(pageNo, pageSize, false);
		Page<DiscountCards> result = discountCardsMapper.selectPage(page, pageWrapper);

		List<Map<String, Object>> list = new ArrayList<>();
		for (DiscountCards entity : result.getRecords()) {
			Map<String, Object> row = rowMapperService.toSnakeCaseMap(entity);
			Object cardIdObj = row.get("card_id");
			if (cardIdObj instanceof Number n) {
				multiLangReadService.overlay(companyId, n.longValue(), row);
			}
			applySerializedListFields(entity, row);
			applyListRowPresentation(row);
			list.add(row);
		}
		res.put("list", list);
		return res;
	}

	private static LambdaQueryWrapper<DiscountCards> buildBaseWrapper(
			long companyId,
			long sourceId,
			String cardTypeParam,
			int endDateFilterInt,
			List<String> fixTermAndLong) {
		LambdaQueryWrapper<DiscountCards> w = new LambdaQueryWrapper<>();
		w.eq(DiscountCards::getCompanyId, companyId);
		w.eq(DiscountCards::getSourceId, sourceId);
		if (cardTypeParam != null && !"all".equals(cardTypeParam)) {
			w.eq(DiscountCards::getCardType, cardTypeParam);
		}
		w.and(inner -> inner
				.nested(n -> n.in(DiscountCards::getDateType, fixTermAndLong).eq(DiscountCards::getEndDate, 0))
				.or()
				.gt(DiscountCards::getEndDate, endDateFilterInt));
		return w;
	}

	private void applySerializedListFields(DiscountCards entity, Map<String, Object> row) {
		row.put("text_image_list", serializedFieldDecodeService.decodeTextImageList(entity.getTextImageList()));
		String timeLimitRaw = entity.getTimeLimit();
		if (StringUtils.hasText(timeLimitRaw)) {
			List<Map<String, Object>> decodedTl = serializedFieldDecodeService.decodeTimeLimit(timeLimitRaw);
			row.put("time_limit", decodedTl.isEmpty() ? null : decodedTl);
		} else {
			row.put("time_limit", null);
		}
	}

	private static void applyListRowPresentation(Map<String, Object> row) {
		String dt = String.valueOf(row.get("date_type"));
		if (DATE_TYPE_FIX_TIME_RANGE.equals(dt) || DiscountCardActionValidationService.DATE_TYPE_SHORT.equals(dt)) {
			row.put("begin_time", row.get("begin_date"));
			row.put("end_time", row.get("end_date"));
		} else if (DATE_TYPE_FIX_TERM.equals(dt) || DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dt)) {
			Object bd = row.get("begin_date");
			boolean beginIsZeroOrUnset = bd == null
					|| (bd instanceof Number n && n.intValue() == 0)
					|| "0".equals(String.valueOf(bd));
			String beginPart = beginIsZeroOrUnset ? "当" : String.valueOf(bd);
			Object ft = row.get("fixed_term");
			String ftPart = ft == null ? "" : String.valueOf(ft);
			row.put("takeEffect", "领取后" + beginPart + "天生效," + ftPart + "天有效");
		}
		row.put("operationType", "increase");
		row.put("storeValue", 1);
		row.put("storePop", false);
	}
}
