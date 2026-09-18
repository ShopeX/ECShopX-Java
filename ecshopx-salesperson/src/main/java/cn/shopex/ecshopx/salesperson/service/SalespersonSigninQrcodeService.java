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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.service.signin.SalespersonSigninWxcodeStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SalespersonSigninQrcodeService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final String pcWxcodeLoginSecondsRaw;

	public SalespersonSigninQrcodeService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			@Value("${ecshopx.salesperson.pc-wxcode-login-seconds:}") String pcWxcodeLoginSecondsRaw) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.pcWxcodeLoginSecondsRaw = pcWxcodeLoginSecondsRaw == null ? "" : pcWxcodeLoginSecondsRaw;
	}

	public Map<String, Object> getSigninQrcode(
			long companyId, String type, String distributorIdRaw, String salespersonIdRaw) {
		String typeTrimmed = type == null ? "" : type.trim();
		String distributorTrimmed = distributorIdRaw == null ? "" : distributorIdRaw.trim();
		boolean typeMissing = !StringUtils.hasText(typeTrimmed);
		boolean distributorMissing = !StringUtils.hasText(distributorTrimmed);

		if (typeMissing && distributorMissing) {
			Map<String, List<String>> errors = new LinkedHashMap<>();
			errors.put("type", List.of("validation.required"));
			errors.put("distributor_id", List.of("validation.required"));
			throw new BadRequestException("参数错误.", errors, 422);
		}
		if (typeMissing) {
			throw new BadRequestException(
					"参数错误.", Map.of("type", List.of("validation.required")), 422);
		}
		if (distributorMissing) {
			throw new BadRequestException(
					"参数错误.", Map.of("distributor_id", List.of("validation.required")), 422);
		}
		if (!"signin".equals(typeTrimmed) && !"signout".equals(typeTrimmed)) {
			throw new BadRequestException(
					"参数错误.", Map.of("type", List.of("validation.in")), 422);
		}

		String sid = salespersonIdRaw == null ? "" : salespersonIdRaw.trim();
		if ("signout".equals(typeTrimmed) && (!StringUtils.hasText(sid) || "0".equals(sid))) {
			throw new BadRequestException("导购员id必填", 422);
		}

		long nowSec = Instant.now().getEpochSecond();
		String timesKey = "salesperson:signin:times:" + nowSec;
		Long num = companysRedisTemplate.opsForValue().increment(timesKey);
		companysRedisTemplate.expire(timesKey, Duration.ofSeconds(60));

		String token = new Hashids(Long.toString(nowSec), 12).encode(Objects.requireNonNull(num).longValue());

		int expOffsetSec;
		int ttlSeconds;
		Integer cfg = parseOptionalPositiveSeconds(pcWxcodeLoginSecondsRaw);
		if (cfg == null || cfg <= 0) {
			expOffsetSec = 120;
			ttlSeconds = 3720;
		} else {
			expOffsetSec = cfg;
			ttlSeconds = cfg;
		}
		long expEpochSec = nowSec + expOffsetSec;

		Object distributorJson = jsonScalarForIdSegment(distributorTrimmed);
		Object salespersonJson = jsonScalarForIdSegmentWithBlankDefault(salespersonIdRaw, 0L);

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("exp", expEpochSec);
		payload.put("time", nowSec);
		payload.put("status", SalespersonSigninWxcodeStatus.STATUS_WXCODE_WRIT);
		payload.put("distributor_id", distributorJson);
		payload.put("salesperson_id", salespersonJson);

		String redisKey = "salesperson:signin:" + token;
		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("签到令牌数据序列化失败", e);
		}
		companysRedisTemplate.opsForValue().set(redisKey, json, Duration.ofSeconds(ttlSeconds));

		String scene = "pages/index?" + buildQuery(typeTrimmed, companyId, token);
		byte[] png = qrcodePng120(scene);
		String base64Image = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(png);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("base64Image", base64Image);
		out.put("access_token", token);
		return out;
	}

	private static Integer parseOptionalPositiveSeconds(String raw) {
		String t = raw == null ? "" : raw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * 整段为十进制非负整数（仅数字）则写入 JSON 数字，否则写入 JSON 字符串。
	 */
	private static Object jsonScalarForIdSegment(String trimmedNonEmpty) {
		if (trimmedNonEmpty.matches("[0-9]+")) {
			try {
				return Long.parseLong(trimmedNonEmpty);
			} catch (NumberFormatException e) {
				return trimmedNonEmpty;
			}
		}
		return trimmedNonEmpty;
	}

	private static Object jsonScalarForIdSegmentWithBlankDefault(String raw, long blankDefault) {
		String t = raw == null ? "" : raw.trim();
		if (!StringUtils.hasText(t)) {
			return blankDefault;
		}
		return jsonScalarForIdSegment(t);
	}

	private static String buildQuery(String typeTrimmed, long companyId, String token) {
		String encType = URLEncoder.encode(typeTrimmed, StandardCharsets.UTF_8);
		String encCid = URLEncoder.encode(String.valueOf(companyId), StandardCharsets.UTF_8);
		String encT = URLEncoder.encode(token, StandardCharsets.UTF_8);
		return "type=" + encType + "&cid=" + encCid + "&t=" + encT;
	}

	private static byte[] qrcodePng120(String scene) {
		try {
			Map<EncodeHintType, Object> hints = new HashMap<>();
			hints.put(EncodeHintType.MARGIN, 0);
			BitMatrix matrix =
					new MultiFormatWriter().encode(scene, BarcodeFormat.QR_CODE, 120, 120, hints);
			BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			return baos.toByteArray();
		} catch (WriterException | IOException e) {
			throw new ResourceException("二维码生成失败");
		}
	}
}
