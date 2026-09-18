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

import cn.shopex.ecshopx.common.dispatch.SendInvoiceEmailJobDispatchPublisher;
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
 * Bus job consumer: Hangxin blue-invoice query for rows still {@code inProgress}, persisting PDF URL and status
 * transitions consistent with {@link InvoiceCreateJobService}.
 */
@Slf4j
@Service
public class InvoiceQueryJobService {

	private static final String SEND_INVOICE_EMAIL_SUBJECT = "您的电子发票已生成";

	private final HangxinFapiaoService hangxinFapiaoService;
	private final OrderInvoiceMapper orderInvoiceMapper;
	private final SendInvoiceEmailJobDispatchPublisher sendInvoiceEmailJobDispatchPublisher;

	public InvoiceQueryJobService(
			HangxinFapiaoService hangxinFapiaoService,
			OrderInvoiceMapper orderInvoiceMapper,
			SendInvoiceEmailJobDispatchPublisher sendInvoiceEmailJobDispatchPublisher) {
		this.hangxinFapiaoService = hangxinFapiaoService;
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.sendInvoiceEmailJobDispatchPublisher = sendInvoiceEmailJobDispatchPublisher;
	}

	public void execute(Map<String, Object> jobData) {
		if (jobData == null || jobData.isEmpty()) {
			return;
		}
		Long invoiceId = parseLong(jobData.get("invoice_id"));
		Long companyId = parseLong(jobData.get("company_id"));
		String orderId = str(jobData.get("order_id"));
		String invoiceApplyBn = str(jobData.get("invoice_apply_bn"));

		if (invoiceId == null || invoiceId <= 0) {
			log.warn("[InvoiceQueryJobService][execute] missing invoice_id keys={}", jobData.keySet());
			return;
		}
		if (companyId == null || companyId <= 0 || !StringUtils.hasText(orderId)) {
			log.warn("[InvoiceQueryJobService][execute] missing keys keys={}", jobData.keySet());
			return;
		}
		if (!StringUtils.hasText(invoiceApplyBn)) {
			log.warn("[InvoiceQueryJobService][execute] missing invoice_apply_bn invoice_id={}", invoiceId);
			return;
		}

		OrderInvoice inv = orderInvoiceMapper.selectById(invoiceId);
		if (inv == null) {
			log.warn("[InvoiceQueryJobService][execute] invoice not found invoice_id={}", invoiceId);
			return;
		}
		if (!companyId.equals(inv.getCompanyId())) {
			log.warn(
					"[InvoiceQueryJobService][execute] company_id mismatch invoice_id={} expected={} actual={}",
					invoiceId,
					companyId,
					inv.getCompanyId());
			return;
		}
		String persistedOrderId = str(inv.getOrderId());
		if (!orderId.equals(persistedOrderId)) {
			log.warn(
					"[InvoiceQueryJobService][execute] order_id mismatch invoice_id={} expected={} actual={}",
					invoiceId,
					orderId,
					persistedOrderId);
			return;
		}
		if (!"inProgress".equals(inv.getInvoiceStatus())) {
			log.info(
					"[InvoiceQueryJobService][execute] skip non-inProgress invoice_id={} status={}",
					invoiceId,
					inv.getInvoiceStatus());
			return;
		}

		log.info("[InvoiceQueryJobService][execute] start invoice_id={}", invoiceId);
		try {
			Map<String, Object> fapiaoinfo = new LinkedHashMap<>();
			fapiaoinfo.put("FPQQLSH", invoiceApplyBn);
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("company_id", companyId);
			params.put("order_id", orderId);
			params.put("kptype", "1");
			params.put("fapiaoinfo", fapiaoinfo);

			Map<String, Object> res = hangxinFapiaoService.getFapiao(params);
			boolean ok = "0000".equals(str(res.get("returnCode")));
			if (!ok) {
				log.error(
						"[InvoiceQueryJobService][execute] hangxin failure invoice_id={} returnCode={} message={}",
						invoiceId,
						res.get("returnCode"),
						res.get("returnMessage"));
				return;
			}

			int now = (int) (System.currentTimeMillis() / 1000L);
			String pdfUrl = extractPdfUrl(res);
			if (StringUtils.hasText(pdfUrl)) {
				LambdaUpdateWrapper<OrderInvoice> uw =
						new LambdaUpdateWrapper<OrderInvoice>()
								.eq(OrderInvoice::getId, invoiceId)
								.set(OrderInvoice::getInvoiceFileUrl, pdfUrl)
								.set(OrderInvoice::getInvoiceStatus, "success")
								.set(OrderInvoice::getUpdateTime, now);
				orderInvoiceMapper.update(null, uw);
				log.info("[InvoiceQueryJobService][execute] updated PDF invoice_id={}", invoiceId);
				String email = str(inv.getEmail());
				if (StringUtils.hasText(email)) {
					try {
						sendInvoiceEmailJobDispatchPublisher.publish(
								email, pdfUrl.trim(), companyId, SEND_INVOICE_EMAIL_SUBJECT);
					} catch (Exception e) {
						log.error(
								"[InvoiceQueryJobService][execute] send email dispatch failed invoice_id={}",
								invoiceId,
								e);
					}
				}
			} else {
				LambdaUpdateWrapper<OrderInvoice> touch =
						new LambdaUpdateWrapper<OrderInvoice>()
								.eq(OrderInvoice::getId, invoiceId)
								.set(OrderInvoice::getUpdateTime, now);
				orderInvoiceMapper.update(null, touch);
				log.info("[InvoiceQueryJobService][execute] no PDF yet invoice_id={}", invoiceId);
			}
		} catch (Exception e) {
			log.error(
					"[InvoiceQueryJobService][execute] exception invoice_id={} msg={}",
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
