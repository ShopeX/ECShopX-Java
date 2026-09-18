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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.hfpay.domain.HfpayBankCard;
import cn.shopex.ecshopx.hfpay.mapper.HfpayBankCardMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayBankGetInfoService {

	private final HfpayBankCardMapper bankCardMapper;

	public HfpayBankGetInfoService(HfpayBankCardMapper bankCardMapper) {
		this.bankCardMapper = bankCardMapper;
	}

	public Object getBankCardAsResponseData(long companyId, Long optionalAuthUserId, String distributorIdRaw) {
		LambdaQueryWrapper<HfpayBankCard> w = new LambdaQueryWrapper<>();
		w.eq(HfpayBankCard::getCompanyId, companyId);
		if (optionalAuthUserId != null) {
			w.eq(HfpayBankCard::getUserId, optionalAuthUserId);
		}
		if (distributorIdRaw != null) {
			String trimmed = distributorIdRaw.trim();
			if (StringUtils.hasText(trimmed)) {
				final long distributorIdParsed;
				try {
					distributorIdParsed = Long.parseLong(trimmed);
				} catch (NumberFormatException e) {
					throw new BadRequestException("distributor_id 参数格式错误");
				}
				w.eq(HfpayBankCard::getDistributorId, distributorIdParsed);
			}
		}
		w.orderByDesc(HfpayBankCard::getUpdatedAt).orderByDesc(HfpayBankCard::getHfpayBankCardId);
		w.last("LIMIT 1");

		HfpayBankCard row = bankCardMapper.selectOne(w);
		if (row == null) {
			return Collections.emptyList();
		}
		return HfpayBankCardRowConverter.toDetailMap(row);
	}
}
