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

import cn.shopex.ecshopx.orders.domain.OrderInvoiceLog;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Aligns with PHP {@code OrderInvoiceService::handleInvoiceStatusChange}. */
@Service
public class OrderInvoiceStatusChangeHandler {

	private static final Logger log = LoggerFactory.getLogger(OrderInvoiceStatusChangeHandler.class);

	private static final DateTimeFormatter LOG_TIME_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final OrderInvoiceLogMapper orderInvoiceLogMapper;
	private final ObjectMapper objectMapper;

	public OrderInvoiceStatusChangeHandler(
			OrderInvoiceLogMapper orderInvoiceLogMapper, ObjectMapper objectMapper) {
		this.orderInvoiceLogMapper = orderInvoiceLogMapper;
		this.objectMapper = objectMapper;
	}

	public void handle(long invoiceId, String oldStatus, String newStatus) {
		try {
			log.info(
					"[OrderInvoiceStatusChangeHandler][handle] invoice_id={} old_status={} new_status={}",
					invoiceId,
					oldStatus,
					newStatus);

			Map<String, Object> data = new LinkedHashMap<>();
			data.put("old_status", oldStatus);
			data.put("new_status", newStatus);
			data.put("change_time", ZonedDateTime.now(ZoneId.systemDefault()).format(LOG_TIME_FORMAT));

			Map<String, Object> content = new LinkedHashMap<>();
			content.put("title", "系统操作");
			content.put("remark", "发票状态从 " + oldStatus + " 变更为 " + newStatus);
			content.put("data", data);
			content.put("action_type", "system");
			content.put(
					"create_time",
					ZonedDateTime.now(ZoneId.systemDefault()).format(LOG_TIME_FORMAT));

			String operatorContent = objectMapper.writeValueAsString(content);
			int nowSec = (int) (System.currentTimeMillis() / 1000L);
			OrderInvoiceLog logRow = new OrderInvoiceLog();
			logRow.setInvoiceId(invoiceId);
			logRow.setOperatorType("system");
			logRow.setOperatorId(1L);
			logRow.setOperatorContent(operatorContent);
			logRow.setCreateTime(nowSec);
			logRow.setUpdateTime(nowSec);
			orderInvoiceLogMapper.insert(logRow);

			if ("pending".equals(oldStatus) && "inProgress".equals(newStatus)) {
				log.info(
						"[OrderInvoiceStatusChangeHandler][handle] invoice started inProgress invoice_id={}",
						invoiceId);
			}
			if ("success".equals(newStatus)) {
				log.info(
						"[OrderInvoiceStatusChangeHandler][handle] invoice success invoice_id={}",
						invoiceId);
			}
		} catch (Exception e) {
			log.error(
					"[OrderInvoiceStatusChangeHandler][handle] failed invoice_id={} error={}",
					invoiceId,
					e.getMessage());
		}
	}
}
