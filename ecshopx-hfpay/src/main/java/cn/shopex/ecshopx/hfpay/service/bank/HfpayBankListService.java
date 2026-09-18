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

package cn.shopex.ecshopx.hfpay.service.bank;

import cn.shopex.ecshopx.hfpay.domain.HfpayBankCard;
import cn.shopex.ecshopx.hfpay.mapper.HfpayBankCardMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayBankListService {

	private final HfpayBankCardMapper bankCardMapper;

	public HfpayBankListService(HfpayBankCardMapper bankCardMapper) {
		this.bankCardMapper = bankCardMapper;
	}

	public List<Map<String, Object>> listBankCardsAsMaps(long companyId, Long optionalAuthUserId, String distributorIdRaw) {
		LambdaQueryWrapper<HfpayBankCard> w = new LambdaQueryWrapper<>();
		w.eq(HfpayBankCard::getCompanyId, companyId);
		if (optionalAuthUserId != null) {
			w.eq(HfpayBankCard::getUserId, optionalAuthUserId);
		}
		if (distributorIdRaw != null) {
			String trimmed = distributorIdRaw.trim();
			if (StringUtils.hasText(trimmed)) {
				try {
					long distributorIdParsed = Long.parseLong(trimmed);
					w.eq(HfpayBankCard::getDistributorId, distributorIdParsed);
				} catch (NumberFormatException e) {
					// Non-numeric distributor_id yields no matching rows
					w.apply("1 = 0");
				}
			}
		}
		w.orderByDesc(HfpayBankCard::getUpdatedAt).orderByDesc(HfpayBankCard::getHfpayBankCardId);

		List<HfpayBankCard> rows = bankCardMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (HfpayBankCard e : rows) {
			out.add(HfpayBankCardRowConverter.toDetailMap(e));
		}
		return out;
	}
}
