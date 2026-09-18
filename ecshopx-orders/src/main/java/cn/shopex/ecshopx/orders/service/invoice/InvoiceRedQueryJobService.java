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

import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.orders.service.fapiao.hangxin.HangxinFapiaoService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Bus job consumer: queries Hangxin for red invoice PDF URL and persists {@code invoice_file_url_red} on
 * {@code orders_invoice} when the row is still {@code waste} without a red file URL.
 */
@Slf4j
@Service
public class InvoiceRedQueryJobService {

	private final HangxinFapiaoService hangxinFapiaoService;
	private final OrderInvoiceMapper orderInvoiceMapper;

	public InvoiceRedQueryJobService(
			HangxinFapiaoService hangxinFapiaoService, OrderInvoiceMapper orderInvoiceMapper) {
		this.hangxinFapiaoService = hangxinFapiaoService;
		this.orderInvoiceMapper = orderInvoiceMapper;
	}

	public void execute(Map<String, Object> jobData) {
		if (jobData == null || jobData.isEmpty()) {
			return;
		}
		Long invoiceId = parseLong(jobData.get("id"));
		Long companyId = parseLong(jobData.get("company_id"));
		String orderId = str(jobData.get("order_id"));
		String redConfirmSerial = str(jobData.get("red_confirm_serial_no"));
		if (invoiceId == null || invoiceId <= 0) {
			log.warn("[InvoiceRedQueryJobService][execute] missing id keys={}", jobData.keySet());
			return;
		}
		if (companyId == null || companyId <= 0 || !StringUtils.hasText(orderId) || !StringUtils.hasText(redConfirmSerial)) {
			log.warn("[InvoiceRedQueryJobService][execute] missing keys keys={}", jobData.keySet());
			return;
		}

		OrderInvoice inv = orderInvoiceMapper.selectById(invoiceId);
		if (inv == null) {
			log.warn("[InvoiceRedQueryJobService][execute] invoice not found invoice_id={}", invoiceId);
			return;
		}
		if (!companyId.equals(inv.getCompanyId())) {
			log.warn(
					"[InvoiceRedQueryJobService][execute] company_id mismatch invoice_id={} expected={} actual={}",
					invoiceId,
					companyId,
					inv.getCompanyId());
			return;
		}
		String persistedOrderId = str(inv.getOrderId());
		if (!orderId.equals(persistedOrderId)) {
			log.warn(
					"[InvoiceRedQueryJobService][execute] order_id mismatch invoice_id={} expected={} actual={}",
					invoiceId,
					orderId,
					persistedOrderId);
			return;
		}
		if (!"waste".equals(inv.getInvoiceStatus())) {
			log.info(
					"[InvoiceRedQueryJobService][execute] skip non-waste invoice_id={} status={}",
					invoiceId,
					inv.getInvoiceStatus());
			return;
		}
		if (StringUtils.hasText(inv.getInvoiceFileUrlRed())) {
			log.info("[InvoiceRedQueryJobService][execute] skip red URL already set invoice_id={}", invoiceId);
			return;
		}

		log.info("[InvoiceRedQueryJobService][execute] start invoice_id={}", invoiceId);
		try {
			Map<String, Object> fapiaoinfoRed = new LinkedHashMap<>();
			fapiaoinfoRed.put("FPQQLSH", redConfirmSerial);
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("company_id", companyId);
			params.put("order_id", orderId);
			params.put("kptype", "2");
			params.put("fapiaoinfo_red", fapiaoinfoRed);

			Map<String, Object> res = hangxinFapiaoService.getFapiao(params);
			boolean ok = "0000".equals(str(res.get("returnCode")));
			if (!ok) {
				log.error(
						"[InvoiceRedQueryJobService][execute] hangxin failure invoice_id={} returnCode={} message={}",
						invoiceId,
						res.get("returnCode"),
						res.get("returnMessage"));
				return;
			}

			String pdfUrl = extractPdfUrl(res);
			if (!StringUtils.hasText(pdfUrl)) {
				log.info("[InvoiceRedQueryJobService][execute] no red PDF in response invoice_id={}", invoiceId);
				return;
			}
			int now = (int) (System.currentTimeMillis() / 1000L);
			LambdaUpdateWrapper<OrderInvoice> uw =
					new LambdaUpdateWrapper<OrderInvoice>()
							.eq(OrderInvoice::getId, invoiceId)
							.set(OrderInvoice::getInvoiceFileUrlRed, pdfUrl)
							.set(OrderInvoice::getUpdateTime, now);
			orderInvoiceMapper.update(null, uw);
			log.info("[InvoiceRedQueryJobService][execute] updated red PDF invoice_id={}", invoiceId);
		} catch (Exception e) {
			log.error(
					"[InvoiceRedQueryJobService][execute] exception invoice_id={} msg={}",
					invoiceId,
					e.getMessage(),
					e);
			throw e;
		}
	}

	private static String extractPdfUrl(Map<String, Object> res) {
		Object raw = res.get("result");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return "";
		}
		Object row0 = list.get(0);
		if (!(row0 instanceof Map<?, ?> m)) {
			return "";
		}
		Object u = m.get("c_url");
		return u == null ? "" : String.valueOf(u).trim();
	}

	private static Long parseLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
