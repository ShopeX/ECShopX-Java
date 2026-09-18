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

package cn.shopex.ecshopx.deposit.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DepositTradeListService {

	private final DepositTradeMapper depositTradeMapper;

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public DepositTradeListService(
			DepositTradeMapper depositTradeMapper, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.depositTradeMapper = depositTradeMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> getDepositTradeListPage(DepositTradeListQuery q, int pageSize, int page) {
		boolean dateBeginSet =
				q.getDateBegin() != null && !q.getDateBegin().trim().isEmpty();
		boolean dateEndSet =
				q.getDateEnd() != null && !q.getDateEnd().trim().isEmpty();
		if (dateBeginSet && !dateEndSet) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", new ArrayList<>());
			return empty;
		}

		LambdaQueryWrapper<DepositTrade> w = new LambdaQueryWrapper<>();
		w.eq(DepositTrade::getCompanyId, q.getCompanyId());
		w.eq(DepositTrade::getTradeStatus, "SUCCESS");
		if (q.isMobileIs11DigitPhone()) {
			w.eq(DepositTrade::getMobile, sensitiveFieldEncryptor.encrypt(q.getMobileOrTradeIdRaw().trim()));
		} else if (q.getMobileOrTradeIdRaw() != null) {
			w.eq(DepositTrade::getDepositTradeId, q.getMobileOrTradeIdRaw().trim());
		}
		if (q.getUserId() != null && !q.getUserId().trim().isEmpty()) {
			w.eq(DepositTrade::getUserId, q.getUserId().trim());
		}
		if (q.getShopName() != null && !q.getShopName().trim().isEmpty()) {
			w.eq(DepositTrade::getShopName, q.getShopName().trim());
		}
		if (dateBeginSet) {
			w.ge(DepositTrade::getTimeStart, q.getDateBegin().trim());
			if (dateEndSet) {
				w.le(DepositTrade::getTimeStart, q.getDateEnd().trim());
			}
		}
		if (q.getTradeTypes() != null && !q.getTradeTypes().isEmpty()) {
			w.in(DepositTrade::getTradeType, q.getTradeTypes());
		}
		w.orderByDesc(DepositTrade::getTimeStart);

		Page<DepositTrade> p = new Page<>(page, pageSize);
		depositTradeMapper.selectPage(p, w);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", p.getTotal());
		List<Map<String, Object>> rows = new ArrayList<>();
		for (DepositTrade e : p.getRecords()) {
			rows.add(toRow(e));
		}
		out.put("list", rows);
		return out;
	}

	public Map<String, Object> getWxappMemberDepositTradeListPage(
			String companyId,
			String userId,
			String outinType,
			int pageSize,
			int page) {
		LambdaQueryWrapper<DepositTrade> w = new LambdaQueryWrapper<>();
		w.eq(DepositTrade::getCompanyId, companyId);
		w.eq(DepositTrade::getUserId, userId);
		w.eq(DepositTrade::getTradeStatus, "SUCCESS");
		if (StringUtils.hasText(outinType)) {
			String t = outinType.trim();
			if ("outcome".equals(t)) {
				w.eq(DepositTrade::getTradeType, "consume");
			} else {
				w.ne(DepositTrade::getTradeType, "consume");
			}
		}
		w.orderByDesc(DepositTrade::getTimeStart);

		Page<DepositTrade> p = new Page<>(page, pageSize);
		depositTradeMapper.selectPage(p, w);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", p.getTotal());
		List<Map<String, Object>> rows = new ArrayList<>();
		for (DepositTrade e : p.getRecords()) {
			rows.add(toRow(e));
		}
		out.put("list", rows);
		return out;
	}

	private Map<String, Object> toRow(DepositTrade e) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("depositTradeId", e.getDepositTradeId());
		row.put("companyId", e.getCompanyId());
		row.put("memberCardCode", e.getMemberCardCode());
		row.put("shopId", e.getShopId());
		row.put("shopName", e.getShopName());
		row.put("userId", e.getUserId());
		String encMobile = e.getMobile();
		row.put("mobile", encMobile == null ? null : sensitiveFieldEncryptor.decrypt(encMobile));
		row.put("openId", e.getOpenId());
		row.put("money", e.getMoney());
		row.put("tradeType", e.getTradeType());
		row.put("authorizerAppid", e.getAuthorizerAppid());
		row.put("wxaAppid", e.getWxaAppid());
		row.put("detail", e.getDetail());
		row.put("timeStart", e.getTimeStart());
		row.put("timeExpire", e.getTimeExpire());
		row.put("tradeStatus", e.getTradeStatus());
		row.put("transactionId", e.getTransactionId());
		row.put("bankType", e.getBankType());
		row.put("rechargeRuleId", e.getRechargeRuleId());
		row.put("payType", e.getPayType());
		row.put("feeType", e.getFeeType());
		row.put("curFeeType", e.getCurFeeType());
		row.put("curFeeRate", e.getCurFeeRate());
		row.put("curFeeSymbol", e.getCurFeeSymbol());
		row.put("curPayFee", e.getCurPayFee());
		return row;
	}
}
