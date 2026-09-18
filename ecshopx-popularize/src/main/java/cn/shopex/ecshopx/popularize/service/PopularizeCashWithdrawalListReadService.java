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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.domain.PromoterCashWithdrawal;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterCashWithdrawalMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PopularizeCashWithdrawalListReadService {

	private static final DateTimeFormatter CREATED_DATE_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

	private final PromoterCashWithdrawalMapper promoterCashWithdrawalMapper;
	private final BrokerageMapper brokerageMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public PopularizeCashWithdrawalListReadService(
			PromoterCashWithdrawalMapper promoterCashWithdrawalMapper,
			BrokerageMapper brokerageMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.promoterCashWithdrawalMapper = promoterCashWithdrawalMapper;
		this.brokerageMapper = brokerageMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> getCashWithdrawalList(
			long companyId,
			String pageRaw,
			String pageSizeRaw,
			String mobileQuery,
			String statusQuery) {
		int page = parseRequiredPositiveInt(pageRaw, "分页参数错误");
		if (page < 1) {
			throw new BadRequestException("分页参数错误");
		}
		int pageSize = parseRequiredPositiveInt(pageSizeRaw, "每页最多查询100条数据");
		if (pageSize < 1 || pageSize > 100) {
			throw new BadRequestException("每页最多查询100条数据");
		}

		LambdaQueryWrapper<PromoterCashWithdrawal> w =
				new LambdaQueryWrapper<PromoterCashWithdrawal>().eq(PromoterCashWithdrawal::getCompanyId, companyId);
		if (mobileQuery != null) {
			String enc = sensitiveFieldEncryptor.encrypt(mobileQuery.trim());
			w.eq(PromoterCashWithdrawal::getMobile, enc);
		}
		if (statusQuery != null) {
			w.eq(PromoterCashWithdrawal::getStatus, statusQuery.trim());
		}
		w.orderByDesc(PromoterCashWithdrawal::getCreated);

		Page<PromoterCashWithdrawal> pg = new Page<>(page, pageSize);
		Page<PromoterCashWithdrawal> result = promoterCashWithdrawalMapper.selectPage(pg, w);
		long total = result.getTotal();

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (PromoterCashWithdrawal e : result.getRecords()) {
			listMaps.add(toListRow(e));
		}

		LinkedHashMap<String, Object> countMap = new LinkedHashMap<>();
		countMap.put("apply", sumMoneyByStatus(companyId, "apply"));
		countMap.put("success", sumMoneyByStatus(companyId, "success"));
		countMap.put("userCount", countDistinctUserId(companyId));
		countMap.put("all", sumClosedBrokerageRebate(companyId));

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", total);
		data.put("list", listMaps);
		data.put("count", countMap);
		return data;
	}

	public Map<String, Object> getCashWithdrawalList(
			long companyId, long promoterUserId, int page, int pageSize) {
		LambdaQueryWrapper<PromoterCashWithdrawal> w =
				new LambdaQueryWrapper<PromoterCashWithdrawal>()
						.eq(PromoterCashWithdrawal::getCompanyId, companyId)
						.eq(PromoterCashWithdrawal::getUserId, String.valueOf(promoterUserId))
						.orderByDesc(PromoterCashWithdrawal::getCreated);

		Page<PromoterCashWithdrawal> result =
				promoterCashWithdrawalMapper.selectPage(new Page<>(page, pageSize), w);
		long total = result.getTotal();

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (PromoterCashWithdrawal e : result.getRecords()) {
			listMaps.add(toListRow(e));
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", total);
		data.put("list", listMaps);
		return data;
	}

	private int parseRequiredPositiveInt(String raw, String onFailureMessage) {
		if (raw == null) {
			throw new BadRequestException(onFailureMessage);
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			throw new BadRequestException(onFailureMessage);
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException(onFailureMessage);
		}
	}

	private Map<String, Object> toListRow(PromoterCashWithdrawal e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("user_id", e.getUserId());
		m.put("pay_account", e.getPayAccount());
		m.put("account_name", e.getAccountName());
		if (e.getMobile() == null) {
			m.put("mobile", null);
		} else {
			m.put("mobile", sensitiveFieldEncryptor.decrypt(e.getMobile()));
		}
		m.put("money", e.getMoney());
		m.put("status", e.getStatus());
		m.put("remarks", e.getRemarks());
		m.put("pay_type", e.getPayType());
		m.put("wxa_appid", e.getWxaAppid() == null ? "" : e.getWxaAppid());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		Integer created = e.getCreated();
		if (created == null) {
			m.put("created_date", null);
		} else {
			Instant inst = Instant.ofEpochSecond(created.longValue());
			m.put("created_date", CREATED_DATE_FMT.format(inst));
		}
		return m;
	}

	private long sumMoneyByStatus(long companyId, String status) {
		QueryWrapper<PromoterCashWithdrawal> q = new QueryWrapper<>();
		q.select("COALESCE(SUM(money),0) AS v")
				.eq("company_id", companyId)
				.eq("status", status);
		return coalesceLongFromSelectMaps(promoterCashWithdrawalMapper.selectMaps(q));
	}

	private long countDistinctUserId(long companyId) {
		QueryWrapper<PromoterCashWithdrawal> q = new QueryWrapper<>();
		q.select("COUNT(DISTINCT user_id) AS v").eq("company_id", companyId);
		return coalesceLongFromSelectMaps(promoterCashWithdrawalMapper.selectMaps(q));
	}

	private long sumClosedBrokerageRebate(long companyId) {
		QueryWrapper<Brokerage> bw = new QueryWrapper<>();
		bw.select("COALESCE(SUM(rebate),0) AS v")
				.eq("company_id", companyId)
				.eq("is_close", Boolean.TRUE);
		List<Map<String, Object>> rows = brokerageMapper.selectMaps(bw);
		return coalesceLongFromSelectMaps(rows);
	}

	private static long coalesceLongFromSelectMaps(List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return 0L;
		}
		Object v = rows.get(0).get("v");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof BigDecimal bd) {
			return bd.longValue();
		}
		return 0L;
	}
}
