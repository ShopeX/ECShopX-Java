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

package cn.shopex.ecshopx.wechat.api.open;

import cn.shopex.ecshopx.common.dispatch.WechatSubscribeDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WxShopsAddDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WxShopsUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.mapper.WechatAuthMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechatAuth/callback")
public class WechatOpenPlatformMessageCallbackController {

	private static final Logger log = LoggerFactory.getLogger(WechatOpenPlatformMessageCallbackController.class);

	private final WechatSubscribeDispatchPublisher wechatSubscribeDispatchPublisher;
	private final WxShopsAddDispatchPublisher wxShopsAddDispatchPublisher;
	private final WxShopsUpdateDispatchPublisher wxShopsUpdateDispatchPublisher;
	private final WechatAuthMapper wechatAuthMapper;
	private final String callbackVerifyToken;

	public WechatOpenPlatformMessageCallbackController(
			WechatSubscribeDispatchPublisher wechatSubscribeDispatchPublisher,
			WxShopsAddDispatchPublisher wxShopsAddDispatchPublisher,
			WxShopsUpdateDispatchPublisher wxShopsUpdateDispatchPublisher,
			WechatAuthMapper wechatAuthMapper,
			@Value("${ecshopx.wechat.open-platform-message.callback-token:}") String callbackVerifyToken) {
		this.wechatSubscribeDispatchPublisher = wechatSubscribeDispatchPublisher;
		this.wxShopsAddDispatchPublisher = wxShopsAddDispatchPublisher;
		this.wxShopsUpdateDispatchPublisher = wxShopsUpdateDispatchPublisher;
		this.wechatAuthMapper = wechatAuthMapper;
		this.callbackVerifyToken = callbackVerifyToken == null ? "" : callbackVerifyToken.trim();
	}

	@GetMapping("/{authorizerAppId}")
	public ResponseEntity<String> verifyCallbackUrl(
			@PathVariable String authorizerAppId,
			@RequestParam("signature") String signature,
			@RequestParam("timestamp") String timestamp,
			@RequestParam("nonce") String nonce,
			@RequestParam("echostr") String echostr) {
		if (!callbackVerifyToken.isEmpty()
				&& !sha1SignatureMatches(callbackVerifyToken, timestamp, nonce, echostr, signature)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("");
		}
		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(echostr);
	}

	@PostMapping(
			value = "/{authorizerAppId}",
			consumes = {MediaType.APPLICATION_XML_VALUE, "text/xml", MediaType.TEXT_PLAIN_VALUE},
			produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> handleMessage(
			@PathVariable String authorizerAppId,
			@RequestParam(value = "signature", required = false) String signature,
			@RequestParam(value = "msg_signature", required = false) String msgSignature,
			@RequestParam(value = "timestamp", required = false) String timestamp,
			@RequestParam(value = "nonce", required = false) String nonce,
			@RequestBody String body,
			HttpServletRequest request) {
		String appId = authorizerAppId == null ? "" : authorizerAppId.trim();
		WechatAuth auth = wechatAuthMapper.selectById(appId);
		if (auth == null || auth.getCompanyId() == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("fail");
		}

		if (!callbackVerifyToken.isEmpty()
				&& !postCallbackSignatureMatches(body, signature, msgSignature, timestamp, nonce)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("");
		}

		Optional<WechatOpenPlatformCallbackXmlSupport.ParsedEventXml> parsed =
				WechatOpenPlatformCallbackXmlSupport.tryParsePlaintextEventXml(body);
		if (parsed.isEmpty()) {
			log.debug("wechat callback: unsupported or encrypted payload, uri={}", request.getRequestURI());
			return ResponseEntity.ok("success");
		}
		WechatOpenPlatformCallbackXmlSupport.ParsedEventXml p = parsed.get();
		if (!"event".equalsIgnoreCase(p.msgType())) {
			return ResponseEntity.ok("success");
		}
		String ev = p.event();
		if ("modify_store_audit_info".equalsIgnoreCase(ev)) {
			Map<String, Object> wxPayload = new LinkedHashMap<>();
			wxPayload.put("audit_id", p.auditId());
			wxPayload.put("status", p.status().isEmpty() ? null : p.status());
			wxPayload.put("reason", p.reason());
			wxShopsUpdateDispatchPublisher.publish(wxPayload);
			return ResponseEntity.ok("success");
		}
		if ("add_store_audit_info".equalsIgnoreCase(ev)) {
			Map<String, Object> wxPayload = new LinkedHashMap<>();
			wxPayload.put("audit_id", p.auditId());
			wxPayload.put("status", p.status().isEmpty() ? null : p.status());
			wxPayload.put("reason", p.reason());
			wxPayload.put("is_upgrade", p.isUpgrade().isEmpty() ? null : p.isUpgrade());
			wxPayload.put("poiid", p.poiid());
			wxShopsAddDispatchPublisher.publish(wxPayload);
			return ResponseEntity.ok("success");
		}
		if (!"subscribe".equalsIgnoreCase(ev) && !"unsubscribe".equalsIgnoreCase(ev)) {
			return ResponseEntity.ok("success");
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("openId", p.fromUserName());
		payload.put("authorizerAppId", appId);
		payload.put("company_id", auth.getCompanyId());
		payload.put("event", ev.toLowerCase());
		wechatSubscribeDispatchPublisher.publish(payload);
		return ResponseEntity.ok("success");
	}

	/**
	 * Plaintext push: {@code signature} = SHA1(sort(token, timestamp, nonce)).
	 * Encrypted push (openid platform): {@code msg_signature} = SHA1(sort(token, timestamp, nonce,
	 * encrypt)).
	 */
	private boolean postCallbackSignatureMatches(
			String body,
			String signature,
			String msgSignature,
			String timestamp,
			String nonce) {
		String ts = timestamp == null ? "" : timestamp.trim();
		String n = nonce == null ? "" : nonce.trim();
		String encrypt = extractEncryptPayload(body);
		if (!encrypt.isEmpty()) {
			String ms = msgSignature == null ? "" : msgSignature.trim();
			if (ms.isEmpty()) {
				return false;
			}
			return sha1SignatureMatchesFour(callbackVerifyToken, ts, n, encrypt, ms);
		}
		String sig = signature == null ? "" : signature.trim();
		if (sig.isEmpty()) {
			return false;
		}
		return sha1SignatureMatchesThree(callbackVerifyToken, ts, n, sig);
	}

	private static String extractEncryptPayload(String rawBody) {
		if (rawBody == null || rawBody.isBlank()) {
			return "";
		}
		int open = rawBody.indexOf("<Encrypt>");
		if (open < 0) {
			return "";
		}
		int close = rawBody.indexOf("</Encrypt>", open);
		if (close < 0) {
			return "";
		}
		String inner = rawBody.substring(open + "<Encrypt>".length(), close).trim();
		if (inner.startsWith("<![CDATA[")) {
			int end = inner.indexOf("]]>");
			if (end > "<![CDATA[".length()) {
				return inner.substring("<![CDATA[".length(), end).trim();
			}
		}
		return inner;
	}

	private static boolean sha1SignatureMatches(
			String token, String timestamp, String nonce, String echostr, String signature) {
		String[] arr = new String[] {token, timestamp, nonce, echostr};
		Arrays.sort(arr);
		String plain = arr[0] + arr[1] + arr[2] + arr[3];
		String computed = sha1HexLower(plain);
		return computed.equalsIgnoreCase(signature == null ? "" : signature.trim());
	}

	private static boolean sha1SignatureMatchesThree(
			String token, String timestamp, String nonce, String signature) {
		String[] arr = new String[] {token, timestamp, nonce};
		Arrays.sort(arr);
		String plain = arr[0] + arr[1] + arr[2];
		String computed = sha1HexLower(plain);
		return computed.equalsIgnoreCase(signature == null ? "" : signature.trim());
	}

	private static boolean sha1SignatureMatchesFour(
			String token, String timestamp, String nonce, String encrypt, String msgSignature) {
		String[] arr = new String[] {token, timestamp, nonce, encrypt};
		Arrays.sort(arr);
		String plain = arr[0] + arr[1] + arr[2] + arr[3];
		String computed = sha1HexLower(plain);
		return computed.equalsIgnoreCase(msgSignature == null ? "" : msgSignature.trim());
	}

	private static String sha1HexLower(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new ResourceException("SHA-1 algorithm not available");
		}
	}
}
