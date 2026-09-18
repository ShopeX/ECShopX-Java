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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.port.systemlink.WdtErpTradeAfterSaleAssemblePort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WdtErpTradeAfterSaleAssemblePortImpl implements WdtErpTradeAfterSaleAssemblePort {

	private final AftersalesMapper aftersalesMapper;

	public WdtErpTradeAfterSaleAssemblePortImpl(AftersalesMapper aftersalesMapper) {
		this.aftersalesMapper = aftersalesMapper;
	}

	@Override
	public List<List<Object>> assembleAftersaleSyncBodies(
			long companyId, Map<String, Object> erpAfterSalePayloadSnakeCase, String wdtShopNo) {
		if (!StringUtils.hasText(wdtShopNo)) {
			return List.of();
		}
		long bn = longOrZero(erpAfterSalePayloadSnakeCase.get("aftersales_bn"));
		if (bn <= 0) {
			return List.of();
		}
		Aftersales a =
				aftersalesMapper.selectOne(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getAftersalesBn, bn)
								.last("LIMIT 1"));
		if (a == null) {
			return List.of();
		}
		Map<String, Object> struct = new LinkedHashMap<>();
		struct.put("tid", String.valueOf(a.getOrderId() == null ? 0L : a.getOrderId()));
		struct.put("refund_no", String.valueOf(a.getAftersalesBn()));
		struct.put("type", mapAftersalesType(a.getAftersalesType()));
		struct.put("status", mapWdtStatus(a.getAftersalesStatus(), a.getProgress()));
		struct.put("refund_amount", fenToYuan(a.getRefundFee()));
		struct.put("reason", a.getReason() != null ? a.getReason() : "");
		struct.put("shop_no", wdtShopNo);
		List<Object> body = new ArrayList<>(2);
		body.add(wdtShopNo);
		body.add(struct);
		return List.of(body);
	}

	private static int mapAftersalesType(String t) {
		if (t == null) {
			return 1;
		}
		if ("ONLY_REFUND".equalsIgnoreCase(t)) {
			return 1;
		}
		if ("RETURN_GOODS".equalsIgnoreCase(t) || "REFUND_GOODS".equalsIgnoreCase(t)) {
			return 2;
		}
		return 1;
	}

	private static int mapWdtStatus(Integer aftersalesStatus, Integer progress) {
		int s = aftersalesStatus == null ? 0 : aftersalesStatus;
		int p = progress == null ? 0 : progress;
		if (s == 3) {
			return 3;
		}
		if (s == 4 || p == 7) {
			return 4;
		}
		if (s == 2 || p == 4 || p == 6) {
			return 2;
		}
		return 1;
	}

	private static double fenToYuan(Integer fen) {
		if (fen == null) {
			return 0d;
		}
		return Math.round(fen / 100.0d * 100d) / 100d;
	}

	private static long longOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
