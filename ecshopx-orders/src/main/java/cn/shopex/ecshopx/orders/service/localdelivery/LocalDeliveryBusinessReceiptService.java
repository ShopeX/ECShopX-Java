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

package cn.shopex.ecshopx.orders.service.localdelivery;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryReceiptPort;
import cn.shopex.ecshopx.common.port.localdelivery.ShansongLocalDeliveryReceiptPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.CompanyRelDada;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.mapper.CompanyRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalDeliveryBusinessReceiptService {

	private static final int DADA_RECEIPT_REFRESH_SECONDS = 60 * 2 + 55;

	private final NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	private final CompanyRelDadaMapper companyRelDadaMapper;
	private final LocalDeliveryOrderInfoLoadService localDeliveryOrderInfoLoadService;
	private final DadaLocalDeliveryReceiptPort dadaLocalDeliveryReceiptPort;
	private final ShansongLocalDeliveryReceiptPort shansongLocalDeliveryReceiptPort;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final String localDeliveryDriver;

	public LocalDeliveryBusinessReceiptService(
			NormalOrdersRelDadaMapper normalOrdersRelDadaMapper,
			CompanyRelDadaMapper companyRelDadaMapper,
			LocalDeliveryOrderInfoLoadService localDeliveryOrderInfoLoadService,
			DadaLocalDeliveryReceiptPort dadaLocalDeliveryReceiptPort,
			ShansongLocalDeliveryReceiptPort shansongLocalDeliveryReceiptPort,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			@Value("${common.local-delivery-dirver:dada}") String localDeliveryDriver) {
		this.normalOrdersRelDadaMapper = normalOrdersRelDadaMapper;
		this.companyRelDadaMapper = companyRelDadaMapper;
		this.localDeliveryOrderInfoLoadService = localDeliveryOrderInfoLoadService;
		this.dadaLocalDeliveryReceiptPort = dadaLocalDeliveryReceiptPort;
		this.shansongLocalDeliveryReceiptPort = shansongLocalDeliveryReceiptPort;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.localDeliveryDriver = localDeliveryDriver;
	}

	public void businessReceipt(long companyId, String orderIdParam, long operatorId) {
		String oid = orderIdParam == null ? "" : orderIdParam.trim();
		String driver =
				localDeliveryDriver == null
						? "dada"
						: localDeliveryDriver.trim().toLowerCase(Locale.ROOT);
		if (!"dada".equals(driver) && !"shansong".equals(driver)) {
			throw new ResourceException("同城配仅支持达达和闪送");
		}

		NormalOrdersRelDada row =
				normalOrdersRelDadaMapper.selectOne(
						new LambdaQueryWrapper<NormalOrdersRelDada>()
								.eq(NormalOrdersRelDada::getCompanyId, companyId)
								.apply("CAST(order_id AS CHAR) = {0}", oid)
								.last("LIMIT 1"));
		if (row == null) {
			if ("shansong".equals(driver)) {
				throw new ResourceException("未查询到闪送订单");
			}
			throw new ResourceException("未查询到达达相关数据");
		}
		Long rowOrderId = row.getOrderId();
		if (rowOrderId == null) {
			if ("shansong".equals(driver)) {
				throw new ResourceException("未查询到闪送订单");
			}
			throw new ResourceException("未查询到达达相关数据");
		}
		long orderId = rowOrderId;
		if (!Integer.valueOf(0).equals(row.getDadaStatus())) {
			if ("shansong".equals(driver)) {
				throw new ResourceException("闪送订单状态不正确，无需此操作");
			}
			throw new ResourceException("订单状态不正确，无需此操作");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);

		String newDeliveryNo = null;
		if ("dada".equals(driver)) {
			newDeliveryNo = processDadaBranch(companyId, orderId, row, now);
		} else {
			shansongLocalDeliveryReceiptPort.orderPlaceApi(companyId, row.getDadaDeliveryNo());
			// 闪送与达达在数据库行更新成功后共用 orderProcessLogPublishPort.publish(log)。
		}

		LambdaUpdateWrapper<NormalOrdersRelDada> uw = new LambdaUpdateWrapper<>();
		uw.eq(NormalOrdersRelDada::getCompanyId, companyId)
				.eq(NormalOrdersRelDada::getOrderId, orderId)
				.set(NormalOrdersRelDada::getDadaStatus, 1)
				.set(NormalOrdersRelDada::getAcceptTime, now)
				.set(NormalOrdersRelDada::getUpdateTime, now);
		if (newDeliveryNo != null) {
			uw.set(NormalOrdersRelDada::getDadaDeliveryNo, newDeliveryNo);
		}
		int updated = normalOrdersRelDadaMapper.update(null, uw);
		if (updated == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		LinkedHashMap<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", Long.valueOf(orderId));
		log.put("company_id", companyId);
		log.put("operator_type", "admin");
		log.put("operator_id", operatorId);
		log.put("remarks", "商家接单");
		log.put("detail", "订单号：" + oid + "，商家已接单");
		log.put("params", Map.of());
		orderProcessLogPublishPort.publish(log);
	}

	private String processDadaBranch(long companyId, long orderId, NormalOrdersRelDada row, int now) {
		int updateTimeForTimeout = row.getUpdateTime() == null ? 0 : row.getUpdateTime();
		if (now - updateTimeForTimeout > DADA_RECEIPT_REFRESH_SECONDS) {
			Map<String, Object> orderInfo = localDeliveryOrderInfoLoadService.loadOrderInfoForDadaFreight(companyId, orderId);
			applyTotalFreightPointAdjustment(orderInfo);
			CompanyRelDada companyRelDada =
					companyRelDadaMapper.selectOne(
							new LambdaQueryWrapper<CompanyRelDada>()
									.eq(CompanyRelDada::getCompanyId, companyId)
									.last("LIMIT 1"));
			boolean buyerPaysFreight = companyRelDada != null && Boolean.TRUE.equals(companyRelDada.getFreightType());
			dadaLocalDeliveryReceiptPort.queryDeliverFeeAndApplyToOrderData(companyId, buyerPaysFreight, orderInfo);
			String newNo = Objects.toString(orderInfo.get("dada_delivery_no"), "").trim();
			if (newNo.isEmpty()) {
				throw new ResourceException("达达询价未返回运单号");
			}
			dadaLocalDeliveryReceiptPort.addAfterQuery(companyId, newNo);
			return newNo;
		}
		String existing = row.getDadaDeliveryNo() == null ? "" : row.getDadaDeliveryNo().trim();
		if (existing.isEmpty()) {
			throw new ResourceException("达达发单缺少运单号");
		}
		dadaLocalDeliveryReceiptPort.addAfterQuery(companyId, existing);
		return null;
	}

	private static void applyTotalFreightPointAdjustment(Map<String, Object> orderInfo) {
		Object totalObj = orderInfo.get("total_fee");
		Object freightObj = orderInfo.get("freight_fee");
		Object pointObj = orderInfo.get("point_fee");
		BigDecimal total = toBigFen(totalObj);
		BigDecimal freight = toBigFen(freightObj);
		BigDecimal point = toBigFen(pointObj);
		BigDecimal adjusted = total.subtract(freight).add(point);
		int totalInt = adjusted.setScale(0, RoundingMode.HALF_UP).intValue();
		orderInfo.put("total_fee", totalInt);
		orderInfo.put("freight_fee", 0);
	}

	private static BigDecimal toBigFen(Object v) {
		if (v == null) {
			return BigDecimal.ZERO;
		}
		if (v instanceof Number n) {
			return BigDecimal.valueOf(n.longValue());
		}
		try {
			return new BigDecimal(v.toString().trim());
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}
}
