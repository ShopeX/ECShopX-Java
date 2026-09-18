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
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.goods.service.ome.ShopexErpOpenApiClient;
import cn.shopex.ecshopx.systemlink.service.third.ThirdShopexErpSettingAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TradeAfterLogiSendOmeNotificationProcessor {

	private static final Logger log = LoggerFactory.getLogger(TradeAfterLogiSendOmeNotificationProcessor.class);

	private static final String OME_AFTERSALE_LOGISTICS_UPDATE = "ome.aftersale.logistics_update";

	private final ThirdShopexErpSettingAdminService thirdShopexErpSettingAdminService;
	private final AftersalesMapper aftersalesMapper;
	private final ShopexErpOpenApiClient shopexErpOpenApiClient;
	private final ObjectMapper objectMapper;

	public TradeAfterLogiSendOmeNotificationProcessor(
			ThirdShopexErpSettingAdminService thirdShopexErpSettingAdminService,
			AftersalesMapper aftersalesMapper,
			ShopexErpOpenApiClient shopexErpOpenApiClient,
			ObjectMapper objectMapper) {
		this.thirdShopexErpSettingAdminService = thirdShopexErpSettingAdminService;
		this.aftersalesMapper = aftersalesMapper;
		this.shopexErpOpenApiClient = shopexErpOpenApiClient;
		this.objectMapper = objectMapper;
	}

	public void handle(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		Long companyId = longObj(payload.get("company_id"));
		if (companyId == null || companyId <= 0L) {
			return;
		}

		log.debug("TradeAfterLogiSendOme payload snapshot companyId={}", companyId);

		Map<String, Object> setting = thirdShopexErpSettingAdminService.getShopexErpSetting(companyId);
		if (setting == null || !isShopexErpOpen(setting)) {
			log.debug("companyId:{} msg:未开启OME", companyId);
			return;
		}

		try {
			Map<String, Object> afterLogistics = buildAfterLogisticsPayload(payload);
			if (afterLogistics == null || afterLogistics.isEmpty()) {
				log.debug("获取售后物流信息失败");
				return;
			}
			log.debug("TradeAfterLogiSendOme afterLogistics keys={}", afterLogistics.keySet());
			omeLogisticsUpdateRequest(afterLogistics, companyId);
		} catch (RuntimeException e) {
			log.debug("OME物流请求失败:{}", e.getMessage());
		}
	}

	private void omeLogisticsUpdateRequest(Map<String, Object> afterData, long companyId) {
		try {
			Map<String, Object> result =
					shopexErpOpenApiClient.call(companyId, OME_AFTERSALE_LOGISTICS_UPDATE, afterData);
			log.debug("{}=>{}", OME_AFTERSALE_LOGISTICS_UPDATE, result);
		} catch (RuntimeException e) {
			log.debug(
					"OME请求失败:{}=>method:{}=>afterData:{}",
					e.getMessage(),
					OME_AFTERSALE_LOGISTICS_UPDATE,
					afterData);
		}
	}

	private Map<String, Object> buildAfterLogisticsPayload(Map<String, Object> entities) {
		Long companyId = longObj(entities.get("company_id"));
		Long aftersalesBn = longObj(entities.get("aftersales_bn"));
		if (companyId == null || aftersalesBn == null) {
			return null;
		}

		Aftersales afterInfo =
				aftersalesMapper.selectOne(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getAftersalesBn, aftersalesBn)
								.last("LIMIT 1"));
		if (afterInfo == null) {
			log.debug(
					"TradeAfterLogiSendOme skipped: aftersales row missing companyId={} aftersales_bn={}",
					companyId,
					aftersalesBn);
			return null;
		}

		Map<String, String> sendbackData = parseSendbackData(afterInfo.getSendbackData());
		if (sendbackData == null
				|| !StringUtils.hasText(sendbackData.get("logi_no"))
				|| !StringUtils.hasText(sendbackData.get("corp_code"))) {
			return null;
		}

		String logiNo = sendbackData.get("logi_no").trim();
		String corpCode = sendbackData.get("corp_code").trim();
		if ("YD".equals(corpCode)) {
			corpCode = "YUNDA";
		}

		Map<String, Object> logisticsInfo = new LinkedHashMap<>();
		logisticsInfo.put("logi_no", logiNo);
		logisticsInfo.put("logi_company", corpCode);

		Map<String, Object> afterData = new LinkedHashMap<>();
		afterData.put("node_version", "3.0");
		try {
			afterData.put("logistics_info", objectMapper.writeValueAsString(logisticsInfo));
		} catch (Exception e) {
			return null;
		}
		afterData.put("order_bn", afterInfo.getOrderId());
		afterData.put("return_bn", afterInfo.getAftersalesBn());
		return afterData;
	}

	private Map<String, String> parseSendbackData(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			return objectMapper.readValue(json, new TypeReference<>() {});
		} catch (Exception e) {
			return null;
		}
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
