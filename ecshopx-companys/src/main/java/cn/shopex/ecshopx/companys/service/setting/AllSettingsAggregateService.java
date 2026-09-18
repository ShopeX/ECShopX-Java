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

package cn.shopex.ecshopx.companys.service.setting;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AllSettingsAggregateService {

	private final TradeRateSettingRedisService tradeRateSettingRedisService;
	private final ShareParametersSettingRedisService shareParametersSettingRedisService;
	private final WhitelistSettingRedisService whitelistSettingRedisService;
	private final PickupcodeSettingRedisService pickupcodeSettingRedisService;
	private final GiftSettingRedisService giftSettingRedisService;
	private final SendOmsSettingRedisService sendOmsSettingRedisService;
	private final NostoresSettingRedisService nostoresSettingRedisService;
	private final RechargeSettingRedisService rechargeSettingRedisService;
	private final TradeCancelSettingRedisService tradeCancelSettingRedisService;
	private final ItemStoreSettingRedisService itemStoreSettingRedisService;
	private final ItemSalesSettingRedisService itemSalesSettingRedisService;
	private final InvoiceSettingRedisService invoiceSettingRedisService;
	private final DianwuSettingRedisService dianwuSettingRedisService;
	private final ItemPriceSettingRedisService itemPriceSettingRedisService;
	private final CategoryPageSettingRedisService categoryPageSettingRedisService;
	private final CompanysPharmaIndustrySettingReadService companysPharmaIndustrySettingReadService;
	private final OpenDistributorDividedSettingRedisService openDistributorDividedSettingRedisService;

	public AllSettingsAggregateService(
			TradeRateSettingRedisService tradeRateSettingRedisService,
			ShareParametersSettingRedisService shareParametersSettingRedisService,
			WhitelistSettingRedisService whitelistSettingRedisService,
			PickupcodeSettingRedisService pickupcodeSettingRedisService,
			GiftSettingRedisService giftSettingRedisService,
			SendOmsSettingRedisService sendOmsSettingRedisService,
			NostoresSettingRedisService nostoresSettingRedisService,
			RechargeSettingRedisService rechargeSettingRedisService,
			TradeCancelSettingRedisService tradeCancelSettingRedisService,
			ItemStoreSettingRedisService itemStoreSettingRedisService,
			ItemSalesSettingRedisService itemSalesSettingRedisService,
			InvoiceSettingRedisService invoiceSettingRedisService,
			DianwuSettingRedisService dianwuSettingRedisService,
			ItemPriceSettingRedisService itemPriceSettingRedisService,
			CategoryPageSettingRedisService categoryPageSettingRedisService,
			CompanysPharmaIndustrySettingReadService companysPharmaIndustrySettingReadService,
			OpenDistributorDividedSettingRedisService openDistributorDividedSettingRedisService) {
		this.tradeRateSettingRedisService = tradeRateSettingRedisService;
		this.shareParametersSettingRedisService = shareParametersSettingRedisService;
		this.whitelistSettingRedisService = whitelistSettingRedisService;
		this.pickupcodeSettingRedisService = pickupcodeSettingRedisService;
		this.giftSettingRedisService = giftSettingRedisService;
		this.sendOmsSettingRedisService = sendOmsSettingRedisService;
		this.nostoresSettingRedisService = nostoresSettingRedisService;
		this.rechargeSettingRedisService = rechargeSettingRedisService;
		this.tradeCancelSettingRedisService = tradeCancelSettingRedisService;
		this.itemStoreSettingRedisService = itemStoreSettingRedisService;
		this.itemSalesSettingRedisService = itemSalesSettingRedisService;
		this.invoiceSettingRedisService = invoiceSettingRedisService;
		this.dianwuSettingRedisService = dianwuSettingRedisService;
		this.itemPriceSettingRedisService = itemPriceSettingRedisService;
		this.categoryPageSettingRedisService = categoryPageSettingRedisService;
		this.companysPharmaIndustrySettingReadService = companysPharmaIndustrySettingReadService;
		this.openDistributorDividedSettingRedisService = openDistributorDividedSettingRedisService;
	}

	public Map<String, Object> getAllSetting(long companyId) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put(
				"traderate_setting",
				tradeRateSettingRedisService.rateSetting(companyId, new LinkedHashMap<>()));
		result.put("share_parameters_setting", shareParametersSettingRedisService.read(companyId));
		result.put("whitelist_setting", whitelistSettingRedisService.getMergedConfig(companyId, null));
		result.put("pickupcode_setting", pickupcodeSettingRedisService.handle(companyId, null));
		result.put("gift_setting", giftSettingRedisService.getGiftSetting(companyId));
		result.put("sendoms_setting", sendOmsSettingRedisService.readZitiSendOms(companyId));
		result.put("nostores_setting", nostoresSettingRedisService.readSetting(companyId));
		result.put("recharge_setting", rechargeSettingRedisService.handle(companyId, null));
		result.put("cancel_setting", tradeCancelSettingRedisService.getCancelSetting(companyId));
		result.put("item_store_setting", itemStoreSettingRedisService.getItemStoreSetting(companyId));
		result.put("item_sales_setting", itemSalesSettingRedisService.getItemSalesSetting(companyId));
		result.put("invoice_setting", invoiceSettingRedisService.getInvoiceSetting(companyId));
		result.put("dianwu_setting", dianwuSettingRedisService.getDianwuSetting(companyId));
		result.put("item_price_setting", itemPriceSettingRedisService.getItemPriceSetting(companyId));
		result.put("category_style", categoryPageSettingRedisService.getCategoryPageSetting(companyId));
		result.put(
				"medicine_setting",
				companysPharmaIndustrySettingReadService.getMedicineSetting(companyId));
		result.put(
				"open_distributor_divided",
				openDistributorDividedSettingRedisService.getOpenDistributorDivided(companyId));
		return result;
	}
}
