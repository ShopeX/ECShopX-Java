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

package cn.shopex.ecshopx.companys.service;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class OperatorsImageVcodeService {

	private static final Logger log = LoggerFactory.getLogger(OperatorsImageVcodeService.class);

	private static final int IMAGE_VCODE_TTL_SECONDS = 300;
	private static final int CAPTCHA_CHAR_COUNT = 4;
	private static final int IMAGE_WIDTH = 150;
	private static final int IMAGE_HEIGHT = 48;
	private static final String PHRASE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final Pattern SAFE_TYPE = Pattern.compile("^[a-zA-Z0-9_-]{1,48}$");

	private final StringRedisTemplate companysRedisTemplate;
	private final SecureRandom secureRandom = new SecureRandom();

	public OperatorsImageVcodeService(@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	/**
	 * Issues a new image captcha for the PC operator flow: Redis key {@code admin-{type}:{token}},
	 * TTL 300 seconds, case-insensitive verify via {@link #checkImageVcode(String, String, String)}.
	 *
	 * @param type raw type query; null or blank normalizes to {@code forget} per {@link #normalizeType(String)}
	 * @return map keys {@code imageToken}, {@code imageData} (data URL with PNG base64)
	 */
	public Map<String, String> generateImageVcode(String type) {
		return buildCaptchaMap(normalizeType(type));
	}

	/**
	 * Issues a new image captcha for the app operator flow: same storage and PNG shape as
	 * {@link #generateImageVcode(String)}, but type defaults to {@code login} when absent or blank.
	 *
	 * @param type optional captcha channel key segment; see {@link #normalizeAppType(String)}
	 * @return map keys {@code imageToken}, {@code imageData} (data URL with PNG base64)
	 */
	public Map<String, String> generateAppImageVcode(String type) {
		return buildCaptchaMap(normalizeAppType(type));
	}

	private Map<String, String> buildCaptchaMap(String normalizedKeyType) {
		String phrase = randomPhrase(CAPTCHA_CHAR_COUNT);
		byte[] tokenEntropy = new byte[32];
		secureRandom.nextBytes(tokenEntropy);
		String token = DigestUtils.md5DigestAsHex(tokenEntropy);
		String key = redisKey(normalizedKeyType, token);
		companysRedisTemplate.opsForValue().set(key, phrase, Duration.ofSeconds(IMAGE_VCODE_TTL_SECONDS));
		log.info("shop forget redis store : key={}, value={}, expire={}", key, phrase, IMAGE_VCODE_TTL_SECONDS);
		byte[] pngBytes = renderCaptchaPng(phrase);
		String imageData = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(pngBytes);
		Map<String, String> out = new LinkedHashMap<>();
		out.put("imageToken", token);
		out.put("imageData", imageData);
		return out;
	}

	private static String normalizeAppType(String type) {
		if (type == null || type.trim().isEmpty()) {
			return "login";
		}
		String t = type.trim();
		if (SAFE_TYPE.matcher(t).matches()) {
			return t;
		}
		return t;
	}

	public boolean checkImageVcode(String token, String vcode, String type) {
		String key = "admin-" + type + ":" + token;
		String stored = companysRedisTemplate.opsForValue().get(key);
		if (stored == null) {
			return false;
		}
		if (stored.equalsIgnoreCase(vcode)) {
			companysRedisTemplate.delete(key);
			return true;
		}
		return false;
	}

	private static String redisKey(String type, String token) {
		return "admin-" + type + ":" + token;
	}

	private static String normalizeType(String type) {
		if (type == null) {
			return "forget";
		}
		String t = type.trim();
		if (t.isEmpty() || !SAFE_TYPE.matcher(t).matches()) {
			return "forget";
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
