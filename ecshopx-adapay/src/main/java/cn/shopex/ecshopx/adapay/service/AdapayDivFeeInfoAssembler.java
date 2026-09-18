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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayDivFee;
import cn.shopex.ecshopx.adapay.mapper.AdapayDivFeeMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.orders.domain.Trade;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AdapayDivFeeInfoAssembler {

	private final AdapayDivFeeMapper adapayDivFeeMapper;
	private final DistributorRepositoryGetInfoSimpleService distributorGetInfoSimpleService;
	private final OperatorsQueryService operatorsQueryService;
	private final AdapayTradeInfoService adapayTradeInfoService;

	public AdapayDivFeeInfoAssembler(
			AdapayDivFeeMapper adapayDivFeeMapper,
			DistributorRepositoryGetInfoSimpleService distributorGetInfoSimpleService,
			OperatorsQueryService operatorsQueryService,
			@Lazy AdapayTradeInfoService adapayTradeInfoService) {
		this.adapayDivFeeMapper = adapayDivFeeMapper;
		this.distributorGetInfoSimpleService = distributorGetInfoSimpleService;
		this.operatorsQueryService = operatorsQueryService;
		this.adapayTradeInfoService = adapayTradeInfoService;
	}

	public Map<String, Object> build(Trade trade, Map<String, Object> jwtMap) {
		long tradeCompanyId = parseCompanyIdLong(trade.getCompanyId());
		LambdaQueryWrapper<AdapayDivFee> w = new LambdaQueryWrapper<AdapayDivFee>()
				.eq(AdapayDivFee::getTradeId, trade.getTradeId());
		String operatorType = jwtMap.get("operator_type") == null ? "" : jwtMap.get("operator_type").toString();
		if (!"admin".equals(operatorType)) {
			w.eq(AdapayDivFee::getOperatorType, operatorType);
		}
		long jwtCompanyId = AdapayTradeInfoService.parseJwtCompanyIdOrThrow(jwtMap);
		w.orderByAsc(AdapayDivFee::getId);
		List<AdapayDivFee> rows = adapayDivFeeMapper.selectList(w);
		if (rows.isEmpty()) {
			return Map.of("total_div_fee", 0, "list", Collections.emptyList());
		}
		Long totalDivFee = adapayDivFeeMapper.sumDivFeeByTradeId(trade.getTradeId());
		int totalDiv = totalDivFee != null ? totalDivFee.intValue() : 0;
		Map<String, Object> listsShape = new LinkedHashMap<>();
		listsShape.put("total_count", (long) rows.size());
		listsShape.put("list", divFeeRowsAsSnakeMaps(rows));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> innerList = (List<Map<String, Object>>) listsShape.get("list");
		Map<String, Object> first = innerList.get(0);
		Object didObj = first.get("distributor_id");
		Object info = distributorGetInfoSimpleService.getInfoSimple(tradeCompanyId,
				didObj == null ? "" : String.valueOf(didObj));
		String distributorDisplayName = "自营";
		if (info instanceof Map<?, ?> m) {
			Object name = m.get("name");
			if (name != null && !name.toString().isEmpty()) {
				distributorDisplayName = name.toString();
			}
		}
		String dealerId = trade.getDealerId() != null ? trade.getDealerId() : "0";
		List<Map<String, Object>> decorated = new ArrayList<>(innerList.size());
		for (Map<String, Object> raw : innerList) {
			Map<String, Object> copy = new LinkedHashMap<>(raw);
			String op = copy.get("operator_type") != null ? copy.get("operator_type").toString() : "";
			switch (op) {
				case "admin" -> copy.put("username", adapayTradeInfoService.resolveMerNameForDivFeeRow(jwtCompanyId));
				case "dealer" -> {
					Map<String, Object> dealerFilter = new LinkedHashMap<>(1);
					dealerFilter.put("operator_id", parseLongOrNull(dealerId));
					Map<String, Object> dealer = operatorsQueryService.getInfo(dealerFilter);
					Object un = dealer != null ? dealer.get("username") : null;
					copy.put("username", un != null ? un.toString() : "");
				}
				case "distributor" -> copy.put("username", distributorDisplayName);
				default -> copy.put("username", "");
			}
			decorated.add(copy);
		}
		listsShape.put("list", decorated);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_div_fee", totalDiv);
		out.put("create_time", first.get("create_time"));
		out.put("list", listsShape);
		return out;
	}

	private static List<Map<String, Object>> divFeeRowsAsSnakeMaps(List<AdapayDivFee> rows) {
		List<Map<String, Object>> list = new ArrayList<>(rows.size());
		for (AdapayDivFee r : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", r.getId());
			m.put("trade_id", r.getTradeId());
			m.put("order_id", r.getOrderId());
			m.put("company_id", r.getCompanyId());
			m.put("distributor_id", r.getDistributorId());
			m.put("operator_type", r.getOperatorType());
			m.put("pay_fee", r.getPayFee() != null ? r.getPayFee() : 0);
			m.put("div_fee", r.getDivFee() != null ? r.getDivFee() : 0);
			m.put("adapay_member_id", r.getAdapayMemberId() != null ? r.getAdapayMemberId() : 0);
			m.put("create_time", r.getCreateTime());
			m.put("update_time", r.getUpdateTime());
			list.add(m);
		}
		return list;
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

	private static Long parseLongOrNull(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
