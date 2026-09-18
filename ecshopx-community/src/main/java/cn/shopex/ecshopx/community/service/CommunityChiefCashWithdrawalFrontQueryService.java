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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.community.domain.CommunityChiefCashWithdrawal;
import cn.shopex.ecshopx.community.mapper.CommunityChiefCashWithdrawalMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CommunityChiefCashWithdrawalFrontQueryService {

	private static final DateTimeFormatter CREATED_DATE_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final CommunityChiefCashWithdrawalMapper communityChiefCashWithdrawalMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final CommunityChiefCashWithdrawalQueryService communityChiefCashWithdrawalQueryService;

	public CommunityChiefCashWithdrawalFrontQueryService(
			CommunityChiefCashWithdrawalMapper communityChiefCashWithdrawalMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			CommunityChiefCashWithdrawalQueryService communityChiefCashWithdrawalQueryService) {
		this.communityChiefCashWithdrawalMapper = communityChiefCashWithdrawalMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.communityChiefCashWithdrawalQueryService = communityChiefCashWithdrawalQueryService;
	}

	public Map<String, Object> listForChiefH5(long companyId, long chiefId, int page, int pageSize) {
		LambdaQueryWrapper<CommunityChiefCashWithdrawal> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.eq(CommunityChiefCashWithdrawal::getCompanyId, companyId)
				.eq(CommunityChiefCashWithdrawal::getChiefId, chiefId)
				.orderByDesc(CommunityChiefCashWithdrawal::getCreated);

		long totalCount = communityChiefCashWithdrawalMapper.selectCount(wrapper);
		List<Map<String, Object>> listOut = new ArrayList<>();
		if (totalCount > 0) {
			Page<CommunityChiefCashWithdrawal> mpPage = new Page<>(page, pageSize, false);
			communityChiefCashWithdrawalMapper.selectPage(mpPage, wrapper);
			for (CommunityChiefCashWithdrawal w : mpPage.getRecords()) {
				listOut.add(toH5WithdrawalLine(w));
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listOut);
		return out;
	}

	public Map<String, Object> buildCashWithdrawalCountForChiefH5(long companyId, long chiefId) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_fee", 0L);
		result.put("rebate_total", 0L);
		result.put("cash_withdrawal_rebate", 0L);
		result.put("payed_rebate", 0L);

		Map<Long, Map<String, Object>> agg =
				communityChiefCashWithdrawalQueryService.buildChiefRebateAggregateByChiefIds(
						companyId, List.of(chiefId));
		Map<String, Object> row = agg.get(chiefId);
		if (row == null) {
			return result;
		}
		result.put("total_fee", longOrZero(row.get("total_fee")));
		result.put("rebate_total", longOrZero(row.get("rebate_total")));
		result.put("cash_withdrawal_rebate", longOrZero(row.get("cash_withdrawal_rebate")));
		result.put("payed_rebate", longOrZero(row.get("payed_rebate")));
		return result;
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

	public Map<String, Object> toH5WithdrawalLine(CommunityChiefCashWithdrawal w) {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("id", w.getId());
		line.put("company_id", w.getCompanyId());
		line.put("distributor_id", w.getDistributorId());
		line.put("chief_id", w.getChiefId());
		line.put("pay_account", w.getPayAccount());
		line.put("account_name", w.getAccountName());
		line.put("bank_name", w.getBankName());
		line.put(
				"mobile",
				w.getMobile() != null ? sensitiveFieldEncryptor.decrypt(w.getMobile()) : null);
		line.put("money", w.getMoney());
		line.put("status", w.getStatus());
		line.put("remarks", w.getRemarks());
		line.put("pay_type", w.getPayType());
		line.put("wxa_appid", w.getWxaAppid());
		line.put("created", w.getCreated());
		line.put("updated", w.getUpdated());
		int createdSec = w.getCreated() != null ? w.getCreated() : 0;
		line.put("created_date", CREATED_DATE_FMT.format(Instant.ofEpochSecond(createdSec)));
		return line;
	}
}
