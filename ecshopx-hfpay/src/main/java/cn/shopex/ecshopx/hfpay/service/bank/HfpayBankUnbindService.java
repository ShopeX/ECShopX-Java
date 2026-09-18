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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.domain.HfpayBankCard;
import cn.shopex.ecshopx.hfpay.mapper.HfpayBankCardMapper;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayBankUnbindService {

	private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

	private final HfpayBankCardMapper bankCardMapper;
	private final HfPayPaymentSettingService paymentSettingService;
	private final HfPayAcouJsonPostClient acouJsonPostClient;
	private final HfPayOrderApplyIdGenerator orderApplyIdGenerator;

	public HfpayBankUnbindService(
			HfpayBankCardMapper bankCardMapper,
			HfPayPaymentSettingService paymentSettingService,
			HfPayAcouJsonPostClient acouJsonPostClient,
			HfPayOrderApplyIdGenerator orderApplyIdGenerator) {
		this.bankCardMapper = bankCardMapper;
		this.paymentSettingService = paymentSettingService;
		this.acouJsonPostClient = acouJsonPostClient;
		this.orderApplyIdGenerator = orderApplyIdGenerator;
	}

	public boolean unbindBank(long companyId, Long optionalAuthUserId, Map<String, Object> params) {
		Object cardNumRaw = params.get("card_num");
		String cardNum = cardNumRaw == null ? "" : String.valueOf(cardNumRaw).trim();
		if (!StringUtils.hasText(cardNum)) {
			throw new BadRequestException("缺少必填参数: card_num");
		}

		LambdaQueryWrapper<HfpayBankCard> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(HfpayBankCard::getCompanyId, companyId);
		wrapper.eq(HfpayBankCard::getCardNum, cardNum);
		if (optionalAuthUserId != null) {
			wrapper.eq(HfpayBankCard::getUserId, optionalAuthUserId);
		}

		Object distRaw = params.get("distributor_id");
		if (distRaw != null) {
			String distStr = String.valueOf(distRaw).trim();
			if (StringUtils.hasText(distStr)) {
				long parsed;
				try {
					parsed = Long.parseLong(distStr);
				} catch (NumberFormatException e) {
					throw new BadRequestException("distributor_id 格式错误，须为有效整数");
				}
				wrapper.eq(HfpayBankCard::getDistributorId, parsed);
			}
		}

		HfpayBankCard card = bankCardMapper.selectOne(wrapper);
		if (card == null) {
			return false;
		}

		boolean result = false;
		if (StringUtils.hasText(card.getBindCardId())) {
			Map<String, Object> setting = paymentSettingService.loadForCompany(companyId);
			LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
			payload.put("version", 10);
			payload.put("mer_cust_id", String.valueOf(setting.get("mer_cust_id")).trim());
			payload.put("order_date", LocalDate.now(ZoneId.systemDefault()).format(DAY));
			payload.put("order_id", orderApplyIdGenerator.nextOrderId());
			payload.put("bind_card_id", card.getBindCardId());
			payload.put("card_buss_type", 0);
			if (!StringUtils.hasText(card.getUserCustId())) {
				payload.put("user_cust_id", card.getUserCustId());
			}
			Map<String, Object> apiResult = acouJsonPostClient.unbd01(setting, payload);
			String respCode = apiResult.get("resp_code") == null ? "" : String.valueOf(apiResult.get("resp_code")).trim();
			if (!"C00000".equals(respCode) && !"C00001".equals(respCode) && !"C00002".equals(respCode)) {
				Object desc = apiResult.get("resp_desc");
				throw new ResourceException(desc == null ? "" : String.valueOf(desc));
			}
		}

		int rows = bankCardMapper.deleteById(card.getHfpayBankCardId());
		result = rows > 0;
		return result;
	}
}
