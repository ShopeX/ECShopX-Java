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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2DiscountCardGetDiscountCardListService {

	private final DiscountCardsMapper discountCardsMapper;
	private final UserDiscountMapper userDiscountMapper;
	private final OpenapiDiscountCardV2ListPresentationService presentationService;

	public OpenapiThirdApiV2DiscountCardGetDiscountCardListService(
			DiscountCardsMapper discountCardsMapper,
			UserDiscountMapper userDiscountMapper,
			OpenapiDiscountCardV2ListPresentationService presentationService) {
		this.discountCardsMapper = discountCardsMapper;
		this.userDiscountMapper = userDiscountMapper;
		this.presentationService = presentationService;
	}

	public Map<String, Object> execute(long companyId, int page, int pageSize) {
		LambdaQueryWrapper<DiscountCards> countWrapper = new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCompanyId, companyId);
		long totalCount = discountCardsMapper.selectCount(countWrapper);

		List<Map<String, Object>> list;
		if (totalCount == 0L) {
			list = new ArrayList<>();
		} else {
			LambdaQueryWrapper<DiscountCards> listWrapper = new LambdaQueryWrapper<DiscountCards>()
					.eq(DiscountCards::getCompanyId, companyId)
					.select(
							DiscountCards::getCardId,
							DiscountCards::getTitle,
							DiscountCards::getDescription,
							DiscountCards::getDiscount,
							DiscountCards::getReduceCost,
							DiscountCards::getCardType,
							DiscountCards::getDateType,
							DiscountCards::getBeginDate,
							DiscountCards::getEndDate,
							DiscountCards::getFixedTerm,
							DiscountCards::getLeastCost,
							DiscountCards::getMostCost,
							DiscountCards::getUseBound,
							DiscountCards::getApplyScope,
							DiscountCards::getQuantity)
					.orderByDesc(DiscountCards::getCreated);

			Page<DiscountCards> pageOut =
					discountCardsMapper.selectPage(new Page<>(page, pageSize, false), listWrapper);
			List<DiscountCards> records = pageOut.getRecords();

			List<Long> cardIds = new ArrayList<>(records.size());
			list = new ArrayList<>(records.size());
			for (DiscountCards entity : records) {
				list.add(toOpenApiListRow(entity));
				if (entity.getCardId() != null) {
					cardIds.add(entity.getCardId());
				}
			}

			Map<Long, Integer> getNums = cardIds.isEmpty()
					? Map.of()
					: toCountMap(userDiscountMapper.countReceivedGroupByCardId(companyId, cardIds));
			for (Map<String, Object> row : list) {
				Long cardId = longValue(row.get("card_id"));
				row.put("get_num", cardId == null ? 0 : getNums.getOrDefault(cardId, 0));
			}
		}

		Map<String, Object> result =
				OpenapiDiscountCardV2ListFormatSupport.formatListStruct(totalCount, list, page, pageSize);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mutableList = (List<Map<String, Object>>) result.get("list");
		presentationService.apply(mutableList);
		return result;
	}

	private static Map<String, Object> toOpenApiListRow(DiscountCards entity) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("card_id", entity.getCardId());
		row.put("title", entity.getTitle());
		row.put("description", entity.getDescription());
		row.put("discount", entity.getDiscount());
		row.put("reduce_cost", entity.getReduceCost());
		row.put("card_type", entity.getCardType());
		row.put("date_type", entity.getDateType());
		row.put("begin_date", entity.getBeginDate());
		row.put("end_date", entity.getEndDate());
		row.put("fixed_term", entity.getFixedTerm());
		row.put("least_cost", entity.getLeastCost());
		row.put("most_cost", entity.getMostCost());
		row.put("use_bound", entity.getUseBound());
		row.put("apply_scope", entity.getApplyScope());
		row.put("quantity", entity.getQuantity());
		return row;
	}

	private static Map<Long, Integer> toCountMap(List<UserDiscountCardAggRow> rows) {
		Map<Long, Integer> counts = new HashMap<>();
		if (rows == null) {
			return counts;
		}
		for (UserDiscountCardAggRow row : rows) {
			if (row.getCardId() != null && row.getNum() != null) {
				counts.put(row.getCardId(), row.getNum().intValue());
			}
		}
		return counts;
	}

	private static Long longValue(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
