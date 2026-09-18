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

package cn.shopex.ecshopx.systemlink.service.ome;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.goods.service.ome.ShopexErpOpenApiClient;
import cn.shopex.ecshopx.systemlink.service.third.ThirdShopexErpSettingAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Mirrors PHP {@code TradeAftersalesSendOme}: OME gate, load persisted aftersales, then {@code ome.aftersale.add}.
 */
@Component
public class TradeAftersalesSendOmeNotificationProcessor {

	private static final Logger log = LoggerFactory.getLogger(TradeAftersalesSendOmeNotificationProcessor.class);

	private static final String OME_AFTERSALE_ADD_METHOD = "ome.aftersale.add";

	private final ThirdShopexErpSettingAdminService thirdShopexErpSettingAdminService;
	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final ShopexErpOpenApiClient shopexErpOpenApiClient;

	public TradeAftersalesSendOmeNotificationProcessor(
			ThirdShopexErpSettingAdminService thirdShopexErpSettingAdminService,
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			ShopexErpOpenApiClient shopexErpOpenApiClient) {
		this.thirdShopexErpSettingAdminService = thirdShopexErpSettingAdminService;
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.shopexErpOpenApiClient = shopexErpOpenApiClient;
	}

	public void handle(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		Long companyId = longObj(payload.get("company_id"));
		if (companyId == null || companyId <= 0L) {
			return;
		}
		Long aftersalesBn = longObj(payload.get("aftersales_bn"));
		if (aftersalesBn == null || aftersalesBn <= 0L) {
			return;
		}

		Map<String, Object> setting = thirdShopexErpSettingAdminService.getShopexErpSetting(companyId);
		if (setting == null || !isShopexErpOpen(setting)) {
			log.debug("TradeAftersalesSendOme skipped: OME not enabled companyId={}", companyId);
			return;
		}

		Aftersales row =
				aftersalesMapper.selectOne(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getAftersalesBn, aftersalesBn)
								.last("LIMIT 1"));
		if (row == null) {
			log.debug(
					"TradeAftersalesSendOme skipped: no aftersales row companyId={} aftersales_bn={}",
					companyId,
					aftersalesBn);
			return;
		}

		Map<String, Object> orderStruct = buildOmeAftersaleAddParams(row);
		omeRequest(orderStruct, companyId);
	}

	private void omeRequest(Map<String, Object> orderStruct, long companyId) {
		try {
			Map<String, Object> result =
					shopexErpOpenApiClient.call(companyId, OME_AFTERSALE_ADD_METHOD, orderStruct);
			log.debug("{}=>aftersaleStruct:{}=>result:{}", OME_AFTERSALE_ADD_METHOD, orderStruct, result);
		} catch (RuntimeException e) {
			log.debug(
					"OME请求失败:{}=>method:{}=>aftersaleStruct:{}",
					e.getMessage(),
					OME_AFTERSALE_ADD_METHOD,
					orderStruct);
		}
	}

	private Map<String, Object> buildOmeAftersaleAddParams(Aftersales row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("aftersales_bn", row.getAftersalesBn());
		m.put("order_id", row.getOrderId());
		m.put("company_id", row.getCompanyId());
		m.put("user_id", row.getUserId());
		m.put("shop_id", row.getShopId());
		m.put("distributor_id", row.getDistributorId());
		m.put("supplier_id", row.getSupplierId());
		m.put("aftersales_type", row.getAftersalesType());
		m.put("aftersales_status", row.getAftersalesStatus());
		m.put("progress", row.getProgress());
		m.put("refund_fee", row.getRefundFee());
		m.put("refund_point", row.getRefundPoint());
		m.put("reason", row.getReason());
		m.put("description", row.getDescription());
		m.put("evidence_pic", row.getEvidencePic());
		m.put("refuse_reason", row.getRefuseReason());
		m.put("memo", row.getMemo());
		m.put("sendback_data", row.getSendbackData());
		m.put("sendconfirm_data", row.getSendconfirmData());
		m.put("third_data", row.getThirdData());
		m.put("aftersales_address", row.getAftersalesAddress());
		m.put("distributor_remark", row.getDistributorRemark());
		m.put("create_time", row.getCreateTime());
		m.put("update_time", row.getUpdateTime());
		m.put("contact", row.getContact());
		m.put("mobile", row.getMobile());
		m.put("merchant_id", row.getMerchantId());
		m.put("is_partial_cancel", row.getIsPartialCancel());
		m.put("return_type", row.getReturnType());
		m.put("return_distributor_id", row.getReturnDistributorId());
		m.put("self_delivery_operator_id", row.getSelfDeliveryOperatorId());
		m.put("freight", row.getFreight());
		m.put("freight_type", row.getFreightType());
		m.put("auto_refuse_time", firstDetailAutoRefuseTime(row));
		m.put("salesman_id", row.getSalesmanId());
		m.put("item_bn", row.getItemBn());
		return m;
	}

	private String firstDetailAutoRefuseTime(Aftersales row) {
		long companyId = row.getCompanyId() == null ? 0L : row.getCompanyId();
		long bn = row.getAftersalesBn() == null ? 0L : row.getAftersalesBn();
		if (companyId <= 0L || bn <= 0L) {
			return "0";
		}
		AftersalesDetail d =
				aftersalesDetailMapper.selectOne(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getAftersalesBn, bn)
								.orderByAsc(AftersalesDetail::getDetailId)
								.last("LIMIT 1"));
		if (d == null || d.getAutoRefuseTime() == null || d.getAutoRefuseTime().isBlank()) {
			return "0";
		}
		return d.getAutoRefuseTime();
	}

	private static boolean isShopexErpOpen(Map<String, Object> setting) {
		Object open = setting.get("is_open");
		if (open instanceof Boolean b) {
			return Boolean.TRUE.equals(b);
		}
		return Boolean.parseBoolean(String.valueOf(open));
	}

	private static Long longObj(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
