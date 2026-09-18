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
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyReadService;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HfpayBankSaveService {

	private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

	private final HfpayEnterapplyReadService enterapplyReadService;
	private final HfpayBankCardMapper bankCardMapper;
	private final HfPayPaymentSettingService paymentSettingService;
	private final HfPayAcouJsonPostClient acouJsonPostClient;
	private final HfPayOrderApplyIdGenerator orderApplyIdGenerator;

	public HfpayBankSaveService(
			HfpayEnterapplyReadService enterapplyReadService,
			HfpayBankCardMapper bankCardMapper,
			HfPayPaymentSettingService paymentSettingService,
			HfPayAcouJsonPostClient acouJsonPostClient,
			HfPayOrderApplyIdGenerator orderApplyIdGenerator) {
		this.enterapplyReadService = enterapplyReadService;
		this.bankCardMapper = bankCardMapper;
		this.paymentSettingService = paymentSettingService;
		this.acouJsonPostClient = acouJsonPostClient;
		this.orderApplyIdGenerator = orderApplyIdGenerator;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> saveWithdrawBankCard(long companyId, long userId, Map<String, Object> requestParams) {
		LinkedHashMap<String, Object> params =
				new LinkedHashMap<>(requestParams != null ? requestParams : Map.of());
		params.put("company_id", companyId);
		params.put("user_id", userId);
		params.put("card_type", "1");
		params.put("is_cash", "1");

		Map<String, Object> enterMap = enterapplyReadService.getEnterapplyByCompanyAndUser(companyId, userId);
		if (enterMap == null) {
			throw new ResourceException("您还未开通汇付天下商户号，无法进行银行卡绑定");
		}
		Object ucid = enterMap.get("user_cust_id");
		if (ucid == null || !StringUtils.hasText(String.valueOf(ucid).trim())) {
			throw new ResourceException("您还未开通汇付天下商户号，无法进行银行卡绑定");
		}
		params.put("user_cust_id", String.valueOf(ucid).trim());

		Object cardNumRaw = params.get("card_num");
		String cardNum = cardNumRaw == null ? "" : String.valueOf(cardNumRaw).trim();

		LambdaQueryWrapper<HfpayBankCard> w = new LambdaQueryWrapper<>();
		w.eq(HfpayBankCard::getCompanyId, companyId);
		w.eq(HfpayBankCard::getUserId, userId);
		if (StringUtils.hasText(cardNum)) {
			w.eq(HfpayBankCard::getCardNum, cardNum);
		}
		HfpayBankCard existing = bankCardMapper.selectOne(w);

		if (existing != null) {
			params.put("hfpay_bank_card_id", existing.getHfpayBankCardId());
			if (StringUtils.hasText(existing.getBindCardId())) {
				throw new ResourceException("请勿重复绑定");
			}
		}

		boolean needBind = existing == null || !StringUtils.hasText(existing.getBindCardId());
		if (needBind) {
			Map<String, Object> setting = paymentSettingService.loadForCompany(companyId);
			LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
			payload.put("version", 10);
			payload.put("mer_cust_id", String.valueOf(setting.get("mer_cust_id")).trim());
			payload.put("order_date", LocalDate.now(ZoneId.systemDefault()).format(DAY));
			payload.put("order_id", orderApplyIdGenerator.nextOrderId());
			payload.put("card_num", cardNum);
			String cardTypeForBind = params.get("card_type") == null
					? "1"
					: String.valueOf(params.get("card_type")).trim();
			if (!StringUtils.hasText(cardTypeForBind)) {
				cardTypeForBind = "1";
			}
			payload.put("card_type", cardTypeForBind);

			switch (cardTypeForBind) {
				case "0" -> {
					Object bankIdRaw = params.get("bank_id");
					String bankId = bankIdRaw == null ? "" : String.valueOf(bankIdRaw).trim();
					if (!StringUtils.hasText(bankId)) {
						throw new BadRequestException("银行代号必填");
					}
					payload.put("user_cust_id", String.valueOf(params.get("user_cust_id")).trim());
					payload.put("bank_id", bankId);
				}
				case "1" -> payload.put("user_cust_id", String.valueOf(params.get("user_cust_id")).trim());
				default -> throw new BadRequestException("未知的绑卡类型");
			}

			String payloadUserCust = payload.get("user_cust_id") == null
					? ""
					: String.valueOf(payload.get("user_cust_id")).trim();
			if (!StringUtils.hasText(payloadUserCust)) {
				Object userName = params.get("user_name");
				Object idCard = params.get("id_card");
				Object userMobile = params.get("user_mobile");
				if (userName != null) {
					payload.put("user_name", String.valueOf(userName).trim());
				}
				if (idCard != null) {
					payload.put("id_card", String.valueOf(idCard).trim());
				}
				if (userMobile != null) {
					payload.put("user_mobile", String.valueOf(userMobile).trim());
				}
				String uidAfter = payload.get("user_cust_id") == null
						? ""
						: String.valueOf(payload.get("user_cust_id")).trim();
				String nameAfter = payload.get("user_name") == null
						? ""
						: String.valueOf(payload.get("user_name")).trim();
				if (!StringUtils.hasText(uidAfter) && !StringUtils.hasText(nameAfter)) {
					throw new BadRequestException("用户客户号必填");
				}
			}

			Map<String, Object> apiResult = acouJsonPostClient.bind01(setting, payload);
			String respCode = apiResult.get("resp_code") == null ? "" : String.valueOf(apiResult.get("resp_code")).trim();
			if ("C00000".equals(respCode) || "A51003".equals(respCode)) {
				Object bindRaw = apiResult.get("bind_card_id");
				if (bindRaw == null || !StringUtils.hasText(String.valueOf(bindRaw).trim())) {
					throw new ResourceException(Objects.toString(apiResult.get("resp_desc"), "绑卡失败"));
				}
				params.put("bind_card_id", String.valueOf(bindRaw).trim());
			} else {
				throw new ResourceException(Objects.toString(apiResult.get("resp_desc"), ""));
			}
		}

		validateSaveBankParams(params);

		Long updateId = parseOptionalLongId(params.get("hfpay_bank_card_id"));
		HfpayBankCard finalRow;
		if (updateId != null) {
			HfpayBankCard row = bankCardMapper.selectById(updateId);
			if (row == null || !Objects.equals(companyId, row.getCompanyId())) {
				throw new ResourceException("未查询到更新数据");
			}
			applyParamsToRow(row, params, userId);
			int affected = bankCardMapper.updateById(row);
			if (affected <= 0) {
				throw new ResourceException("未查询到更新数据");
			}
			finalRow = bankCardMapper.selectById(updateId);
			if (finalRow == null) {
				throw new ResourceException("未查询到更新数据");
			}
		} else {
			HfpayBankCard entity = new HfpayBankCard();
			entity.setCompanyId(companyId);
			entity.setUserId(userId);
			entity.setUserCustId(trimToNull(params.get("user_cust_id")));
			entity.setCardType(trimToNull(params.get("card_type")));
			entity.setBankId(trimToNull(params.get("bank_id")));
			entity.setBankName(trimToNull(params.get("bank_name")));
			entity.setCardNum(trimToNull(params.get("card_num")));
			entity.setBindCardId(trimToNull(params.get("bind_card_id")));
			entity.setIsCash(trimToNull(params.get("is_cash")));
			applyDistributorIdFromParams(entity, params);
			bankCardMapper.insert(entity);
			Long newId = entity.getHfpayBankCardId();
			if (newId == null) {
				throw new ResourceException("未查询到更新数据");
			}
			HfpayBankCard loaded = bankCardMapper.selectById(newId);
			finalRow = loaded != null ? loaded : entity;
		}

		return HfpayBankCardRowConverter.toDetailMap(finalRow);
	}

	private static Long parseOptionalLongId(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void validateSaveBankParams(Map<String, Object> params) {
		Object ctRaw = params.get("card_type");
		String ct = ctRaw == null ? "" : String.valueOf(ctRaw).trim();
		if (!StringUtils.hasText(ct)) {
			throw new ResourceException("绑卡类型必填");
		}
		if (!"0".equals(ct) && !"1".equals(ct)) {
			throw new ResourceException("绑卡类型不正确");
		}
		Object ucid = params.get("user_cust_id");
		if (ucid == null || !StringUtils.hasText(String.valueOf(ucid).trim())) {
			throw new ResourceException("用户客户号必填");
		}
		if ("0".equals(ct)) {
			Object bankIdRaw = params.get("bank_id");
			if (bankIdRaw == null || !StringUtils.hasText(String.valueOf(bankIdRaw).trim())) {
				throw new ResourceException("银行代号必填");
			}
		}
		Object cn = params.get("card_num");
		if (cn == null || !StringUtils.hasText(String.valueOf(cn).trim())) {
			throw new ResourceException("银行卡号必填");
		}
	}

	private void applyParamsToRow(HfpayBankCard row, Map<String, Object> params, long userId) {
		if (params.containsKey("bank_name")) {
			row.setBankName(trimToNull(params.get("bank_name")));
		}
		if (params.containsKey("card_num")) {
			row.setCardNum(trimToNull(params.get("card_num")));
		}
		if (params.containsKey("user_cust_id")) {
			row.setUserCustId(trimToNull(params.get("user_cust_id")));
		}
		if (params.containsKey("card_type")) {
			row.setCardType(trimToNull(params.get("card_type")));
		}
		if (params.containsKey("bank_id")) {
			row.setBankId(trimToNull(params.get("bank_id")));
		}
		if (params.containsKey("bind_card_id")) {
			row.setBindCardId(trimToNull(params.get("bind_card_id")));
		}
		if (params.containsKey("is_cash")) {
			row.setIsCash(trimToNull(params.get("is_cash")));
		}
		row.setUserId(userId);
		if (params.containsKey("distributor_id")) {
			applyDistributorIdFromParams(row, params);
		}
	}

	private static void applyDistributorIdFromParams(HfpayBankCard row, Map<String, Object> params) {
		if (!params.containsKey("distributor_id")) {
			return;
		}
		Object v = params.get("distributor_id");
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			row.setDistributorId(null);
			return;
		}
		try {
			row.setDistributorId(Long.parseLong(String.valueOf(v).trim()));
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 格式错误，须为有效整数");
		}
	}

	private static String trimToNull(Object v) {
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v).trim();
		return StringUtils.hasText(s) ? s : null;
	}
}
