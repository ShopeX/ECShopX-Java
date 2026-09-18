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
 * PHP {@code TradeAftersaleCancelSendOme}: after OME gate + row load, {@code ome.refund.add} for ONLY_REFUND else
 * {@code ome.aftersale.add} with cancel status (5).
 */
@Component
public class TradeAftersaleCancelSendOmeNotificationProcessor {

	private static final Logger log =
			LoggerFactory.getLogger(TradeAftersaleCancelSendOmeNotificationProcessor.class);

	private static final String OME_REFUND_ADD_METHOD = "ome.refund.add";
	private static final String OME_AFTERSALE_ADD_METHOD = "ome.aftersale.add";

	private final ThirdShopexErpSettingAdminService thirdShopexErpSettingAdminService;
	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final ShopexErpOpenApiClient shopexErpOpenApiClient;

	public TradeAftersaleCancelSendOmeNotificationProcessor(
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
			log.debug("TradeAftersaleCancelSendOme skipped: OME not enabled companyId={}", companyId);
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
					"TradeAftersaleCancelSendOme skipped: no aftersales row companyId={} aftersales_bn={}",
					companyId,
					aftersalesBn);
			return;
		}

		String aftersalesType = row.getAftersalesType() == null ? "" : row.getAftersalesType().trim();
		if ("ONLY_REFUND".equals(aftersalesType)) {
			omeRefundCancel(row, companyId);
		} else {
			omeAftersaleCancelStatus5(row, companyId);
		}
	}

	private void omeRefundCancel(Aftersales row, long companyId) {
		Map<String, Object> cancelData = buildOmeRefundCancelParams(row);
		omeRequest(cancelData, companyId, OME_REFUND_ADD_METHOD);
	}

	private void omeAftersaleCancelStatus5(Aftersales row, long companyId) {
		Map<String, Object> cancelData = buildOmeAftersaleAddParams(row);
		cancelData.put("status", 5);
		omeRequest(cancelData, companyId, OME_AFTERSALE_ADD_METHOD);
	}

	private void omeRequest(Map<String, Object> struct, long companyId, String method) {
		try {
			Map<String, Object> result = shopexErpOpenApiClient.call(companyId, method, struct);
			log.debug("{}=>aftersaleStruct:{}=>result:{}", method, struct, result);
		} catch (RuntimeException e) {
			log.debug("OME请求失败:{}=>method:{}=>aftersaleStruct:{}", e.getMessage(), method, struct);
		}
	}

	private static Map<String, Object> buildOmeRefundCancelParams(Aftersales row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("aftersales_bn", row.getAftersalesBn());
		m.put("order_id", row.getOrderId());
		m.put("company_id", row.getCompanyId());
		m.put("user_id", row.getUserId());
		m.put("shop_id", row.getShopId());
		m.put("distributor_id", row.getDistributorId());
		m.put("supplier_id", row.getSupplierId());
		m.put("aftersales_type", row.getAftersalesType());
		return m;
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
