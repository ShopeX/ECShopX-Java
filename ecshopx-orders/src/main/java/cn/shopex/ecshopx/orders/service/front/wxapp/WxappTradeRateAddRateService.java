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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappTradeRateAddRateService {

	private static final Pattern NUMERIC_KEY = Pattern.compile("^[0-9]+$");

	private final NormalOrdersMapper normalOrdersMapper;

	private final WxappTradeRateAddRateTxService wxappTradeRateAddRateTxService;

	public WxappTradeRateAddRateService(
			NormalOrdersMapper normalOrdersMapper,
			WxappTradeRateAddRateTxService wxappTradeRateAddRateTxService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.wxappTradeRateAddRateTxService = wxappTradeRateAddRateTxService;
	}

	public Object addRate(Map<String, Object> merged, Map<String, Object> auth) {
		long companyId = longVal(auth.get("company_id"));
		long userId = longVal(auth.get("user_id"));
		if (companyId <= 0L || userId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		String unionid =
				auth.get("unionid") == null ? null : String.valueOf(auth.get("unionid")).trim();

		Object orderIdRaw = merged.get("order_id");
		if (orderIdRaw == null || !StringUtils.hasText(String.valueOf(orderIdRaw).trim())) {
			throw new BadRequestException("order_id 必填");
		}

		boolean anonymous = parseAnonymous(merged.get("anonymous"));
		List<WxappTradeRateAddRateTxService.RateLine> normalizedRates = normalizeRates(merged.get("rates"));

		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(String.valueOf(orderIdRaw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("order_id 格式错误");
		}

		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderIdNum));
		if (order == null) {
			throw new ResourceException("订单不存在");
		}

		return wxappTradeRateAddRateTxService.addRate(
				order, orderIdNum, companyId, userId, unionid, anonymous, normalizedRates);
	}

	private static boolean parseAnonymous(Object a) {
		if (a == null) {
			return false;
		}
		if (a instanceof Boolean b) {
			return b;
		}
		if (a instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = String.valueOf(a).trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static List<WxappTradeRateAddRateTxService.RateLine> normalizeRates(Object raw) {
		if (raw == null) {
			throw new BadRequestException("rates 不能为空");
		}
		List<Object> segments = normalizeRateSegments(raw);
		if (segments.isEmpty()) {
			throw new BadRequestException("rates 不能为空");
		}
		List<WxappTradeRateAddRateTxService.RateLine> out = new ArrayList<>();
		for (Object seg : segments) {
			if (!(seg instanceof Map<?, ?> m)) {
				throw new BadRequestException("rates 格式错误");
			}
			out.add(parseRateLine(m));
		}
		return out;
	}

	private static List<Object> normalizeRateSegments(Object raw) {
		if (raw instanceof List<?> list) {
			return new ArrayList<>(list);
		}
		if (raw instanceof Object[] arr) {
			return new ArrayList<>(Arrays.asList(arr));
		}
		if (raw instanceof Map<?, ?> m) {
			if (m.isEmpty()) {
				return Collections.emptyList();
			}
			List<Map.Entry<Integer, Object>> indexed = new ArrayList<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				String keyStr = String.valueOf(e.getKey()).trim();
				if (!NUMERIC_KEY.matcher(keyStr).matches()) {
					throw new BadRequestException("rates 格式错误");
				}
				if (!(e.getValue() instanceof Map<?, ?>)) {
					throw new BadRequestException("rates 格式错误");
				}
				indexed.add(Map.entry(Integer.parseInt(keyStr), e.getValue()));
			}
			indexed.sort(Comparator.comparingInt(Map.Entry::getKey));
			List<Object> segments = new ArrayList<>();
			for (Map.Entry<Integer, Object> e : indexed) {
				segments.add(e.getValue());
			}
			return segments;
		}
		throw new BadRequestException("rates 格式错误");
	}

	private static WxappTradeRateAddRateTxService.RateLine parseRateLine(Map<?, ?> m) {
		Object itemIdRaw = m.get("item_id");
		long itemId = parsePositiveLongStrict(itemIdRaw, "item_id");
		if (itemId < 1L) {
			throw new BadRequestException("item_id 无效");
		}

		Object contentRaw = m.get("content");
		if (contentRaw == null || !StringUtils.hasText(String.valueOf(contentRaw).trim())) {
			throw new BadRequestException("content 不能为空");
		}
		String content = String.valueOf(contentRaw).trim();

		int star = parseStar(m.get("star"));
		List<String> pics = parsePics(m.get("pics"));

		return new WxappTradeRateAddRateTxService.RateLine(itemId, content, star, pics);
	}

	private static int parseStar(Object raw) {
		if (raw == null) {
			throw new BadRequestException("star 无效");
		}
		try {
			return new BigDecimal(raw.toString().trim())
					.setScale(0, RoundingMode.UNNECESSARY)
					.intValueExact();
		} catch (ArithmeticException | NumberFormatException e) {
			throw new BadRequestException("star 无效");
		}
	}

	private static List<String> parsePics(Object raw) {
		if (raw == null) {
			return Collections.emptyList();
		}
		if (raw instanceof String s) {
			return List.of(s);
		}
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				out.add(String.valueOf(o));
			}
			return out;
		}
		throw new BadRequestException("pics 格式错误");
	}

	private static long parsePositiveLongStrict(Object raw, String field) {
		if (raw == null) {
			throw new BadRequestException(field + " 无效");
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(field + " 无效");
		}
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
