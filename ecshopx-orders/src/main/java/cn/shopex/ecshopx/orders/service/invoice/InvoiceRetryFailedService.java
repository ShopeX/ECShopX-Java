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

package cn.shopex.ecshopx.orders.service.invoice;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceLog;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceLogMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InvoiceRetryFailedService {

	private static final Logger log = LoggerFactory.getLogger(InvoiceRetryFailedService.class);

	private static final DateTimeFormatter LOG_TIME_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final OrderInvoiceMapper orderInvoiceMapper;
	private final OrderInvoiceLogMapper orderInvoiceLogMapper;
	private final ObjectMapper objectMapper;

	public InvoiceRetryFailedService(
			OrderInvoiceMapper orderInvoiceMapper,
			OrderInvoiceLogMapper orderInvoiceLogMapper,
			ObjectMapper objectMapper) {
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.orderInvoiceLogMapper = orderInvoiceLogMapper;
		this.objectMapper = objectMapper;
	}

	public void retryFailedInvoice(long companyId, long operatorId, Map<String, Object> merged) {
		log.info("[retryFailedInvoice] start companyId={} operatorId={}", companyId, operatorId);

		Object raw = merged == null ? null : merged.get("invoice_id");
		if (raw == null) {
			throw new BadRequestException("发票ID缺失（invoice_id）");
		}
		String trimmed = String.valueOf(raw).trim();
		if (trimmed.isEmpty() || "0".equals(trimmed)) {
			throw new BadRequestException("发票ID缺失（invoice_id）");
		}

		final long invoiceId;
		try {
			invoiceId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("发票不存在");
		}

		log.info("[retryFailedInvoice] invoiceIdResolved companyId={} operatorId={} invoiceId={}",
				companyId, operatorId, invoiceId);

		OrderInvoice row = orderInvoiceMapper.selectOne(new LambdaQueryWrapper<OrderInvoice>()
				.eq(OrderInvoice::getId, invoiceId)
				.eq(OrderInvoice::getCompanyId, companyId));
		if (row == null) {
			throw new ResourceException("发票不存在");
		}
		if (!"failed".equals(row.getInvoiceStatus())) {
			throw new ResourceException("发票状态不能重新开票");
		}

		int current = Optional.ofNullable(row.getTryTimes()).orElse(0);
		int newTryTimes = current + 1;
		log.info("[retryFailedInvoice] tryTimes companyId={} operatorId={} invoiceId={} tryTimesOld={} tryTimesNew={}",
				companyId, operatorId, invoiceId, current, newTryTimes);

		LambdaUpdateWrapper<OrderInvoice> uw = new LambdaUpdateWrapper<OrderInvoice>()
				.eq(OrderInvoice::getId, invoiceId)
				.set(OrderInvoice::getInvoiceStatus, "pending")
				.set(OrderInvoice::getTryTimes, newTryTimes);
		int n = orderInvoiceMapper.update(null, uw);
		log.info("[retryFailedInvoice] update companyId={} operatorId={} invoiceId={} updateRows={}",
				companyId, operatorId, invoiceId, n);
		if (n <= 0) {
			throw new ResourceException("重新开票失败");
		}

		row.setInvoiceStatus("pending");
		row.setTryTimes(newTryTimes);
		createInvoiceLog(invoiceId, "admin", "重新开票", row, operatorId);

		log.info("[retryFailedInvoice] invoiceLogRecorded companyId={} operatorId={} invoiceId={}",
				companyId, operatorId, invoiceId);
	}

	private void createInvoiceLog(
			long invoiceId,
			String operatorType,
			String message,
			OrderInvoice dataSnapshot,
			long operatorId) {
		try {
			Map<String, Object> content = new LinkedHashMap<>();
			content.put("title", "发票操作");
			content.put("remark", message);
			content.put("data", objectMapper.convertValue(dataSnapshot, new TypeReference<Map<String, Object>>() {}));
			content.put("action_type", operatorType);
			content.put(
					"create_time",
					ZonedDateTime.now(ZoneId.systemDefault()).format(LOG_TIME_FORMAT));
			String jsonString = objectMapper.writeValueAsString(content);

			OrderInvoiceLog logRow = new OrderInvoiceLog();
			logRow.setInvoiceId(invoiceId);
			logRow.setOperatorType(operatorType);
			logRow.setOperatorId(operatorId);
			logRow.setOperatorContent(jsonString);
			logRow.setCreateTime((int) (System.currentTimeMillis() / 1000));
			orderInvoiceLogMapper.insert(logRow);
		} catch (Exception e) {
			log.error("记录发票日志失败", e);
		}
	}
}
