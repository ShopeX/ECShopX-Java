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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.operator.sms.CompanySmsSendPort;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Service
public class DistributorH5ShopCaptchaSmsService {

	private static final Logger log = LoggerFactory.getLogger(DistributorH5ShopCaptchaSmsService.class);

	private static final int IMAGE_VCODE_TTL_SECONDS = 300;
	private static final int SMS_CODE_TTL_SECONDS = 300;
	private static final int CAPTCHA_CHAR_COUNT = 4;
	private static final int IMAGE_WIDTH = 150;
	private static final int IMAGE_HEIGHT = 48;
	private static final String PHRASE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final Pattern SAFE_TYPE = Pattern.compile("^[a-zA-Z0-9_-]{1,48}$");
	private static final List<String> ALLOWED_TYPES = List.of("bind");

	private final StringRedisTemplate companysRedisTemplate;
	private final DistributorH5GetDistributorInfoService distributorH5GetDistributorInfoService;
	private final CompanySmsSendPort companySmsSendPort;
	private final SecureRandom secureRandom = new SecureRandom();

	public DistributorH5ShopCaptchaSmsService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			DistributorH5GetDistributorInfoService distributorH5GetDistributorInfoService,
			CompanySmsSendPort companySmsSendPort) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.distributorH5GetDistributorInfoService = distributorH5GetDistributorInfoService;
		this.companySmsSendPort = companySmsSendPort;
	}

	public Map<String, String> generateImageVcode(long companyId, String typeRaw) {
		String type = normalizeShopCaptchaType(typeRaw);
		if (!ALLOWED_TYPES.contains(type)) {
			throw new ResourceException("图片验证码类型错误");
		}
		String phrase = randomPhrase(CAPTCHA_CHAR_COUNT);
		byte[] tokenEntropy = new byte[32];
		secureRandom.nextBytes(tokenEntropy);
		String token = DigestUtils.md5DigestAsHex(tokenEntropy);
		String key = distributorImageRedisKey(type, companyId, token);
		companysRedisTemplate.opsForValue().set(key, phrase, Duration.ofSeconds(IMAGE_VCODE_TTL_SECONDS));
		log.info(
				"分销商图片验证码已写入 Redis：key={}，expireSeconds={}，phrase={}",
				key,
				IMAGE_VCODE_TTL_SECONDS,
				phrase);
		byte[] pngBytes = renderCaptchaPng(phrase);
		String imageData = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(pngBytes);
		Map<String, String> out = new LinkedHashMap<>();
		out.put("imageToken", token);
		out.put("imageData", imageData);
		return out;
	}

	public Map<String, String> getSmsCode(
			long companyId,
			String distributorIdRaw,
			String yzmRaw,
			String tokenRaw,
			String typeRaw,
			String requestLangTag) {
		StringBuilder missingRequired = new StringBuilder();
		if (!StringUtils.hasText(distributorIdRaw == null ? "" : distributorIdRaw.trim())) {
			missingRequired.append("validation.required，");
		}
		if (!StringUtils.hasText(tokenRaw == null ? "" : tokenRaw.trim())) {
			missingRequired.append("validation.required，");
		}
		if (!StringUtils.hasText(yzmRaw == null ? "" : yzmRaw.trim())) {
			missingRequired.append("validation.required，");
		}
		if (missingRequired.length() > 0) {
			throw new ResourceException(missingRequired.toString());
		}

		String type = normalizeShopCaptchaType(typeRaw);
		if (!ALLOWED_TYPES.contains(type)) {
			throw new ResourceException("手机验证码类型错误");
		}

		String distributorTrim = distributorIdRaw.trim();
		long distributorId;
		try {
			distributorId = Long.parseLong(distributorTrim);
		} catch (NumberFormatException e) {
			throw new ResourceException("店铺或店铺联系手机不存在");
		}

		Map<String, Object> row =
				distributorH5GetDistributorInfoService.getDistributorInfo(companyId, distributorId, requestLangTag);
		if (row == null || row.isEmpty()) {
			throw new ResourceException("店铺或店铺联系手机不存在");
		}
		Object mobileObj = row.get("mobile");
		String phone = mobileObj == null ? "" : String.valueOf(mobileObj).trim();
		if (!StringUtils.hasText(phone) || "0".equals(phone)) {
			throw new ResourceException("店铺或店铺联系手机不存在");
		}

		String yzm = yzmRaw.trim();
		String token = tokenRaw.trim();
		boolean imageCaptchaOk = checkImageVcode(token, companyId, yzm, type);
		if (!imageCaptchaOk) {
			throw new ResourceException("验证码错误");
		}

		String sendCountKey = distributorSmsSendCountKey(phone, companyId, type);
		String cnt = companysRedisTemplate.opsForValue().get(sendCountKey);
		int n = 0;
		if (cnt != null) {
			try {
				n = Integer.parseInt(cnt.trim());
			} catch (NumberFormatException ignored) {
				n = 0;
			}
		}
		if (n >= 5) {
			throw new ResourceException("验证码发送过多");
		}
		companysRedisTemplate.opsForValue().increment(sendCountKey);
		companysRedisTemplate.expire(sendCountKey, Duration.ofHours(24));
		String vcode = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
		String smsKey = distributorSmsRedisKey(type, companyId, phone);
		companysRedisTemplate.opsForValue().set(smsKey, vcode, Duration.ofSeconds(SMS_CODE_TTL_SECONDS));
		companySmsSendPort.sendVerificationCode(companyId, phone, vcode);
		return Map.of("message", "短信发送成功");
	}

	private boolean checkImageVcode(String token, long companyId, String vcode, String type) {
		String t = token == null ? "" : token.trim();
		String v = vcode == null ? "" : vcode.trim();
		if (!StringUtils.hasText(t) || !StringUtils.hasText(v)) {
			return false;
		}
		String key = distributorImageRedisKey(type, companyId, t);
		String stored = companysRedisTemplate.opsForValue().get(key);
		if (stored == null || !stored.trim().equalsIgnoreCase(v)) {
			return false;
		}
		companysRedisTemplate.delete(key);
		return true;
	}

	private static String distributorImageRedisKey(String type, long companyId, String token) {
		return "distributor-" + type + ":company" + companyId + ":" + token;
	}

	private static String distributorSmsRedisKey(String type, long companyId, String phone) {
		return "distributor-" + type + ":company" + companyId + ":" + phone;
	}

	private static String distributorSmsSendCountKey(String phone, long companyId, String type) {
		String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		return "distributor_yzmsend:" + companyId + ":" + day + ":" + type + ":" + phone;
	}

	private static String normalizeShopCaptchaType(String typeRaw) {
		if (typeRaw == null || typeRaw.trim().isEmpty()) {
			return "bind";
		}
		String t = typeRaw.trim();
		if (!SAFE_TYPE.matcher(t).matches()) {
			return "__invalid__";
		}
		return t;
	}

	private String randomPhrase(int len) {
		char[] buf = new char[len];
		for (int i = 0; i < len; i++) {
			buf[i] = PHRASE_ALPHABET.charAt(secureRandom.nextInt(PHRASE_ALPHABET.length()));
		}
		return new String(buf);
	}

	private byte[] renderCaptchaPng(String phrase) {
		BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = image.createGraphics();
		try {
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
			g2.setStroke(new BasicStroke(1.2f));
			for (int n = 0; n < 6; n++) {
				g2.setColor(new Color(secureRandom.nextInt(180), secureRandom.nextInt(180), secureRandom.nextInt(180)));
				int x1 = secureRandom.nextInt(IMAGE_WIDTH);
				int y1 = secureRandom.nextInt(IMAGE_HEIGHT);
				int x2 = secureRandom.nextInt(IMAGE_WIDTH);
				int y2 = secureRandom.nextInt(IMAGE_HEIGHT);
				g2.drawLine(x1, y1, x2, y2);
			}
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			char[] chars = phrase.toCharArray();
			int charCount = chars.length;
			int slot = IMAGE_WIDTH / (charCount + 1);
			for (int i = 0; i < charCount; i++) {
				double rot = (secureRandom.nextDouble() - 0.5) * 0.35;
				g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
				g2.setColor(new Color(secureRandom.nextInt(80), secureRandom.nextInt(80), secureRandom.nextInt(80)));
				var old = g2.getTransform();
				int cx = slot * (i + 1);
				int cy = IMAGE_HEIGHT / 2 + 12;
				g2.rotate(rot, cx, cy);
				g2.drawChars(chars, i, 1, cx - 14, cy);
				g2.setTransform(old);
			}
		} finally {
			g2.dispose();
		}
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			if (!ImageIO.write(image, "png", bos)) {
				throw new IllegalStateException("PNG ImageIO writer not available");
			}
			return bos.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
