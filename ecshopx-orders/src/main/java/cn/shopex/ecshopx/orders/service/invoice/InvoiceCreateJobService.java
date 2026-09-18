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
 * Queue consumer for {@code InvoiceCreateJob}: Hangxin invoice issuance and persisting outcomes to {@code orders_invoice}
 * (pending flow, Hangxin failures, and {@code OrderInvoiceService#createFapiao} integration path).
 */
@Slf4j
@Service
public class InvoiceCreateJobService {

	private final HangxinFapiaoService hangxinFapiaoService;
	private final OrderInvoiceMapper orderInvoiceMapper;

	public InvoiceCreateJobService(
			HangxinFapiaoService hangxinFapiaoService, OrderInvoiceMapper orderInvoiceMapper) {
		this.hangxinFapiaoService = hangxinFapiaoService;
		this.orderInvoiceMapper = orderInvoiceMapper;
	}

	public void execute(Map<String, Object> jobData) {
		if (jobData == null || jobData.isEmpty()) {
			return;
		}
		Long invoiceId = parseLong(jobData.get("invoice_id"));
		if (invoiceId == null || invoiceId <= 0) {
			log.warn("[InvoiceCreateJobService][execute] missing invoice_id keys={}", jobData.keySet());
			return;
		}

		OrderInvoice inv = orderInvoiceMapper.selectById(invoiceId);
		if (inv == null) {
			log.warn("[InvoiceCreateJobService][execute] invoice not found invoice_id={}", invoiceId);
			return;
		}
		if (!"pending".equals(inv.getInvoiceStatus())) {
			log.info(
					"[InvoiceCreateJobService][execute] skip non-pending invoice_id={} status={}",
					invoiceId,
					inv.getInvoiceStatus());
			return;
		}

		log.info("[InvoiceCreateJobService][execute] start invoice_id={}", invoiceId);
		try {
			Map<String, Object> params = buildHangxinParams(inv);
			Map<String, Object> res = hangxinFapiaoService.createFapiao(params);
			boolean ok = "0000".equals(str(res.get("returnCode")));
			if (!ok) {
				log.error(
						"[InvoiceCreateJobService][execute] hangxin failure invoice_id={} returnCode={} message={}",
						invoiceId,
						res.get("returnCode"),
						res.get("returnMessage"));
				markInvoiceFailed(invoiceId);
				return;
			}

			String pdfUrl = extractPdfUrl(res);
			String fpqqlsh = str(res.get("FPQQLSH"));
			int now = (int) (System.currentTimeMillis() / 1000L);
			LambdaUpdateWrapper<OrderInvoice> uw =
					new LambdaUpdateWrapper<OrderInvoice>()
							.eq(OrderInvoice::getId, invoiceId)
							.set(OrderInvoice::getUpdateTime, now);
			if (StringUtils.hasText(pdfUrl)) {
				uw.set(OrderInvoice::getInvoiceFileUrl, pdfUrl);
				uw.set(OrderInvoice::getInvoiceStatus, "success");
			} else if (StringUtils.hasText(fpqqlsh)) {
				uw.set(OrderInvoice::getInvoiceApplyBn, fpqqlsh);
				uw.set(OrderInvoice::getInvoiceStatus, "inProgress");
			} else {
				uw.set(OrderInvoice::getInvoiceStatus, "inProgress");
			}
			orderInvoiceMapper.update(null, uw);
			log.info("[InvoiceCreateJobService][execute] done invoice_id={}", invoiceId);
		} catch (Exception e) {
			log.error(
					"[InvoiceCreateJobService][execute] exception invoice_id={} msg={}",
					invoiceId,
					e.getMessage());
			markInvoiceFailed(invoiceId, e);
			throw e;
		}
	}

	private void markInvoiceFailed(long invoiceId) {
		markInvoiceFailed(invoiceId, null);
	}

	private void markInvoiceFailed(long invoiceId, Exception cause) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<OrderInvoice> uw =
				new LambdaUpdateWrapper<OrderInvoice>()
						.eq(OrderInvoice::getId, invoiceId)
						.set(OrderInvoice::getInvoiceStatus, "failed")
						.set(OrderInvoice::getUpdateTime, now);
		orderInvoiceMapper.update(null, uw);
		if (cause != null) {
			log.error("[InvoiceCreateJobService] invoice_id={} marked failed", invoiceId, cause);
		} else {
			log.warn("[InvoiceCreateJobService] invoice_id={} marked failed (hangxin business error)", invoiceId);
		}
	}

	private static Map<String, Object> buildHangxinParams(OrderInvoice inv) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", inv.getCompanyId());
		params.put("order_id", str(inv.getOrderId()));
		params.put("kptype", "1");
		params.put("title", str(inv.getCompanyTitle()));
		params.put("invoice_title", str(inv.getCompanyTitle()));
		params.put("taxpayer_id", str(inv.getCompanyTaxNumber()));
		params.put("tax_no", str(inv.getCompanyTaxNumber()));
		params.put("address", str(inv.getCompanyAddress()));
		params.put("telephone", str(inv.getCompanyTelephone()));
		params.put("bank_name", str(inv.getBankName()));
		params.put("bank_account", str(inv.getBankAccount()));
		params.put("email", str(inv.getEmail()));
		if (StringUtils.hasText(inv.getMobile())) {
			params.put("mobile", inv.getMobile().trim());
		}
		return params;
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
