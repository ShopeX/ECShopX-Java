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

package cn.shopex.ecshopx.members.service.h5.prelogin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.client.ali.AliMiniProgramOAuthClient;
import cn.shopex.ecshopx.members.config.H5AliProperties;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class H5AliappPreLoginService {

	private final AliMiniProgramOAuthClient aliMiniProgramOAuthClient;

	private final H5AliProperties h5AliProperties;

	private final MemberAccountService memberAccountService;

	private final H5WxappLoginRegisterFacade h5WxappLoginRegisterFacade;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public H5AliappPreLoginService(
			AliMiniProgramOAuthClient aliMiniProgramOAuthClient,
			H5AliProperties h5AliProperties,
			MemberAccountService memberAccountService,
			H5WxappLoginRegisterFacade h5WxappLoginRegisterFacade) {
		this.aliMiniProgramOAuthClient = aliMiniProgramOAuthClient;
		this.h5AliProperties = h5AliProperties;
		this.memberAccountService = memberAccountService;
		this.h5WxappLoginRegisterFacade = h5WxappLoginRegisterFacade;
	}

	public Map<String, Object> aliappPreLogin(Map<String, Object> params) {
		String code = trim(String.valueOf(params.get("code")));
		if (!StringUtils.hasText(code)) {
			throw new ResourceException("缺少参数，登录失败！");
		}
		String encryptedData = trim(String.valueOf(params.get("encryptedData")));
		if (!StringUtils.hasText(encryptedData)) {
			throw new ResourceException("缺少参数，登录失败！");
		}

		long companyId = toLong(params.get("company_id"));
		String alipayUserId = aliMiniProgramOAuthClient.exchangeCodeForUserId(companyId, code);
		String mobile = decryptAliEncryptedPhoneNumber(companyId, encryptedData);

		params.put("open_id", alipayUserId);
		params.put("unionid", alipayUserId);
		params.put("mobile", mobile);
		params.put("user_type", "ali");
		String appIdCfg = h5AliProperties.getMiniAppIdByCompanyId().get(String.valueOf(companyId));
		if (StringUtils.hasText(appIdCfg)) {
			params.put("alipay_appid", appIdCfg);
		} else {
			params.put("alipay_appid", trim(String.valueOf(params.get("appid"))));
		}

		Map<String, Object> member = memberAccountService.getInfoByMobile(companyId, mobile);
		Map<String, Object> aliSession = new LinkedHashMap<>();
		aliSession.put("alipay_user_id", alipayUserId);
		aliSession.put("purePhoneNumber", mobile);

		if (member == null || member.isEmpty()) {
			h5WxappLoginRegisterFacade.registerMemberForAliapp(params, aliSession);
			member = memberAccountService.getInfoByMobile(companyId, mobile);
		} else {
			long userId = toLong(member.get("user_id"));
			Map<String, Object> assoc = memberAccountService.getMembersAssociationByUserId(companyId, "ali", userId);
			boolean linked = assoc != null
					&& !assoc.isEmpty()
					&& alipayUserId.equals(Objects.toString(assoc.get("unionid"), ""));
			if (!linked) {
				h5WxappLoginRegisterFacade.registerMemberForAliapp(params, aliSession);
				member = memberAccountService.getInfoByMobile(companyId, mobile);
			}
		}

		long finalUserId = toLong(member.get("user_id"));
		h5WxappLoginRegisterFacade.bindSalespersonIfNeeded(params, finalUserId);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("user_id", member.get("user_id"));
		out.put("alipay_user_id", alipayUserId);
		return out;
	}

	private String decryptAliEncryptedPhoneNumber(long companyId, String encryptedPayload) {
		String aesKeyB64 = h5AliProperties.getAesDecryptKeyByCompanyId().get(String.valueOf(companyId));
		if (!StringUtils.hasText(aesKeyB64)) {
			throw new ResourceException("授权手机号失败");
		}
		String trimmed = encryptedPayload.trim();
		try {
			String charset = "UTF-8";
			String signType = "RSA2";
			String encryptType = "AES";
			String content;
			String sign = null;
			if (trimmed.startsWith("{")) {
				JsonNode root = objectMapper.readTree(trimmed);
				content = root.path("response").asText("");
				if (!StringUtils.hasText(content)) {
					throw new ResourceException("授权手机号失败");
				}
				if (root.hasNonNull("sign")) {
					sign = root.get("sign").asText();
				}
				if (root.hasNonNull("charset")) {
					charset = root.get("charset").asText(charset);
				}
				if (root.hasNonNull("sign_type")) {
					signType = root.get("sign_type").asText(signType);
				}
				if (root.hasNonNull("encrypt_type")) {
					encryptType = root.get("encrypt_type").asText(encryptType);
				}
			} else {
				content = trimmed;
			}

			boolean isDataEncrypted = !content.startsWith("{");
			String pub = h5AliProperties.getAlipayRsaPublicKeyByCompanyId().get(String.valueOf(companyId));
			if (isDataEncrypted && StringUtils.hasText(sign) && StringUtils.hasText(pub)) {
				verifyAlipayResponseSignature(content, sign, signType, charset, pub);
			}

			String plainJson;
			if (isDataEncrypted) {
				if (!"AES".equalsIgnoreCase(encryptType)) {
					throw new ResourceException("授权手机号失败");
				}
				byte[] keyBytes = Base64.getDecoder().decode(aesKeyB64.trim());
				if (keyBytes.length != 16) {
					throw new ResourceException("授权手机号失败");
				}
				SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
				Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
				byte[] iv = new byte[16];
				cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
				byte[] cipherBytes = Base64.getDecoder().decode(content.trim());
				byte[] decrypted = cipher.doFinal(cipherBytes);
				plainJson = new String(decrypted, charset);
			} else {
				plainJson = content;
			}

			JsonNode data = objectMapper.readTree(plainJson);
			JsonNode mobileNode = data.get("mobile");
			if (mobileNode == null || mobileNode.isNull() || !StringUtils.hasText(mobileNode.asText())) {
				throw new ResourceException("授权手机号失败");
			}
			return mobileNode.asText().trim();
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("授权手机号失败");
		}
	}

	private static void verifyAlipayResponseSignature(
			String content,
			String sign,
			String signType,
			String charset,
			String publicKeyPemOrB64) throws Exception {
		PublicKey publicKey = parseRsaPublicKey(publicKeyPemOrB64);
		String signContent = content.startsWith("{") ? content : "\"" + content + "\"";
		Signature signature = Signature.getInstance("SHA256withRSA".equalsIgnoreCase(signType) ? "SHA256withRSA" : "SHA1withRSA");
		signature.initVerify(publicKey);
		signature.update(signContent.getBytes(Charset.forName(charset)));
		byte[] signBytes = Base64.getDecoder().decode(sign.trim());
		if (!signature.verify(signBytes)) {
			throw new ResourceException("授权手机号失败");
		}
	}

	private static PublicKey parseRsaPublicKey(String pemOrB64) throws Exception {
		String k = pemOrB64
				.replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "")
				.replaceAll("\\s+", "");
		byte[] der = Base64.getDecoder().decode(k);
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}
}
