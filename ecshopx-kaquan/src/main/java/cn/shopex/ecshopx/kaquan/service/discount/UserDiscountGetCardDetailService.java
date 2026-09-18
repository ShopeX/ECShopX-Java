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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.discount.DiscountCardKaquanDetailForUserLoadService;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserDiscountGetCardDetailService {

	private final WechatAuthQueryService wechatAuthQueryService;
	private final UserDiscountMapper userDiscountMapper;
	private final UserDiscountUserRecordDetailAssembler userDiscountUserRecordDetailAssembler;
	private final WxShopsListForUserDiscountService wxShopsListForUserDiscountService;
	private final DiscountCardKaquanDetailForUserLoadService discountCardKaquanDetailForUserLoadService;
	private final ExchangeCardBarcodeImageService exchangeCardBarcodeImageService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;

	public UserDiscountGetCardDetailService(
			WechatAuthQueryService wechatAuthQueryService,
			UserDiscountMapper userDiscountMapper,
			UserDiscountUserRecordDetailAssembler userDiscountUserRecordDetailAssembler,
			WxShopsListForUserDiscountService wxShopsListForUserDiscountService,
			DiscountCardKaquanDetailForUserLoadService discountCardKaquanDetailForUserLoadService,
			ExchangeCardBarcodeImageService exchangeCardBarcodeImageService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.userDiscountMapper = userDiscountMapper;
		this.userDiscountUserRecordDetailAssembler = userDiscountUserRecordDetailAssembler;
		this.wxShopsListForUserDiscountService = wxShopsListForUserDiscountService;
		this.discountCardKaquanDetailForUserLoadService = discountCardKaquanDetailForUserLoadService;
		this.exchangeCardBarcodeImageService = exchangeCardBarcodeImageService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
	}

	public Map<String, Object> build(long companyId, long userId, String code, Long cardId, String wxappAppid) {
		String principalName = "";
		if (StringUtils.hasText(wxappAppid)) {
			principalName = wechatAuthQueryService.findPrincipalNameByCompanyAndAppid(companyId, wxappAppid).orElse("");
		}

		Map<String, Object> result = new LinkedHashMap<>();

		LambdaQueryWrapper<UserDiscount> qw = new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getUserId, userId);
		if (StringUtils.hasText(code)) {
			qw.eq(UserDiscount::getCode, code.trim());
		}
		if (cardId != null && cardId > 0L) {
			qw.eq(UserDiscount::getCardId, cardId);
		}
		qw.orderByAsc(UserDiscount::getStatus).orderByAsc(UserDiscount::getEndDate).last("LIMIT 100");
		List<UserDiscount> list = userDiscountMapper.selectList(qw);

		if (list.isEmpty()) {
			result.put("detail", Collections.emptyList());
			result.put("shop_list", Collections.emptyList());
			result.put("card_info", Collections.emptyList());
			result.put("card_code", Collections.emptyList());
			return result;
		}

		UserDiscount first = list.get(0);
		Map<String, Object> detailMap = userDiscountUserRecordDetailAssembler.toDetailMap(first);
		result.put("detail", detailMap);

		Object relShopsForQuery = detailMap.get("rel_shops_ids");
		Map<String, Object> shopList = wxShopsListForUserDiscountService.listShopsPoi(companyId, relShopsForQuery);
		if (StringUtils.hasText(principalName)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> lst = (List<Map<String, Object>>) shopList.get("list");
			if (lst != null) {
				for (Map<String, Object> row : lst) {
					row.put("companyName", principalName);
				}
			}
		}
		result.put("shop_list", shopList);

		long templateCardId = first.getCardId() != null ? first.getCardId() : 0L;
		if (templateCardId > 0L) {
			Map<String, Object> cardInfo =
					discountCardKaquanDetailForUserLoadService.loadDetailForUserCard(companyId, templateCardId);
			Map<String, Object> cardCodeMap = new LinkedHashMap<>();
			if ("SWEEP".equals(String.valueOf(cardInfo.get("use_scenes")).trim())) {
				String payload = "CQ_" + nullToEmpty(first.getCode());
				byte[] barPng = exchangeCardBarcodeImageService.sweepBarcodePngBytes(payload);
				byte[] qrPng = exchangeCardBarcodeImageService.sweepQrcodePngBytes(payload);
				String barB64 = Base64.getEncoder().encodeToString(barPng);
				String qrB64 = Base64.getEncoder().encodeToString(qrPng);
				cardCodeMap.put("barcode_url", "data:image/jpg;base64," + barB64);
				cardCodeMap.put("qrcode_url", "data:image/jpg;base64," + qrB64);
				cardCodeMap.put("code", first.getCode());
			}
			result.put("card_info", cardInfo);
			result.put("card_code", cardCodeMap);
		} else {
			result.put("card_info", Collections.emptyMap());
			result.put("card_code", Collections.emptyMap());
		}

		return result;
	}

	public Map<String, Object> buildWithCur(long companyId, long userId, String code, Long cardId, String wxappAppid) {
		Map<String, Object> result = build(companyId, userId, code, cardId, wxappAppid);
		CurrencyExchangeRate row = companyDefaultCurrencyService.getCur(companyId);
		result.put("cur", companyDefaultCurrencyService.toCurResponseMap(row));
		return result;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
