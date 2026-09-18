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

package cn.shopex.ecshopx.payment.service.orderrefund;

import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * 微信退款（对位 {@code secapi/pay/refund}，需商户 API 证书）。
 */
@Component
public class WxpaySecapiAftersalesRefundRunner {

	private static final String WX_REFUND_URL = "https://api.mch.weixin.qq.com/secapi/pay/refund";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public WxpaySecapiAftersalesRefundRunner(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> refund(
			long companyId,
			long distributorIdForSetting,
			String wxaAppIdHint,
			String tradeId,
			long refundBn,
			int refundFeeFen,
			int totalPayFeeFen) {
		if (!StringUtils.hasText(tradeId)) {
			return fail("缺少交易单号");
		}
		Map<String, Object> cfg = loadWxCfg(companyId, distributorIdForSetting);
		if (cfg.isEmpty()) {
			return fail("请检查微信支付相关配置是否完成");
		}
		boolean servicer = isServicer(cfg);
		String apiKey = str(cfg.get("key"));
		String merchantId = str(cfg.get("merchant_id"));
		String certMaterial = resolvePemMaterial(cfg, "cert", "cert_url");
		String keyMaterial = resolvePemMaterial(cfg, "cert_key", "cert_key_url");
		if (!StringUtils.hasText(apiKey)
				|| !StringUtils.hasText(merchantId)
				|| !StringUtils.hasText(certMaterial)
				|| !StringUtils.hasText(keyMaterial)) {
			return fail("请检查微信支付相关配置是否完成");
		}
		String appId = str(cfg.get("app_id"));
		if (!StringUtils.hasText(appId)) {
			appId = wxaAppIdHint == null ? "" : wxaAppIdHint.trim();
		}
		if (!StringUtils.hasText(appId)) {
			return fail("请检查微信支付相关配置是否完成");
		}
		int useRefund = refundFeeFen > 0 ? refundFeeFen : totalPayFeeFen;
		int useTotal = totalPayFeeFen > 0 ? totalPayFeeFen : useRefund;

		TreeMap<String, String> p = new TreeMap<>();
		if (servicer) {
			String servicerAppId = str(cfg.get("servicer_app_id"));
			String servicerMchId = str(cfg.get("servicer_merchant_id"));
			if (!StringUtils.hasText(servicerAppId) || !StringUtils.hasText(servicerMchId)) {
				return fail("请检查微信支付相关配置是否完成");
			}
			p.put("appid", servicerAppId);
			p.put("mch_id", servicerMchId);
			p.put("sub_appid", appId);
			p.put("sub_mch_id", merchantId);
		} else {
			p.put("appid", appId);
			p.put("mch_id", merchantId);
		}
		p.put("nonce_str", randomNonce());
		p.put("out_trade_no", tradeId);
		p.put("out_refund_no", String.valueOf(refundBn));
		p.put("total_fee", String.valueOf(useTotal));
		p.put("refund_fee", String.valueOf(useRefund));
		p.put("op_user_id", servicer ? str(cfg.get("servicer_merchant_id")) : merchantId);
		p.put("sign", signParams(p, apiKey));

		String xml = buildXml(p);
		SSLContext ssl;
		try {
			ssl = buildSslContext(certMaterial, keyMaterial);
		} catch (Exception e) {
			return fail("无法加载微信退款证书，请确认证书路径与 PKCS#8 私钥格式");
		}
		HttpClient client = HttpClient.newBuilder().sslContext(ssl).build();
		HttpRequest req =
				HttpRequest.newBuilder()
						.uri(URI.create(WX_REFUND_URL))
						.header("Content-Type", "application/xml")
						.POST(HttpRequest.BodyPublishers.ofString(xml, StandardCharsets.UTF_8))
						.build();
		try {
			HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			String respXml = resp.body();
			if (!StringUtils.hasText(respXml)) {
				return fail("微信退款失败");
			}
			String returnCode = xmlText(respXml, "return_code");
			String resultCode = xmlText(respXml, "result_code");
			if ("SUCCESS".equals(returnCode) && "SUCCESS".equals(resultCode)) {
				Map<String, Object> ok = new LinkedHashMap<>();
				ok.put("status", "SUCCESS");
				ok.put("refund_id", xmlText(respXml, "refund_id"));
				return ok;
			}
			String err = xmlText(respXml, "err_code_des");
			if (!StringUtils.hasText(err)) {
				err = xmlText(respXml, "return_msg");
			}
			Map<String, Object> f = new LinkedHashMap<>();
			f.put("status", "FAIL");
			f.put("error_code", xmlText(respXml, "err_code"));
			f.put("error_desc", StringUtils.hasText(err) ? err : "微信退款失败");
			return f;
		} catch (Exception e) {
			return fail("微信退款请求失败");
		}
	}

	private Map<String, Object> loadWxCfg(long companyId, long distributorIdForSetting) {
		String raw =
				companysRedisTemplate
						.opsForValue()
						.get(PaymentSettingRedisKeys.wxpayRedisKey(companyId, distributorIdForSetting));
		return PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
	}

	private static boolean isServicer(Map<String, Object> cfg) {
		Object v = cfg.get("is_servicer");
		return v != null && "true".equalsIgnoreCase(v.toString().trim());
	}

	private static String resolvePemMaterial(Map<String, Object> cfg, String contentKey, String pathKey) {
		String content = str(cfg.get(contentKey));
		if (looksLikePem(content)) {
			return content;
		}
		String path = str(cfg.get(pathKey));
		if (StringUtils.hasText(path)) {
			return path;
		}
		return content;
	}

	private static boolean looksLikePem(String value) {
		return StringUtils.hasText(value) && value.contains("BEGIN");
	}

	private static SSLContext buildSslContext(String certPemOrPath, String keyPemOrPath) throws Exception {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		Collection<? extends Certificate> certs;
		try (InputStream in = openPemStream(certPemOrPath)) {
			certs = cf.generateCertificates(in);
		}
		if (certs == null || certs.isEmpty()) {
			throw new IllegalArgumentException("empty cert chain");
		}
		PrivateKey pk = readRsaPrivateKey(readPemText(keyPemOrPath));
		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(null, null);
		Certificate[] chain = certs.toArray(Certificate[]::new);
		ks.setKeyEntry("wechat", pk, new char[0], chain);
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, new char[0]);
		SSLContext ssl = SSLContext.getInstance("TLS");
		ssl.init(kmf.getKeyManagers(), null, new SecureRandom());
		return ssl;
	}

	private static InputStream openPemStream(String pemOrPath) throws Exception {
		if (looksLikePem(pemOrPath)) {
			return new ByteArrayInputStream(pemOrPath.getBytes(StandardCharsets.ISO_8859_1));
		}
		return Files.newInputStream(Path.of(pemOrPath));
	}

	private static String readPemText(String pemOrPath) throws Exception {
		if (looksLikePem(pemOrPath)) {
			return pemOrPath;
		}
		return Files.readString(Path.of(pemOrPath), StandardCharsets.UTF_8);
	}

	private static PrivateKey readRsaPrivateKey(String pem) throws Exception {
		if (pem.contains("BEGIN RSA PRIVATE KEY")) {
			String pk =
					pem.replace("-----BEGIN RSA PRIVATE KEY-----", "")
							.replace("-----END RSA PRIVATE KEY-----", "")
							.replaceAll("\\s", "");
			byte[] pkcs1 = Base64.getDecoder().decode(pk);
			byte[] pkcs8 = wrapPkcs1ToPkcs8(pkcs1);
			return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
		}
		String pk =
				pem.replace("-----BEGIN PRIVATE KEY-----", "")
						.replace("-----END PRIVATE KEY-----", "")
						.replaceAll("\\s", "");
		byte[] keyBytes = Base64.getDecoder().decode(pk);
		return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
	}

	/**
	 * Wrap PKCS#1 RSAPrivateKey bytes into a PKCS#8 PrivateKeyInfo structure.
	 */
	private static byte[] wrapPkcs1ToPkcs8(byte[] pkcs1) {
		byte[] algId =
				new byte[] {
					0x30,
					0x0d,
					0x06,
					0x09,
					0x2a,
					(byte) 0x86,
					0x48,
					(byte) 0x86,
					(byte) 0xf7,
					0x0d,
					0x01,
					0x01,
					0x01,
					0x05,
					0x00
				};
		byte[] version = new byte[] {0x02, 0x01, 0x00};
		byte[] octetHeader = derLengthHeader((byte) 0x04, pkcs1.length);
		int innerLen = version.length + algId.length + octetHeader.length + pkcs1.length;
		byte[] seqHeader = derLengthHeader((byte) 0x30, innerLen);
		byte[] out = new byte[seqHeader.length + innerLen];
		int pos = 0;
		System.arraycopy(seqHeader, 0, out, pos, seqHeader.length);
		pos += seqHeader.length;
		System.arraycopy(version, 0, out, pos, version.length);
		pos += version.length;
		System.arraycopy(algId, 0, out, pos, algId.length);
		pos += algId.length;
		System.arraycopy(octetHeader, 0, out, pos, octetHeader.length);
		pos += octetHeader.length;
		System.arraycopy(pkcs1, 0, out, pos, pkcs1.length);
		return out;
	}

	private static byte[] derLengthHeader(byte tag, int length) {
		if (length < 0x80) {
			return new byte[] {tag, (byte) length};
		}
		if (length <= 0xff) {
			return new byte[] {tag, (byte) 0x81, (byte) length};
		}
		if (length <= 0xffff) {
			return new byte[] {tag, (byte) 0x82, (byte) ((length >> 8) & 0xff), (byte) (length & 0xff)};
		}
		return new byte[] {
			tag,
			(byte) 0x83,
			(byte) ((length >> 16) & 0xff),
			(byte) ((length >> 8) & 0xff),
			(byte) (length & 0xff)
		};
	}

	private static Map<String, Object> fail(String msg) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("status", "FAIL");
		m.put("error_desc", msg);
		return m;
	}

	private static String signParams(TreeMap<String, String> sorted, String apiKey) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			if ("sign".equals(e.getKey())) {
				continue;
			}
			if (e.getValue() == null || e.getValue().isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(e.getKey()).append('=').append(e.getValue());
		}
		sb.append("&key=").append(apiKey);
		return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8)).toUpperCase(Locale.ROOT);
	}

	private static String randomNonce() {
		byte[] b = new byte[16];
		new SecureRandom().nextBytes(b);
		StringBuilder sb = new StringBuilder(32);
		for (byte value : b) {
			sb.append(String.format("%02x", value));
		}
		return sb.toString();
	}

	private static String xmlText(String xml, String tag) {
		String open = "<" + tag + ">";
		String close = "</" + tag + ">";
		int a = xml.indexOf(open);
		int b = xml.indexOf(close);
		if (a < 0 || b < 0 || b <= a) {
			return "";
		}
		String raw = xml.substring(a + open.length(), b).trim();
		if (raw.startsWith("<![CDATA[") && raw.endsWith("]]>")) {
			return raw.substring("<![CDATA[".length(), raw.length() - 3).trim();
		}
		return raw;
	}

	private static String buildXml(TreeMap<String, String> params) {
		StringBuilder sb = new StringBuilder();
		sb.append("<xml>");
		for (Map.Entry<String, String> e : params.entrySet()) {
			sb.append('<').append(e.getKey()).append('>');
			sb.append(escapeXml(e.getValue()));
			sb.append("</").append(e.getKey()).append('>');
		}
		sb.append("</xml>");
		return sb.toString();
	}

	private static String escapeXml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
