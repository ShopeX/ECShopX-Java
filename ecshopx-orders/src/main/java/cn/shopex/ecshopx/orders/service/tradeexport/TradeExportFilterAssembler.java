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

package cn.shopex.ecshopx.orders.service.tradeexport;

import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import cn.shopex.ecshopx.orders.service.admin.support.TradeListTimeStartFromRequest;
import cn.shopex.ecshopx.orders.service.admin.support.TradeListTimeStartParams;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeExportFilterAssembler {

	private final MembersUserIdByMobileLookupService membersUserIdByMobileLookupService;

	public TradeExportFilterAssembler(MembersUserIdByMobileLookupService membersUserIdByMobileLookupService) {
		this.membersUserIdByMobileLookupService = membersUserIdByMobileLookupService;
	}

	public LinkedHashMap<String, Object> assemble(
			long companyId,
			String operatorType,
			Long merchantIdOrNull,
			List<Long> distributorIdsFromJwt,
			List<Long> shopIdsFromJwt,
			HttpServletRequest request) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", Long.valueOf(companyId));

		if (operatorType != null && "merchant".equalsIgnoreCase(operatorType.trim())) {
			if (merchantIdOrNull != null && merchantIdOrNull.longValue() > 0L) {
				filter.put("merchant_id", merchantIdOrNull);
			}
		}

		String statusParam = request.getParameter("status");
		if (StringUtils.hasText(statusParam)) {
			filter.put("trade_state", statusParam.trim().toUpperCase(Locale.ROOT));
		}

		String orderIdParam = request.getParameter("orderId");
		if (StringUtils.hasText(orderIdParam)) {
			filter.put("order_id", orderIdParam.trim());
		}

		String mobileRaw = request.getParameter("mobile");
		if (StringUtils.hasText(mobileRaw)) {
			String m = mobileRaw.trim();
			if (m.length() == 11) {
				Long uid = membersUserIdByMobileLookupService.findUserIdByCompanyAndPlainMobile(companyId, m);
				if (uid != null) {
					filter.put("user_id", String.valueOf(uid));
				} else {
					filter.put("mobile_cipher", LegacyFixedMobileEncrypt.fixedEncryptMobile(m));
				}
			} else {
				filter.put("trade_id_exact", m);
			}
		}

		TradeListTimeStartParams tw = TradeListTimeStartFromRequest.resolve(request);
		if (StringUtils.hasText(tw.timeStartBegin())) {
			filter.put("time_start_begin", tw.timeStartBegin());
			if (StringUtils.hasText(tw.timeStartEnd())) {
				filter.put("time_start_end", tw.timeStartEnd());
			}
		}

		String shopOverride = request.getParameter("shop_id");
		if (StringUtils.hasText(shopOverride)) {
			filter.put("shop_id", shopOverride.trim());
		} else if (shopIdsFromJwt != null && !shopIdsFromJwt.isEmpty()) {
			filter.put("shop_id_in", new ArrayList<>(shopIdsFromJwt.stream().map(String::valueOf).toList()));
		}

		Set<Long> jwtDistributorSet =
				distributorIdsFromJwt == null
						? Set.of()
						: distributorIdsFromJwt.stream()
								.filter(id -> id != null && id.longValue() > 0L)
								.collect(Collectors.toSet());

		Long queryDistributorId = parseQueryDistributorId(request);
		List<String> distributorIdInStrings = null;
		Long distributorEq = null;
		if (!jwtDistributorSet.isEmpty()) {
			if (queryDistributorId != null) {
				if (!jwtDistributorSet.contains(queryDistributorId)) {
					queryDistributorId = null;
				} else {
					distributorEq = queryDistributorId;
				}
			} else {
				distributorIdInStrings =
						jwtDistributorSet.stream().map(String::valueOf).collect(Collectors.toList());
			}
		} else if (queryDistributorId != null) {
			distributorEq = queryDistributorId;
		}
		if (distributorEq != null) {
			filter.put("distributor_eq", distributorEq);
		}
		if (distributorIdInStrings != null && !distributorIdInStrings.isEmpty()) {
			filter.put("distributor_id_in", new ArrayList<>(distributorIdInStrings));
		}

		String orderType = request.getParameter("order_type");
		if (StringUtils.hasText(orderType)) {
			String ot = orderType.trim();
			switch (ot) {
				case "service" ->
					filter.put("trade_source_in", new ArrayList<>(List.of("service", "groups", "seckill")));
				case "normal" ->
					filter.put(
							"trade_source_in",
							new ArrayList<>(List.of("normal", "normal_groups", "normal_seckill", "normal_community")));
				case "diposit" -> filter.put("trade_source_eq", "diposit");
				case "order_pay" -> filter.put("trade_source_eq", "order_pay");
				default -> {
				}
			}
		}

		String dp = request.getHeader("x-datapass-block");
		if (dp != null) {
			filter.put("datapass_block", dp.trim());
		}

		return filter;
	}

	private static Long parseQueryDistributorId(HttpServletRequest request) {
		String dRaw = request.getParameter("distributor_id");
		if (dRaw == null) {
			return null;
		}
		String t = dRaw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			long v = new BigDecimal(t).longValue();
			if (v == 0L) {
				return null;
			}
			return v;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
