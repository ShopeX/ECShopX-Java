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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.distribution.domain.CashWithdrawal;
import cn.shopex.ecshopx.distribution.mapper.CashWithdrawalMapper;
import cn.shopex.ecshopx.popularize.dto.PromoterAlipayInfoRow;
import cn.shopex.ecshopx.popularize.service.PromoterAlipayInfoQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CashWithdrawalListCoreService {

	private static final Logger log = LoggerFactory.getLogger(CashWithdrawalListCoreService.class);

	private final CashWithdrawalMapper cashWithdrawalMapper;
	private final DistributorListQueryService distributorListQueryService;
	private final PromoterAlipayInfoQueryService promoterAlipayInfoQueryService;

	public CashWithdrawalListCoreService(
			CashWithdrawalMapper cashWithdrawalMapper,
			DistributorListQueryService distributorListQueryService,
			PromoterAlipayInfoQueryService promoterAlipayInfoQueryService) {
		this.cashWithdrawalMapper = cashWithdrawalMapper;
		this.distributorListQueryService = distributorListQueryService;
		this.promoterAlipayInfoQueryService = promoterAlipayInfoQueryService;
	}

	public Map<String, Object> buildList(Map<String, Object> user, Map<String, Object> merged) {
		long companyId = longOf(user.get("company_id"));
		LambdaQueryWrapper<CashWithdrawal> w = new LambdaQueryWrapper<>();
		w.eq(CashWithdrawal::getCompanyId, companyId);

		String operatorType = user.get("operator_type") != null ? user.get("operator_type").toString() : "";
		Map<String, Object> filterDebug = new LinkedHashMap<>();
		filterDebug.put("company_id", companyId);
		filterDebug.put("operator_type", operatorType);

		if ("merchant".equals(operatorType)) {
			long merchantId = longOfDefault(user.get("merchant_id"));
			List<Long> shopIds = distributorListQueryService.listValidDistributorIdsForMerchantCapped(companyId, merchantId, 10000);
			List<String> shopIdStrs = shopIds.stream().map(String::valueOf).toList();
			filterDebug.put("distributor_id_in_count", shopIdStrs.size());
			if (shopIdStrs.isEmpty()) {
				log.debug("cash withdrawal list filter: {}", filterDebug);
				log.debug("cash withdrawal list filter: {}", filterDebug);
				return Map.of("list", List.of(), "total_count", 0);
			}
			w.in(CashWithdrawal::getDistributorId, shopIdStrs);
		} else {
			long queryDid = CashWithdrawalListSupport.parseDistributorIdFilter(merged.get("distributor_id"));
			if (queryDid > 0) {
				w.eq(CashWithdrawal::getDistributorId, String.valueOf(queryDid));
				filterDebug.put("distributor_id", queryDid);
			}
		}

		if (StringUtils.hasText((String) merged.get("mobile"))) {
			String m = merged.get("mobile").toString().trim();
			w.eq(CashWithdrawal::getDistributorMobile, m);
			filterDebug.put("mobile", m);
		}

		if (Boolean.TRUE.equals(merged.get("statusQueryPresent"))
				&& StringUtils.hasText((String) merged.get("status"))) {
			String st = merged.get("status").toString().trim();
			w.eq(CashWithdrawal::getStatus, st);
			filterDebug.put("status", st);
		}

		log.debug("cash withdrawal list filter: {}", filterDebug);

		int page = ((Number) merged.get("page")).intValue();
		int pageSize = ((Number) merged.get("pageSize")).intValue();
		Page<CashWithdrawal> p = new Page<>(page, pageSize);
		cashWithdrawalMapper.selectPage(p, w.orderByDesc(CashWithdrawal::getCreated));
		int totalCount = (int) p.getTotal();

		log.debug("cash withdrawal list filter: {}", filterDebug);

		List<Map<String, Object>> list = new ArrayList<>();
		for (CashWithdrawal cw : p.getRecords()) {
			list.add(toRow(cw));
		}

		List<String> promoterUserIds = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Object uid = row.get("user_id");
			if (uid != null && StringUtils.hasText(uid.toString())) {
				promoterUserIds.add(uid.toString());
			}
		}

		Map<String, PromoterAlipayInfoRow> promoMap =
				promoterAlipayInfoQueryService.mapAlipayByCompanyAndUserIds(companyId, promoterUserIds);
		for (Map<String, Object> row : list) {
			Object uid = row.get("user_id");
			String key = uid != null ? uid.toString() : "";
			PromoterAlipayInfoRow pr = StringUtils.hasText(key) ? promoMap.get(key) : null;
			if (pr != null) {
				row.put("alipay_name", pr.getAlipayName() != null ? pr.getAlipayName() : "");
				row.put("alipay_account", pr.getAlipayAccount() != null ? pr.getAlipayAccount() : "");
			} else {
				row.put("alipay_name", "");
				row.put("alipay_account", "");
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		data.put("total_count", totalCount);
		return data;
	}

	private static Map<String, Object> toRow(CashWithdrawal cw) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", cw.getId());
		row.put("company_id", cw.getCompanyId());
		row.put("distributor_id", cw.getDistributorId());
		row.put("distributor_name", cw.getDistributorName());
		row.put("open_id", cw.getOpenId());
		row.put("user_id", cw.getUserId());
		row.put("distributor_mobile", cw.getDistributorMobile());
		row.put("money", cw.getMoney());
		row.put("status", cw.getStatus());
		row.put("remarks", cw.getRemarks());
		row.put("wxa_appid", cw.getWxaAppid());
		row.put("shop_id", cw.getShopId());
		row.put("created", cw.getCreated());
		row.put("updated", cw.getUpdated());
		return row;
	}

	private static long longOf(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static long longOfDefault(Object o) {
		if (o == null) {
			return 0L;
		}
		return longOf(o);
	}
}
