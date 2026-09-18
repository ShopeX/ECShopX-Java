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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2TradeListService {

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final TradeMapper tradeMapper;

	public OpenapiThirdApiV2TradeListService(TradeMapper tradeMapper) {
		this.tradeMapper = tradeMapper;
	}

	public Map<String, Object> list(long companyId, int page, int pageSize) {
		String companyIdStr = String.valueOf(companyId);

		LambdaQueryWrapper<Trade> countWrapper = new LambdaQueryWrapper<>();
		countWrapper.eq(Trade::getCompanyId, companyIdStr);
		Long total = tradeMapper.selectCount(countWrapper);
		long totalCount = total != null ? total : 0L;

		long offset = (long) (page - 1) * pageSize;
		LambdaQueryWrapper<Trade> listWrapper = new LambdaQueryWrapper<>();
		listWrapper
				.eq(Trade::getCompanyId, companyIdStr)
				.orderByDesc(Trade::getTimeStart)
				.last("LIMIT " + pageSize + " OFFSET " + offset);
		List<Trade> entities = tradeMapper.selectList(listWrapper);

		List<Map<String, Object>> formattedRows =
				entities.stream().map(this::formatOpenApiTradeRow).toList();
		return formatListStruct(totalCount, formattedRows, page, pageSize);
	}

	private Map<String, Object> formatOpenApiTradeRow(Trade t) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trade_id", t.getTradeId());
		row.put("order_id", t.getOrderId());
		row.put("mch_id", t.getMchId());
		row.put("total_fee", t.getTotalFee());
		row.put("discount_fee", t.getDiscountFee());
		row.put("fee_type", t.getFeeType());
		row.put("pay_fee", t.getPayFee());
		row.put("trade_state", t.getTradeState());
		row.put("pay_type", t.getPayType());
		row.put("time_start", formatPhpDate(t.getTimeStart()));
		row.put("time_expire", formatPhpDate(t.getTimeExpire()));
		return row;
	}

	private static String formatPhpDate(String raw) {
		if (raw == null || raw.isBlank()) {
			return DATE_TIME_FMT.format(Instant.ofEpochSecond(0L));
		}
		String s = raw.trim();
		if (!isAsciiDigitsOnly(s)) {
			return DATE_TIME_FMT.format(Instant.ofEpochSecond(0L));
		}
		try {
			long n = Long.parseLong(s);
			Instant instant;
			if (s.length() >= 13) {
				instant = Instant.ofEpochMilli(n);
			} else {
				instant = Instant.ofEpochSecond(n);
			}
			return DATE_TIME_FMT.format(instant);
		} catch (DateTimeException | NumberFormatException | ArithmeticException e) {
			return DATE_TIME_FMT.format(Instant.ofEpochSecond(0L));
		}
	}

	private static boolean isAsciiDigitsOnly(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	private static Map<String, Object> formatListStruct(
			long totalCount, List<Map<String, Object>> list, int page, int pageSize) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("is_last_page", computeIsLastPage(totalCount, page, pageSize));
		result.put("pager", Map.of("page", page, "page_size", pageSize));
		result.put("list", list != null ? list : List.of());
		return result;
	}

	private static int computeIsLastPage(long totalCount, int page, int pageSize) {
		if (pageSize <= 0) {
			return 1;
		}
		long totalPage = (long) Math.ceil((double) totalCount / pageSize);
		return totalPage <= page ? 1 : 0;
	}
}
