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

package cn.shopex.ecshopx.kaquan.service.vipgrade;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeOrder;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeOrderMapper;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VipGradeOrderBuyCreateService {

	private final VipGradeMapper vipGradeMapper;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final NormalOrderNumericIdService normalOrderNumericIdService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final VipGradeOrderMapper vipGradeOrderMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final ObjectMapper objectMapper;

	public VipGradeOrderBuyCreateService(
			VipGradeMapper vipGradeMapper,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			NormalOrderNumericIdService normalOrderNumericIdService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			VipGradeOrderMapper vipGradeOrderMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			ObjectMapper objectMapper) {
		this.vipGradeMapper = vipGradeMapper;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.normalOrderNumericIdService = normalOrderNumericIdService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.vipGradeOrderMapper = vipGradeOrderMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createDataForBuy(
			long companyId,
			long userId,
			String mobilePlain,
			String vipGradeIdStr,
			String cardTypeName,
			long distributorIdForOrder) {
		long vipGradeId;
		try {
			vipGradeId = Long.parseLong(vipGradeIdStr.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("没有该会员卡");
		}
		if (vipGradeId > Integer.MAX_VALUE || vipGradeId < Integer.MIN_VALUE) {
			throw new ResourceException("没有该会员卡");
		}
		int vipGradeIdInt = (int) vipGradeId;

		VipGrade grade = vipGradeMapper.selectOne(new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getVipGradeId, vipGradeId)
				.eq(VipGrade::getCompanyId, (int) companyId));
		if (grade == null) {
			throw new ResourceException("没有该会员卡");
		}

		Map<String, Object> vipgrade = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		String userVipType = String.valueOf(vipgrade.getOrDefault("vip_type", "")).trim();
		String targetLv = grade.getLvType() == null ? "" : grade.getLvType().trim();
		if ("svip".equals(userVipType) && !"svip".equals(targetLv)) {
			throw new ResourceException("已购买更高等级会员卡");
		}

		String priceListJson = grade.getPriceList();
		if (priceListJson == null || priceListJson.isBlank()) {
			throw new ResourceException("购买失败");
		}
		JsonNode priceRoot;
		try {
			priceRoot = objectMapper.readTree(priceListJson);
		} catch (JsonProcessingException e) {
			throw new ResourceException("购买失败");
		}
		if (!priceRoot.isArray()) {
			throw new ResourceException("购买失败");
		}
		JsonNode matchedListNode = null;
		for (JsonNode list : priceRoot) {
			if (!list.isObject()) {
				continue;
			}
			String name = list.path("name").asText("");
			if (cardTypeName.equals(name)) {
				matchedListNode = list;
				break;
			}
		}
		if (matchedListNode == null) {
			throw new ResourceException("购买失败");
		}
		int priceFen = (int) Math.round(matchedListNode.path("price").asDouble(0d) * 100.0);

		String title = grade.getGradeName();
		String lvType = grade.getLvType();
		int discount = VipGradeGradeDiscountParser.parseDiscountFromPrivilegesJson(grade.getPrivileges(), objectMapper);

		long orderId = normalOrderNumericIdService.generate(userId);
		int nowSec = (int) (System.currentTimeMillis() / 1000L);

		CurrencyExchangeRate cur = companyDefaultCurrencyService.getCur(companyId);
		String feeType = cur.getCurrency() != null ? cur.getCurrency() : "";
		Double feeRate = cur.getRate() != null ? cur.getRate() : 1.0;
		String feeSymbol = cur.getSymbol() != null ? cur.getSymbol() : "";

		String cardTypeJson;
		try {
			cardTypeJson = objectMapper.writeValueAsString(matchedListNode);
		} catch (JsonProcessingException e) {
			throw new ResourceException("购买失败");
		}

		String mobileStored = sensitiveFieldEncryptor.encrypt(mobilePlain == null ? "" : mobilePlain.trim());

		VipGradeOrder order = new VipGradeOrder();
		order.setOrderId(orderId);
		order.setVipGradeId(vipGradeIdInt);
		order.setLvType(lvType);
		order.setCompanyId((int) companyId);
		order.setUserId(userId);
		order.setMobile(mobileStored);
		order.setTitle(title);
		order.setPrice(priceFen);
		order.setCardType(cardTypeJson);
		order.setDiscount(discount);
		order.setShopId(0L);
		order.setDistributorId(distributorIdForOrder);
		order.setOrderStatus("NOTPAY");
		order.setSourceType("sale");
		order.setCreated(nowSec);
		order.setUpdated(nowSec);
		order.setFeeType(feeType);
		order.setFeeRate(feeRate);
		order.setFeeSymbol(feeSymbol);

		vipGradeOrderMapper.insert(order);

		OrderAssociations assoc = new OrderAssociations();
		assoc.setOrderId(orderId);
		assoc.setTitle(title);
		assoc.setCompanyId(companyId);
		assoc.setShopId(0L);
		assoc.setUserId(userId);
		assoc.setTotalFee((long) priceFen);
		assoc.setOrderStatus("NOTPAY");
		assoc.setCreateTime(nowSec);
		assoc.setOrderType("memberCard");
		assoc.setTotalRebate(0);
		assoc.setMemberDiscount(0);
		assoc.setCouponDiscount(0);
		assoc.setOrderClass("memberCard");
		assoc.setMobile("");
		assoc.setFeeType(order.getFeeType());
		assoc.setFeeRate(feeRate.floatValue());
		assoc.setFeeSymbol(order.getFeeSymbol());

		orderAssociationsMapper.insert(assoc);

		Map<String, Object> orderRow = new LinkedHashMap<>();
		orderRow.put("order_id", orderId);
		orderRow.put("vip_grade_id", vipGradeIdInt);
		orderRow.put("lv_type", lvType);
		orderRow.put("company_id", (int) companyId);
		orderRow.put("user_id", userId);
		orderRow.put("mobile", mobileStored);
		orderRow.put("title", title);
		orderRow.put("price", priceFen);
		try {
			Map<String, Object> cardTypeObject =
					objectMapper.readValue(cardTypeJson, new TypeReference<Map<String, Object>>() {});
			orderRow.put("card_type", cardTypeObject);
		} catch (JsonProcessingException e) {
			throw new ResourceException("购买失败");
		}
		orderRow.put("discount", discount);
		orderRow.put("shop_id", 0L);
		orderRow.put("distributor_id", distributorIdForOrder);
		orderRow.put("source_id", order.getSourceId());
		orderRow.put("source_type", order.getSourceType());
		orderRow.put("monitor_id", order.getMonitorId());
		orderRow.put("order_status", "NOTPAY");
		orderRow.put("created", nowSec);
		orderRow.put("updated", nowSec);
		orderRow.put("fee_type", order.getFeeType());
		orderRow.put("fee_rate", order.getFeeRate());
		orderRow.put("fee_symbol", order.getFeeSymbol());

		Map<String, Object> assocRow = new LinkedHashMap<>();
		assocRow.put("order_id", orderId);
		assocRow.put("authorizer_appid", assoc.getAuthorizerAppid());
		assocRow.put("wxa_appid", assoc.getWxaAppid());
		assocRow.put("title", title);
		assocRow.put("total_fee", (long) priceFen);
		assocRow.put("company_id", companyId);
		assocRow.put("shop_id", 0L);
		assocRow.put("store_name", assoc.getStoreName());
		assocRow.put("user_id", userId);
		assocRow.put("salesman_id", assoc.getSalesmanId());
		assocRow.put("promoter_user_id", assoc.getPromoterUserId());
		assocRow.put("promoter_shop_id", assoc.getPromoterShopId());
		assocRow.put("source_id", assoc.getSourceId());
		assocRow.put("monitor_id", assoc.getMonitorId());
		assocRow.put("mobile", "");
		assocRow.put("order_class", "memberCard");
		assocRow.put("order_type", "memberCard");
		assocRow.put("order_status", "NOTPAY");
		assocRow.put("create_time", nowSec);
		assocRow.put("update_time", assoc.getUpdateTime());
		assocRow.put("is_distribution", assoc.getIsDistribution());
		assocRow.put("total_rebate", 0);
		assocRow.put("delivery_corp", assoc.getDeliveryCorp());
		assocRow.put("delivery_code", assoc.getDeliveryCode());
		assocRow.put("member_discount", 0);
		assocRow.put("coupon_discount", 0);
		assocRow.put("coupon_discount_desc", assoc.getCouponDiscountDesc());
		assocRow.put("member_discount_desc", assoc.getMemberDiscountDesc());
		assocRow.put("delivery_status", assoc.getDeliveryStatus());
		assocRow.put("delivery_time", assoc.getDeliveryTime());
		assocRow.put("cancel_status", assoc.getCancelStatus());
		assocRow.put("end_time", assoc.getEndTime());
		assocRow.put("fee_type", order.getFeeType());
		assocRow.put("fee_rate", feeRate.floatValue());
		assocRow.put("fee_symbol", order.getFeeSymbol());

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(orderRow);
		merged.putAll(assocRow);
		return merged;
	}
}
