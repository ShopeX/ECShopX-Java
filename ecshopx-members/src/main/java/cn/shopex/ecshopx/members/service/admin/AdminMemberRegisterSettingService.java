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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberRegisterRequestFieldsSettingPort;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class AdminMemberRegisterSettingService {

	private static final Logger log = LoggerFactory.getLogger(AdminMemberRegisterSettingService.class);

	private static final int IMAGE_VCODE_TTL_SECONDS = 300;
	private static final int CAPTCHA_CHAR_COUNT = 4;
	private static final int IMAGE_WIDTH = 150;
	private static final int IMAGE_HEIGHT = 48;
	private static final String PHRASE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	private final StringRedisTemplate membersRedis;
	private final ShopProtocolSetService shopProtocolSetService;
	private final AdminMemberRegisterRequestFieldsSettingPort registerRequestFieldsSettingPort;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	public AdminMemberRegisterSettingService(
			@Qualifier("membersStringRedisTemplate") StringRedisTemplate membersRedis,
			ShopProtocolSetService shopProtocolSetService,
			AdminMemberRegisterRequestFieldsSettingPort registerRequestFieldsSettingPort,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.membersRedis = membersRedis;
		this.shopProtocolSetService = shopProtocolSetService;
		this.registerRequestFieldsSettingPort = registerRequestFieldsSettingPort;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> getMemberRegItems(long companyId, HttpServletRequest request) {
		String acceptLang = RequestLangTag.current(langueProperties);
		if (acceptLang != null && acceptLang.isBlank()) {
			acceptLang = null;
		}
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("setting", registerRequestFieldsSettingPort.loadSettingByKeyName(companyId, acceptLang));
		payload.put("registerSettingStatus", Boolean.TRUE);
		Map<String, Object> typeBlock = shopProtocolSetService.get(companyId, "member_register", null);
		Object innerObj = typeBlock.get("member_register");
		String content = "";
		if (innerObj instanceof Map<?, ?> inner) {
			Object c = inner.get("content");
			content = c == null ? "" : String.valueOf(c);
		}
		payload.put("content_agreement", content);
		return payload;
	}

	public Optional<Map<String, Object>> readMemberRegSettingRoot(long companyId) {
		String key = "memberRegSetting:" + sha1HexUtf8(String.valueOf(companyId));
		String raw = membersRedis.opsForValue().get(key);
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		try {
			Map<String, Object> root =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			return Optional.of(root);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Map<String, String> generateImageVcode(long companyId, String type) {
		String phrase = randomPhrase(CAPTCHA_CHAR_COUNT);
		byte[] tokenEntropy = new byte[32];
		secureRandom.nextBytes(tokenEntropy);
		String token = DigestUtils.md5DigestAsHex(tokenEntropy);
		String key = memberImageVcodeRedisKey(type, companyId, token);
		membersRedis.opsForValue().set(key, phrase, Duration.ofSeconds(IMAGE_VCODE_TTL_SECONDS));
		log.info(
				"member image vcode redis store: key={}, value={}, expire={}",
				key,
				phrase,
				IMAGE_VCODE_TTL_SECONDS);
		byte[] pngBytes = renderCaptchaPng(phrase);
		String imageData = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
		LinkedHashMap<String, String> out = new LinkedHashMap<>();
		out.put("imageToken", token);
		out.put("imageData", imageData);
		return out;
	}

	public boolean verifyAndConsumeMemberImageVcode(String token, long companyId, String yzm, String type) {
		String key = memberImageVcodeRedisKey(type, companyId, token);
		String stored = membersRedis.opsForValue().get(key);
		String yzmNorm = yzm == null ? "" : yzm.trim();
		if (stored != null && stored.trim().equalsIgnoreCase(yzmNorm)) {
			membersRedis.delete(key);
			return true;
		}
		return false;
	}

	public void setMemberRegItems(long companyId, Map<String, Object> merged) {
		if (merged.containsKey("content") && scalarTruthy(merged.get("content"))) {
			String agreementKey = "memberRegAgreementSetting:" + sha1HexUtf8(String.valueOf(companyId));
			String html = String.valueOf(merged.get("content"));
			membersRedis.opsForValue().set(agreementKey, html);
			String countryCode = resolveCountryCodeFromMerged(merged);
			shopProtocolSetService.updateMemberRegisterFromAdmin(companyId, countryCode, html);
			return;
		}

		boolean registerSettingStatus =
				(merged.get("registerSettingStatus") instanceof Boolean b && b)
						|| "true".equals(String.valueOf(merged.get("registerSettingStatus")).trim());
		Object normalizedSetting = normalizeSettingRoot(merged.get("setting"));
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("registerSettingStatus", registerSettingStatus);
		payload.put("setting", normalizedSetting);
		String itemKey = "memberRegSetting:" + sha1HexUtf8(String.valueOf(companyId));
		try {
			membersRedis.opsForValue().set(itemKey, objectMapper.writeValueAsString(payload));
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

	private static String resolveCountryCodeFromMerged(Map<String, Object> merged) {
		Object raw = merged.get("country_code");
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return null;
		}
		return s;
	}

	private static Object normalizeSettingRoot(Object settingRaw) {
		if (settingRaw == null) {
			return new LinkedHashMap<String, Object>();
		}
		if (settingRaw instanceof List<?> list) {
			if (list.isEmpty()) {
				return new ArrayList<Object>();
			}
			List<Object> out = new ArrayList<>(list.size());
			for (Object elem : list) {
				if (elem instanceof Map<?, ?> m) {
					out.add(normalizeRegItemMap(m));
				} else {
					out.add(elem);
				}
			}
			return out;
		}
		if (settingRaw instanceof Map<?, ?> map) {
			LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : map.entrySet()) {
				if (e.getKey() == null) {
					continue;
				}
				String key = String.valueOf(e.getKey());
				Object val = e.getValue();
				if (val instanceof Map<?, ?> vm) {
					normalized.put(key, normalizeRegItemMap(vm));
				} else {
					normalized.put(key, val);
				}
			}
			return normalized;
		}
		return new LinkedHashMap<String, Object>();
	}

	private static LinkedHashMap<String, Object> normalizeRegItemMap(Map<?, ?> v) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : v.entrySet()) {
			if (e.getKey() == null) {
				continue;
			}
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (!out.containsKey("is_open") || out.get("is_open") == null) {
			out.put("is_open", true);
		} else {
			out.put("is_open", looseStringFalseEquals(out.get("is_open")) ? Boolean.FALSE : Boolean.TRUE);
		}
		if (!out.containsKey("is_required") || out.get("is_required") == null) {
			out.put("is_required", true);
		} else {
			out.put("is_required", looseStringFalseEquals(out.get("is_required")) ? Boolean.FALSE : Boolean.TRUE);
		}
		Object itemsObj = out.get("items");
		if (itemsObj instanceof List<?> list) {
			List<Object> newList = new ArrayList<>(list.size());
			for (Object elem : list) {
				if (elem instanceof Map<?, ?> m) {
					newList.add(normalizeItemElementMap(m));
				} else {
					newList.add(elem);
				}
			}
			out.put("items", newList);
		} else if (itemsObj instanceof Map<?, ?> m) {
			LinkedHashMap<String, Object> newMap = new LinkedHashMap<>();
			for (Map.Entry<?, ?> en : m.entrySet()) {
				if (en.getKey() == null) {
					continue;
				}
				Object ev = en.getValue();
				if (ev instanceof Map<?, ?> im) {
					newMap.put(String.valueOf(en.getKey()), normalizeItemElementMap(im));
				} else {
					newMap.put(String.valueOf(en.getKey()), ev);
				}
			}
			out.put("items", newMap);
		}
		return out;
	}

	private static LinkedHashMap<String, Object> normalizeItemElementMap(Map<?, ?> elem) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : elem.entrySet()) {
			if (e.getKey() == null) {
				continue;
			}
			row.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (row.containsKey("ischecked") && row.get("ischecked") != null) {
			row.put("ischecked", looseStringFalseEquals(row.get("ischecked")) ? Boolean.FALSE : Boolean.TRUE);
		}
		return row;
	}

	/** Whether {@code content} should select the agreement branch (non-empty string except {@code "0"}, etc.). */
	private static boolean scalarTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof String s) {
			return !s.isEmpty() && !"0".equals(s);
		}
		if (v instanceof Number n) {
			return n.doubleValue() != 0.0;
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		return true;
	}

	/**
	 * Checkbox-style flag: literal {@code "false"} or numeric zero means off; JSON boolean {@code false}
	 * does not (absent keys are handled by callers).
	 */
	private static boolean looseStringFalseEquals(Object x) {
		if (x == null) {
			return false;
		}
		if (x instanceof Boolean) {
			return false;
		}
		if (x instanceof String s) {
			return "false".equals(s.trim());
		}
		if (x instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		return false;
	}

	private static String sha1HexUtf8(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String memberImageVcodeRedisKey(String type, long companyId, String token) {
		return "member-" + type + ":company" + companyId + ":" + token;
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
