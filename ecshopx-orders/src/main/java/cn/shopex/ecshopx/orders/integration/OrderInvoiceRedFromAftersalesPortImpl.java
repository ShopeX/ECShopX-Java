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

package cn.shopex.ecshopx.orders.integration;

import cn.shopex.ecshopx.common.port.orders.OrderInvoiceRedFromAftersalesPort;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Performs the success-branch invoice row transition used after refund approval async jobs. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderInvoiceRedFromAftersalesPortImpl implements OrderInvoiceRedFromAftersalesPort {

	private final OrderInvoiceMapper orderInvoiceMapper;

	@Override
	public void redInvoice(Map<String, Object> jobData) {
		if (jobData == null || jobData.isEmpty()) {
			return;
		}
		Long companyId = extractLongNullable(jobData.get("company_id"));
		String orderId = normalizeOrderId(jobData.get("order_id"));
		if (companyId == null || !StringUtils.hasText(orderId)) {
			log.warn("[InvoiceRedJob][redInvoice] missing company_id or order_id keys={}", jobData.keySet());
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<OrderInvoice> uw =
				new LambdaUpdateWrapper<OrderInvoice>()
						.eq(OrderInvoice::getCompanyId, companyId)
						.eq(OrderInvoice::getOrderId, orderId)
						.eq(OrderInvoice::getInvoiceStatus, "success")
						.set(OrderInvoice::getInvoiceStatus, "waste")
						.set(OrderInvoice::getUpdateTime, now);
		String bn = normalizeBn(jobData.get("aftersales_bn"));
		if (StringUtils.hasText(bn)) {
			uw.set(OrderInvoice::getRedContent, "aftersales_bn=" + bn);
		}
		int rows = orderInvoiceMapper.update(null, uw);
		log.info("[InvoiceRedJob][redInvoice] company_id={} order_id={} rows={}", companyId, orderId, rows);
	}

	private static String normalizeBn(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw).trim();
	}

	private static Long extractLongNullable(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String normalizeOrderId(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof String s) {
			return s.trim();
		}
		return String.valueOf(raw).trim();
	}
}
