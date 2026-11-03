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

package cn.shopex.ecshopx.digios.service;

import cn.shopex.ecshopx.digios.client.DigiOsGatewayClient;
import cn.shopex.ecshopx.digios.client.DigiOsGatewayResult;
import cn.shopex.ecshopx.digios.config.DigiOsProperties;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 对齐 PHP {@code DigiOsStoreAdjustmentSyncService}。 */
@Service
public class DigiOsStoreAdjustmentSyncService {

	private static final Logger log = LoggerFactory.getLogger(DigiOsStoreAdjustmentSyncService.class);

	private final DigiOsConfigService configService;
	private final DigiOsStoreAdjustmentLineResolver lineResolver;
	private final DigiOsGatewayClient gatewayClient;
	private final DigiOsProperties properties;

	public DigiOsStoreAdjustmentSyncService(
			DigiOsConfigService configService,
			DigiOsStoreAdjustmentLineResolver lineResolver,
			DigiOsGatewayClient gatewayClient,
			DigiOsProperties properties) {
		this.configService = configService;
		this.lineResolver = lineResolver;
		this.gatewayClient = gatewayClient;
		this.properties = properties;
	}

	public boolean syncOnDelivery(long companyId, long orderId) {
		var context = lineResolver.resolveForDelivery(companyId, orderId);
		if (context == null) {
			log.info(
					"digios skip delivery adjustment, no resolvable lines companyId={} orderId={}",
					companyId,
					orderId);
			return true;
		}
		return pushAdjustment(
				companyId,
				context.nm(),
				outboundDocTypCd(),
				context.shopCode(),
				DigiOsStoreAdjustmentMapper.mapDetails(context.lines(), true),
				"order_delivery",
				Map.of("order_id", orderId));
	}

	public boolean syncOnAftersalesReturn(long companyId, long aftersalesBn) {
		var context = lineResolver.resolveForAftersalesReturn(companyId, aftersalesBn);
		if (context == null) {
			log.info(
					"digios skip aftersales adjustment, no resolvable lines companyId={} aftersalesBn={}",
					companyId,
					aftersalesBn);
			return true;
		}
		return pushAdjustment(
				companyId,
				context.nm(),
				inboundDocTypCd(),
				context.shopCode(),
				DigiOsStoreAdjustmentMapper.mapDetails(context.lines(), false),
				"aftersales_return",
				Map.of("aftersales_bn", aftersalesBn));
	}

	private boolean pushAdjustment(
			long companyId,
			String nm,
			String docTypCd,
			String logWhCd,
			List<Map<String, Object>> details,
			String trigger,
			Map<String, Object> context) {
		if (!configService.resolve(companyId).enabled()) {
			return true;
		}
		if (details == null || details.isEmpty()) {
			log.info(
					"digios skip adjustment empty details companyId={} trigger={} context={}",
					companyId,
					trigger,
					context);
			return true;
		}
		List<List<Map<String, Object>>> chunks =
				DigiOsStoreAdjustmentMapper.chunkDetails(details, properties.getStockPushMaxRows());
		boolean allOk = true;
		for (int i = 0; i < chunks.size(); i++) {
			List<Map<String, Object>> chunk = chunks.get(i);
			Map<String, Object> payload =
					DigiOsStoreAdjustmentMapper.buildPayload(nm, docTypCd, logWhCd, chunk);
			DigiOsGatewayResult result = gatewayClient.put(companyId, "store.adjustment", payload);
			if (!result.ok()) {
				allOk = false;
				log.warn(
						"digios store adjustment failed companyId={} trigger={} nm={} docTypCd={} logWhCd={} chunk={}/{} error={} httpStatus={} context={}",
						companyId,
						trigger,
						nm,
						docTypCd,
						logWhCd,
						i + 1,
						chunks.size(),
						result.error(),
						result.httpStatus(),
						context);
			} else {
				log.info(
						"digios store adjustment finished companyId={} trigger={} nm={} docTypCd={} logWhCd={} chunk={}/{} detailCount={}",
						companyId,
						trigger,
						nm,
						docTypCd,
						logWhCd,
						i + 1,
						chunks.size(),
						chunk.size());
			}
		}
		return allOk;
	}

	private String outboundDocTypCd() {
		String v = properties.getStoreAdjustmentDocOutbound();
		return v == null || v.isBlank() ? DigiOsStoreAdjustmentMapper.DOC_OUTBOUND : v.trim();
	}

	private String inboundDocTypCd() {
		String v = properties.getStoreAdjustmentDocInbound();
		return v == null || v.isBlank() ? DigiOsStoreAdjustmentMapper.DOC_INBOUND : v.trim();
	}
}
