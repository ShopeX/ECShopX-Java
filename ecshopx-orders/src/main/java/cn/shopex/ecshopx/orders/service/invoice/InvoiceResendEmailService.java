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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class InvoiceResendEmailService {

	private static final Logger log = LoggerFactory.getLogger(InvoiceResendEmailService.class);

	/** Fixed subject line for the send-invoice-email job payload (admin resend). */
	private static final String SEND_INVOICE_EMAIL_SUBJECT = "您的电子发票已生成";

	private static final String MSG_REQUIRED = "orders.order.required_params_missing";
	private static final String DEFAULT_REQUIRED = "必要参数缺失";
	private static final String MSG_NOT_FOUND = "orders.order.invoice_not_found";
	private static final String DEFAULT_NOT_FOUND = "OrdersBundle/Order.invoice_not_found";
	private static final String MSG_NOT_SUCCESS = "orders.order.invoice_not_success_or_no_file";
	private static final String DEFAULT_NOT_SUCCESS = "发票未开成功或没有发票文件";

	private final OrderInvoiceMapper orderInvoiceMapper;
	private final MessageSource messageSource;
	private final SendInvoiceEmailJobDispatchPublisher sendInvoiceEmailJobDispatchPublisher;

	public InvoiceResendEmailService(
			OrderInvoiceMapper orderInvoiceMapper,
			MessageSource messageSource,
			SendInvoiceEmailJobDispatchPublisher sendInvoiceEmailJobDispatchPublisher) {
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.messageSource = messageSource;
		this.sendInvoiceEmailJobDispatchPublisher = sendInvoiceEmailJobDispatchPublisher;
	}

	public void resendInvoice(long companyId, Map<String, Object> merged) {
		Object idObj = merged.get("id");
		Object emailObj = merged.get("confirm_email");

		Locale locale = resolveLocale();
		// Resend requires both ids: treat null, "", "0" as empty, but not whitespace-only strings.
		if (isLooseEmpty(idObj) || isLooseEmpty(emailObj)) {
			throw new ResourceException(messageSource.getMessage(MSG_REQUIRED, null, DEFAULT_REQUIRED, locale));
		}

		String idStr = String.valueOf(idObj);
		long invoiceId;
		try {
			String idForParse = idStr.trim();
			if (idForParse.isEmpty()) {
				throw new NumberFormatException();
			}
			invoiceId = Long.parseLong(idForParse);
		} catch (NumberFormatException e) {
			throw new ResourceException(messageSource.getMessage(MSG_NOT_FOUND, null, DEFAULT_NOT_FOUND, locale));
		}

		OrderInvoice row = orderInvoiceMapper.selectOne(
				new LambdaQueryWrapper<OrderInvoice>()
						.eq(OrderInvoice::getId, invoiceId)
						.eq(OrderInvoice::getCompanyId, companyId));
		if (row == null) {
			throw new ResourceException(messageSource.getMessage(MSG_NOT_FOUND, null, DEFAULT_NOT_FOUND, locale));
		}

		String url = row.getInvoiceFileUrl();
		boolean urlMissing = (url == null || url.isBlank() || "0".equals(url));
		if (!"success".equals(row.getInvoiceStatus()) || urlMissing) {
			throw new ResourceException(messageSource.getMessage(MSG_NOT_SUCCESS, null, DEFAULT_NOT_SUCCESS, locale));
		}

		String confirmEmailForSend = String.valueOf(emailObj).trim();
		try {
			sendInvoiceEmailJobDispatchPublisher.publish(
					confirmEmailForSend, url.trim(), companyId, SEND_INVOICE_EMAIL_SUBJECT);
		} catch (Exception e) {
			log.error("分发发票邮件任务失败", e);
		}
	}

	/**
	 * Loose empty for request scalars: true for null, {@code ""}, {@code "0"}; false for whitespace-only strings.
	 */
	private static boolean isLooseEmpty(Object v) {
		if (v == null) {
			return true;
		}
		String s = String.valueOf(v);
		return s.isEmpty() || "0".equals(s);
	}

	private static Locale resolveLocale() {
		Locale l = LocaleContextHolder.getLocale();
		return l != null ? l : Locale.SIMPLIFIED_CHINESE;
	}
}
