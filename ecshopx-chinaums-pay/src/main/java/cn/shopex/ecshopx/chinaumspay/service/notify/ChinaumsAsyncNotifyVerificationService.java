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

package cn.shopex.ecshopx.chinaumspay.service.notify;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChinaumsAsyncNotifyVerificationService {

	private final String notifyMd5Key;

	public ChinaumsAsyncNotifyVerificationService(
			@Value("${ecshopx.ums.notify-md5-key:}") String notifyMd5Key) {
		this.notifyMd5Key = notifyMd5Key;
	}

	public ChinaumsVerifiedNotifyParams verify(Map<String, String> data) {
		if (data == null) {
			throw new IllegalArgumentException("缺少或空的签名字段 sign");
		}
		String sign = data.get("sign");
		if (sign == null || sign.trim().isEmpty()) {
			throw new IllegalArgumentException("缺少或空的签名字段 sign");
		}
		if (notifyMd5Key == null || notifyMd5Key.trim().isEmpty()) {
			throw new IllegalStateException("银联异步通知验签密钥未配置（ecshopx.ums.notify-md5-key）");
		}
		Map<String, String> copy = new LinkedHashMap<>(data);
		copy.remove("sign");
		String assembledNoTrailingAmpersand = buildSignBaseString(copy);
		String toDigest = assembledNoTrailingAmpersand + notifyMd5Key;
		byte[] digest = sha256Utf8(toDigest);
		String expected = HexFormat.of().withUpperCase().formatHex(digest);
		if (!MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.UTF_8),
				sign.getBytes(StandardCharsets.UTF_8))) {
			throw new IllegalStateException("验签失败，请检查银联支付相关配置是否有修改");
		}
		String mer = data.get("merOrderId");
		String outTradeNo = (mer == null || mer.length() <= 4) ? "" : mer.substring(4);
		String tradeNo = Objects.toString(data.get("targetOrderId"), "");
		return new ChinaumsVerifiedNotifyParams(data.get("status"), "chinaums", outTradeNo, tradeNo);
	}

	private static byte[] sha256Utf8(String s) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * 与网关侧组串一致：按键名做字典序（字符串序）、跳过值为 null 的项、空串仍参与拼接；
	 * 形如 {@code k1=v1&k2=v2}（无末尾 {@code &}），对应组串结果去掉末尾分隔符后再与密钥拼接做摘要。
	 */
	private static String buildSignBaseString(Map<String, String> params) {
		if (params == null || params.isEmpty()) {
			return "";
		}
		TreeMap<String, String> sorted = new TreeMap<>();
		for (Map.Entry<String, String> e : params.entrySet()) {
			if (e.getValue() == null) {
				continue;
			}
			sorted.put(e.getKey(), e.getValue());
		}
		if (sorted.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			sb.append(e.getKey()).append('=').append(e.getValue()).append('&');
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}
}
