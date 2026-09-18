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

package cn.shopex.ecshopx.companys.service.companycreate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * SaaS online-open SMS (legacy Shopex gateway) and email notifications after company creation.
 */
@Service
public class CompanyOnlineOpenNotificationService {

	private static final Logger log = LoggerFactory.getLogger(CompanyOnlineOpenNotificationService.class);

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

	/** Same endpoint and credential layout as the legacy listener implementation. */
	private static final String SMS_API_URL = "http://api.sms.shopex.cn";

	private static final String SMS_ENT_ID = "10023";
	private static final String SMS_ENT_PWD = "efca9b3f71133525fbbeec60284eddd9";
	private static final String SMS_LICENSE = "111";
	private static final String SMS_SOURCE = "603622";
	private static final String SMS_SECRET = "70b3f25f3b334b1fbe6904c565d9f979";

	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;
	private final JavaMailSender mailSender;

	@Value("${common.shop-admin-url:}")
	private String shopAdminUrl;

	public CompanyOnlineOpenNotificationService(
			ObjectMapper objectMapper, org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider) {
		this.objectMapper = objectMapper;
		this.mailSender = mailSenderProvider.getIfAvailable();
		SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
		f.setConnectTimeout(10_000);
		f.setReadTimeout(30_000);
		this.restTemplate = new RestTemplate(f);
	}

	public void sendOpeningSmsIfApplicable(String mobile, long activeAtEpochSeconds, long expiredAtEpochSeconds) {
		if (!StringUtils.hasText(mobile)) {
			return;
		}
		if (!StringUtils.hasText(shopAdminUrl)) {
			log.debug("Opening SMS skipped: shop admin URL not configured");
			return;
		}
		String activeAt = formatDay(activeAtEpochSeconds);
		String expiredAt = formatDay(expiredAtEpochSeconds);
		String text =
				"尊敬的用户，您已于"
						+ activeAt
						+ "成功开通商派云店系统，有效期至"
						+ expiredAt
						+ "。请通过："
						+ shopAdminUrl.trim()
						+ "进入管理端。账户："
						+ mobile
						+ "，密码：注册所用密码。";

		try {
			Map<String, String> sendStr = new LinkedHashMap<>();
			sendStr.put("certi_app", "sms.send");
			sendStr.put("entId", SMS_ENT_ID);
			sendStr.put("entPwd", SMS_ENT_PWD);
			sendStr.put("license", SMS_LICENSE);
			sendStr.put("source", SMS_SOURCE);
			sendStr.put("sendType", "notice");
			sendStr.put("version", "1.0");
			sendStr.put("format", "json");
			sendStr.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
			sendStr.put("contents", buildSmsContentsJson(mobile, text + "【商派】"));
			sendStr.put("certi_ac", certiAc(sendStr, SMS_SECRET));

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			org.springframework.util.LinkedMultiValueMap<String, String> form =
					new org.springframework.util.LinkedMultiValueMap<>();
			for (Map.Entry<String, String> e : sendStr.entrySet()) {
				form.add(e.getKey(), e.getValue());
			}
			ResponseEntity<String> resp =
					restTemplate.postForEntity(SMS_API_URL, new HttpEntity<>(form, headers), String.class);
			log.debug("Opening SMS gateway status={} bodyLen={}", resp.getStatusCode(), resp.getBody() != null ? resp.getBody().length() : 0);
		} catch (JsonProcessingException e) {
			log.warn("Opening SMS payload build failed mobile={} msg={}", mobile, e.getMessage());
		} catch (RuntimeException e) {
			log.warn("Opening SMS send failed mobile={} msg={}", mobile, e.getMessage());
		}
	}

	public void sendOpeningEmailIfApplicable(String to, String mobile, long activeAtEpochSeconds, long expiredAtEpochSeconds) {
		if (!StringUtils.hasText(to)) {
			return;
		}
		if (mailSender == null) {
			log.debug("Opening email skipped: JavaMailSender not configured");
			return;
		}
		if (!StringUtils.hasText(shopAdminUrl)) {
			log.debug("Opening email skipped: shop admin URL not configured");
			return;
		}
		String activeAt = formatDay(activeAtEpochSeconds);
		String expiredAt = formatDay(expiredAtEpochSeconds);
		String url = shopAdminUrl.trim();
		String body =
				"<p>尊敬的用户:</p>"
						+ "<p style=\"text-indent: 2em;\">您已于"
						+ activeAt
						+ "成功开通商派云店系统，有效期至"
						+ expiredAt
						+ ";</p>"
						+ "<p style=\"text-indent: 2em;\">请通过：<a href=\""
						+ url
						+ "\">"
						+ url
						+ "</a>进入管理端;</p>"
						+ "<p style=\"text-indent: 2em;\">账户："
						+ (mobile != null ? mobile : "")
						+ ";</p>"
						+ "<p style=\"text-indent: 2em;\">密码：注册所用密码;</p>";
		try {
			var msg = mailSender.createMimeMessage();
			MimeMessageHelper h = new MimeMessageHelper(msg, false, StandardCharsets.UTF_8.name());
			h.setTo(to.trim());
			h.setSubject("商派云店系统成功开通通知");
			h.setText(body, true);
			mailSender.send(msg);
		} catch (Exception e) {
			log.warn("Opening email send failed to={} msg={}", to, e.getMessage());
		}
	}

	private static String formatDay(long epochSeconds) {
		if (epochSeconds <= 0) {
			return DATE_FMT.format(Instant.now());
		}
		return DATE_FMT.format(Instant.ofEpochSecond(epochSeconds));
	}

	private String buildSmsContentsJson(String phone, String sms) throws JsonProcessingException {
		List<Map<String, String>> content = List.of(Map.of("content", sms, "phones", phone));
		return objectMapper.writeValueAsString(content);
	}

	private static String certiAc(Map<String, String> params, String token) {
		String assemble = assemble(params);
		String md5token = md5Hex(token).toLowerCase();
		return md5Hex(assemble + md5token).toLowerCase();
	}

	private static String assemble(Map<String, String> params) {
		TreeMap<String, String> sorted = new TreeMap<>(params);
		StringBuilder sign = new StringBuilder();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			if ("certi_ac".equals(e.getKey())) {
				continue;
			}
			sign.append(e.getValue());
		}
		return sign.toString();
	}

	private static String md5Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
