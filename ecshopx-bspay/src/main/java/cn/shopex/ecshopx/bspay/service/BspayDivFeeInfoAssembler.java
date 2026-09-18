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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.domain.DivFee;
import cn.shopex.ecshopx.bspay.mapper.DivFeeMapper;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.mapper.MerchantMapper;
import cn.shopex.ecshopx.orders.domain.Trade;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BspayDivFeeInfoAssembler {

	private final DivFeeMapper divFeeMapper;
	private final MerchantMapper merchantMapper;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	public BspayDivFeeInfoAssembler(
			DivFeeMapper divFeeMapper,
			MerchantMapper merchantMapper,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService) {
		this.divFeeMapper = divFeeMapper;
		this.merchantMapper = merchantMapper;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
	}

	public Map<String, Object> build(Trade trade, Map<String, Object> jwtMap) {
		LambdaQueryWrapper<DivFee> w = new LambdaQueryWrapper<DivFee>()
				.eq(DivFee::getTradeId, trade.getTradeId())
				.orderByAsc(DivFee::getId);
		String operatorType = (jwtMap.get("operator_type") == null) ? "" : jwtMap.get("operator_type").toString();
		if (!"admin".equals(operatorType)) {
			w.eq(DivFee::getOperatorType, operatorType);
		}
		List<DivFee> rows = divFeeMapper.selectList(w);

		if (rows.isEmpty()) {
			return Map.of("total_div_fee", 0, "list", Collections.emptyList());
		}

		Long sumVal = divFeeMapper.sumDivFeeByTradeId(trade.getTradeId());
		int totalDivFee = sumVal == null ? 0 : sumVal.intValue();

		List<Map<String, Object>> innerSnakes = divFeeRowsAsSnakeMaps(rows);
		LinkedHashMap<String, Object> listsShape = new LinkedHashMap<>();
		listsShape.put("total_count", (long) rows.size());
		listsShape.put("list", innerSnakes);

		DivFee first = rows.get(0);
		String distributorIdRaw = first.getDistributorId();
		long tradeCompanyId = parseCompanyIdLong(trade.getCompanyId());
		Object info = distributorRepositoryGetInfoSimpleService.getInfoSimple(tradeCompanyId,
				distributorIdRaw == null ? "" : distributorIdRaw);
		String distributorDisplayName = "自营";
		if (info instanceof Map<?, ?> map) {
			Object name = map.get("name");
			if (name != null && !name.toString().isEmpty()) {
				distributorDisplayName = name.toString();
			}
		}

		List<Map<String, Object>> decoratedInnerList = new ArrayList<>(innerSnakes.size());
		for (Map<String, Object> orig : innerSnakes) {
			Map<String, Object> copy = new LinkedHashMap<>(orig);
			String op = copy.get("operator_type") == null ? "" : copy.get("operator_type").toString();
			if ("admin".equals(op)) {
				copy.put("username", "总部");
			} else if ("merchant".equals(op)) {
				Merchant m = merchantMapper.selectById(trade.getMerchantId());
				copy.put("username",
						(m != null && m.getMerchantName() != null) ? m.getMerchantName() : "");
			} else if ("distributor".equals(op)) {
				copy.put("username", distributorDisplayName);
			}
			decoratedInnerList.add(copy);
		}
		listsShape.put("list", decoratedInnerList);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_div_fee", totalDivFee);
		out.put("created", first.getCreated());
		out.put("list", listsShape);
		return out;
	}

	private static List<Map<String, Object>> divFeeRowsAsSnakeMaps(List<DivFee> rows) {
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (DivFee row : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", row.getId());
			m.put("trade_id", row.getTradeId());
			m.put("order_id", row.getOrderId());
			m.put("company_id", row.getCompanyId());
			m.put("distributor_id", row.getDistributorId());
			m.put("supplier_id", row.getSupplierId() != null ? row.getSupplierId() : 0L);
			m.put("operator_type", row.getOperatorType());
			m.put("pay_fee", row.getPayFee() != null ? row.getPayFee() : 0);
			m.put("div_fee", row.getDivFee() != null ? row.getDivFee() : 0);
			m.put("huifu_id", row.getHuifuId());
			m.put("merchant_id", row.getMerchantId());
			m.put("created", row.getCreated());
			m.put("updated", row.getUpdated());
			out.add(m);
		}
		return out;
	}

	private static long parseCompanyIdLong(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
