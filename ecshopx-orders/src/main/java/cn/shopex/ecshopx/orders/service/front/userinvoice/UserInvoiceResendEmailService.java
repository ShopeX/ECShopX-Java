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

package cn.shopex.ecshopx.orders.service.front.userinvoice;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.common.dispatch.SendInvoiceEmailJobDispatchPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserInvoiceResendEmailService {

	private static final Logger log = LoggerFactory.getLogger(UserInvoiceResendEmailService.class);

	private static final String MSG_EMAIL_REQUIRED = "orders.order.email_required";
	private static final String DEFAULT_EMAIL_REQUIRED = "请输入电子邮箱";
	private static final String MSG_EMAIL_INVALID = "orders.order.email_invalid_format";
	private static final String DEFAULT_EMAIL_INVALID = "邮箱格式不正确";
	private static final String MSG_RESEND_FAILED = "orders.order.invoice_resend_failed";
	private static final String DEFAULT_RESEND_FAILED = "重发发票失败";
	private static final String MSG_RESEND_SUCCESS = "orders.order.invoice_resend_success";
	private static final String DEFAULT_RESEND_SUCCESS = "发票已重新发送到您的邮箱";

	private final OrderInvoiceMapper orderInvoiceMapper;
	private final UserInvoiceResendEmailTxService userInvoiceResendEmailTxService;
	private final SendInvoiceEmailJobDispatchPublisher sendInvoiceEmailJobDispatchPublisher;
	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;

	public UserInvoiceResendEmailService(
			OrderInvoiceMapper orderInvoiceMapper,
			UserInvoiceResendEmailTxService userInvoiceResendEmailTxService,
			SendInvoiceEmailJobDispatchPublisher sendInvoiceEmailJobDispatchPublisher,
			MessageSource messageSource,
			ObjectMapper objectMapper) {
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.userInvoiceResendEmailTxService = userInvoiceResendEmailTxService;
		this.sendInvoiceEmailJobDispatchPublisher = sendInvoiceEmailJobDispatchPublisher;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> resendInvoiceEmail(long userId, long companyId, Map<String, Object> merged) {
		Locale locale = resolveLocale();
		if (merged == null || !merged.containsKey("confirm_email")) {
			throw new ResourceException(
					messageSource.getMessage(MSG_EMAIL_REQUIRED, null, DEFAULT_EMAIL_REQUIRED, locale));
		}

		String confirmEmail = String.valueOf(merged.get("confirm_email")).trim();
		if (confirmEmail.isEmpty()) {
			throw new ResourceException(
					messageSource.getMessage(MSG_EMAIL_INVALID, null, DEFAULT_EMAIL_INVALID, locale));
		}

		try {
			InternetAddress addr = new InternetAddress(confirmEmail, false);
			addr.validate();
		} catch (AddressException e) {
			throw new ResourceException(
					messageSource.getMessage(MSG_EMAIL_INVALID, null, DEFAULT_EMAIL_INVALID, locale));
		}

		Object idObj = merged.get("id");
		OrderInvoice row = null;
		if (idObj != null) {
			String idTrim = String.valueOf(idObj).trim();
			if (!idTrim.isEmpty()) {
				try {
					long invoiceId = Long.parseLong(idTrim);
					row =
							orderInvoiceMapper.selectOne(
									new LambdaQueryWrapper<OrderInvoice>()
											.eq(OrderInvoice::getId, invoiceId)
											.eq(OrderInvoice::getUserId, userId)
											.eq(OrderInvoice::getCompanyId, companyId)
											.last("LIMIT 1"));
				} catch (NumberFormatException e) {
					row = null;
				}
			}
		}

		if (row == null) {
			throw new ResourceException(
					messageSource.getMessage(MSG_RESEND_FAILED, null, DEFAULT_RESEND_FAILED, locale));
		}

		String url = row.getInvoiceFileUrl();
		boolean urlInvalid = (url == null || url.isBlank() || "0".equals(url));
		if (urlInvalid) {
			throw new ResourceException(
					messageSource.getMessage(MSG_RESEND_FAILED, null, DEFAULT_RESEND_FAILED, locale));
		}

		String dbEmail = row.getEmail() == null ? "" : row.getEmail();
		long nowSec = System.currentTimeMillis() / 1000L;
		if (!confirmEmail.equals(dbEmail)) {
			String remark = "更新发票邮箱:" + dbEmail + "=>" + confirmEmail;
			List<Map<String, Object>> paramsList = new ArrayList<>();
			Map<String, Object> p = new LinkedHashMap<>();
			p.put("field", "email");
			p.put("name", "电子邮箱");
			p.put("oldValue", dbEmail);
			p.put("newValue", confirmEmail);
			paramsList.add(p);
			Map<String, Object> logPayload = new LinkedHashMap<>();
			logPayload.put("title", "更新发票邮箱");
			logPayload.put("remark", remark);
			logPayload.put("params", paramsList);
			String operatorContentJson;
			try {
				operatorContentJson = objectMapper.writeValueAsString(logPayload);
			} catch (JsonProcessingException e) {
				throw new ResourceException("记录发票日志失败");
			}
			long invoicePk = row.getId();
			userInvoiceResendEmailTxService.applyEmailChangeAndLog(
					invoicePk, row, confirmEmail, nowSec, operatorContentJson);
		}

		String urlToSend = url.trim();
		try {
			sendInvoiceEmailJobDispatchPublisher.publish(confirmEmail, urlToSend, companyId);
		} catch (Exception e) {
			log.error("分发发票邮件任务失败", e);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put(
				"message",
				messageSource.getMessage(MSG_RESEND_SUCCESS, null, DEFAULT_RESEND_SUCCESS, locale));
		return out;
	}

	private static Locale resolveLocale() {
		Locale l = LocaleContextHolder.getLocale();
		return l != null ? l : Locale.SIMPLIFIED_CHINESE;
	}
}
