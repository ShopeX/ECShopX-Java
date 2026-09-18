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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.community.domain.CommunityChiefCashWithdrawal;
import cn.shopex.ecshopx.community.domain.dto.ChiefRebateAggRow;
import cn.shopex.ecshopx.community.mapper.CommunityChiefCashWithdrawalMapper;
import cn.shopex.ecshopx.goods.web.DatapassBlockResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommunityChiefCashWithdrawalQueryService {

	private static final DateTimeFormatter CREATED_DATE_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final CommunityChiefCashWithdrawalMapper communityChiefCashWithdrawalMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public CommunityChiefCashWithdrawalQueryService(
			CommunityChiefCashWithdrawalMapper communityChiefCashWithdrawalMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.communityChiefCashWithdrawalMapper = communityChiefCashWithdrawalMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> buildListResponse(
			long companyId,
			String operatorType,
			int distributorId,
			String mobile,
			String status,
			int page,
			int pageSize,
			HttpServletRequest request) {
		String mobileEnc = null;
		if (StringUtils.hasText(mobile) && StringUtils.hasText(mobile.trim())) {
			mobileEnc = sensitiveFieldEncryptor.encrypt(mobile.trim());
		}
		String statusFilter = null;
		if (StringUtils.hasText(status) && StringUtils.hasText(status.trim())) {
			statusFilter = status.trim();
		}

		long totalCount =
				communityChiefCashWithdrawalMapper.countWithdrawalList(
						companyId, distributorId, mobileEnc, statusFilter);
		int offset = (page - 1) * pageSize;
		List<CommunityChiefCashWithdrawal> rows =
				communityChiefCashWithdrawalMapper.selectWithdrawalList(
						companyId, distributorId, mobileEnc, statusFilter, offset, pageSize);

		Map<Long, Map<String, Object>> chiefRebateAgg = new HashMap<>();
		if (!rows.isEmpty()) {
			List<Long> chiefIds = new ArrayList<>();
			for (CommunityChiefCashWithdrawal w : rows) {
				if (w.getChiefId() != null) {
					chiefIds.add(w.getChiefId());
				}
			}
			if (!chiefIds.isEmpty()) {
				chiefRebateAgg = buildChiefRebateAggregateByChiefIds(companyId, chiefIds);
			}
		}

		boolean mask = DatapassBlockResolver.isBlocked(request);
		List<Map<String, Object>> listOut = new ArrayList<>();
		for (CommunityChiefCashWithdrawal w : rows) {
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("id", w.getId());
			line.put("company_id", w.getCompanyId());
			line.put("distributor_id", w.getDistributorId());
			line.put("chief_id", w.getChiefId());
			line.put("pay_account", w.getPayAccount());
			line.put("account_name", w.getAccountName());
			line.put("bank_name", w.getBankName());
			String plainMobile =
					w.getMobile() != null ? sensitiveFieldEncryptor.decrypt(w.getMobile()) : null;
			if (mask) {
				line.put("mobile", DataMasking.maskMobile(plainMobile == null ? "" : plainMobile));
			} else {
				line.put("mobile", plainMobile);
			}
			line.put("money", w.getMoney());
			line.put("status", w.getStatus());
			line.put("remarks", w.getRemarks());
			line.put("pay_type", w.getPayType());
			line.put("wxa_appid", w.getWxaAppid());
			line.put("created", w.getCreated());
			line.put("updated", w.getUpdated());
			int createdSec = w.getCreated() != null ? w.getCreated() : 0;
			line.put("created_date", CREATED_DATE_FMT.format(Instant.ofEpochSecond(createdSec)));

			long cashWithdrawalRebate = 0L;
			if (w.getChiefId() != null) {
				Map<String, Object> agg = chiefRebateAgg.get(w.getChiefId());
				if (agg != null) {
					Object v = agg.get("cash_withdrawal_rebate");
					if (v instanceof Number n) {
						cashWithdrawalRebate = n.longValue();
					}
				}
			}
			line.put("cash_withdrawal_rebate", cashWithdrawalRebate);
			listOut.add(line);
		}

		Map<String, Object> countRaw =
				communityChiefCashWithdrawalMapper.selectCashWithdrawalCountAggregates(companyId, distributorId);
		Map<String, Object> countOut = new LinkedHashMap<>();
		if (countRaw != null) {
			countOut.put("rebate_total", countRaw.get("rebate_total"));
			countOut.put("freeze_cash_withdrawal_rebate", countRaw.get("freeze_cash_withdrawal_rebate"));
			countOut.put("payed_rebate", countRaw.get("payed_rebate"));
			countOut.put("apply_chief_num", countRaw.get("apply_chief_num"));
		} else {
			countOut.put("rebate_total", null);
			countOut.put("freeze_cash_withdrawal_rebate", null);
			countOut.put("payed_rebate", null);
			countOut.put("apply_chief_num", null);
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", listOut);
		data.put("count", countOut);
		return data;
	}

	public Map<Long, Map<String, Object>> buildChiefRebateAggregateByChiefIds(long companyId, List<Long> chiefIds) {
		long nowSeconds = Instant.now().getEpochSecond();
		return mergeChiefRebateMaps(companyId, chiefIds, nowSeconds);
	}

	private Map<Long, Map<String, Object>> mergeChiefRebateMaps(
			long companyId, List<Long> chiefIdsOrdered, long nowSeconds) {
		Set<Long> chiefIdSet = chiefIdsOrdered.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		List<Long> chiefIds = new ArrayList<>(chiefIdSet);
		if (chiefIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, Map<String, Object>> byChief = new HashMap<>();
		for (ChiefRebateAggRow row :
				communityChiefCashWithdrawalMapper.selectChiefRebateRebateTotalByChiefIds(companyId, chiefIds)) {
			if (row.getChiefId() == null) {
				continue;
			}
			byChief.computeIfAbsent(row.getChiefId(), k -> new HashMap<>());
			Map<String, Object> m = byChief.get(row.getChiefId());
			m.put("rebate_total", row.getRebateTotal());
			m.put("total_fee", row.getTotalFee());
		}
		for (ChiefRebateAggRow row :
				communityChiefCashWithdrawalMapper.selectChiefRebateCloseRebateByChiefIds(
						companyId, chiefIds, nowSeconds)) {
			if (row.getChiefId() == null) {
				continue;
			}
			byChief.computeIfAbsent(row.getChiefId(), k -> new HashMap<>());
			byChief.get(row.getChiefId()).put("close_rebate", row.getCloseRebate());
		}
		for (ChiefRebateAggRow row :
				communityChiefCashWithdrawalMapper.selectChiefRebateApplyMoneyByChiefIds(companyId, chiefIds)) {
			if (row.getChiefId() == null) {
				continue;
			}
			byChief.computeIfAbsent(row.getChiefId(), k -> new HashMap<>());
			byChief.get(row.getChiefId()).put("freeze_cash_withdrawal_rebate", row.getMoney());
		}
		for (ChiefRebateAggRow row :
				communityChiefCashWithdrawalMapper.selectChiefRebateSuccessMoneyByChiefIds(companyId, chiefIds)) {
			if (row.getChiefId() == null) {
				continue;
			}
			byChief.computeIfAbsent(row.getChiefId(), k -> new HashMap<>());
			byChief.get(row.getChiefId()).put("payed_rebate", row.getMoney());
		}

		for (Map.Entry<Long, Map<String, Object>> e : byChief.entrySet()) {
			Map<String, Object> v = e.getValue();
			long rebateTotal = longOrZero(v.get("rebate_total"));
			long closeRebate = longOrZero(v.get("close_rebate"));
			long freeze = longOrZero(v.get("freeze_cash_withdrawal_rebate"));
			long payed = longOrZero(v.get("payed_rebate"));
			long noCloseRebate = rebateTotal - closeRebate;
			long cashWithdrawalRebate = closeRebate - payed - freeze;
			v.put("no_close_rebate", noCloseRebate);
			v.put("cash_withdrawal_rebate", cashWithdrawalRebate);
		}
		return byChief;
	}

	private static long longOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}
}
