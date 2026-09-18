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

package cn.shopex.ecshopx.members.client.wx;

import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.members.config.H5WxProperties;
import cn.shopex.ecshopx.members.mapper.WechatAuthorizerLookupMapper;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WxOpenPlatformClient {

	private static final String DECRYPT_PHONE_UNBOUND_MESSAGE = "当前公众号或小程序未绑定或已解绑，请重新授权";

	private enum Code2SessionFailurePolicy {
		LOGIN_DEFAULT,
		DECRYPT_PHONE_PARITY
	}

	private final WxJavaMaRuntime wxJavaMaRuntime;
	private final WxJavaMpRuntime wxJavaMpRuntime;

	private final WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper;

	private final H5WxProperties h5WxProperties;

	public WxOpenPlatformClient(
			WxJavaMaRuntime wxJavaMaRuntime,
			WxJavaMpRuntime wxJavaMpRuntime,
			WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper,
			H5WxProperties h5WxProperties) {
		this.wxJavaMaRuntime = wxJavaMaRuntime;
		this.wxJavaMpRuntime = wxJavaMpRuntime;
		this.wechatAuthorizerLookupMapper = wechatAuthorizerLookupMapper;
		this.h5WxProperties = h5WxProperties;
	}

	public Long getCompanyIdByAuthorizerAppid(String appid) {
		if (!StringUtils.hasText(appid)) {
			return null;
		}
		return wechatAuthorizerLookupMapper.selectCompanyIdByAuthorizerAppid(appid);
	}

	/**
	 * 小程序 {@code appid} 在开放平台授权表或本地 YAML 中至少有一处可识别时通过；否则视为非法 {@code appid}，与前台登录链路的鉴权语义一致。
	 */
	public void assertWxMiniProgramAppIdRecognized(String appid) {
		if (!StringUtils.hasText(appid)) {
			return;
		}
		String yamlSecret = h5WxProperties.getMiniProgramSecretByAppid().get(appid);
		if (StringUtils.hasText(yamlSecret)) {
			return;
		}
		if (wechatAuthorizerLookupMapper.selectCompanyIdByAuthorizerAppid(appid) != null) {
			return;
		}
		throw new UnauthorizedException("401 Unauthorized");
	}

	/**
	 * 微信小程序手机号密文解密，返回 JSON 字符串（含 {@code purePhoneNumber} 等字段）。
	 */
	public String decryptWxMiniProgramPhoneNumber(String sessionKeyBase64, String ivBase64, String encryptedDataBase64) {
		if (!StringUtils.hasText(sessionKeyBase64) || !StringUtils.hasText(ivBase64) || !StringUtils.hasText(encryptedDataBase64)) {
			throw new ResourceException("授权手机号失败");
		}
		try {
			return decryptWxMiniProgramPhonePayloadRaw(sessionKeyBase64, ivBase64, encryptedDataBase64);
		} catch (Exception e) {
			throw new ResourceException("授权手机号失败");
		}
	}

	/**
	 * 与 {@link #decryptWxMiniProgramPhoneNumber} 相同的解密算法；失败时抛出 {@link BadRequestException}，供无需预登录会话的开放接口使用。
	 */
	public String decryptWxMiniProgramPhoneNumberForBadRequest(
			String sessionKeyBase64, String ivBase64, String encryptedDataBase64) {
		if (!StringUtils.hasText(sessionKeyBase64) || !StringUtils.hasText(ivBase64) || !StringUtils.hasText(encryptedDataBase64)) {
			throw new BadRequestException("授权手机号失败");
		}
		try {
			return decryptWxMiniProgramPhonePayloadRaw(sessionKeyBase64, ivBase64, encryptedDataBase64);
		} catch (Exception e) {
			throw new BadRequestException("授权手机号失败");
		}
	}

	private String decryptWxMiniProgramPhonePayloadRaw(
			String sessionKeyBase64, String ivBase64, String encryptedDataBase64) throws Exception {
		byte[] sessionKey = Base64.getDecoder().decode(sessionKeyBase64);
		byte[] iv = Base64.getDecoder().decode(ivBase64);
		byte[] encrypted = Base64.getDecoder().decode(encryptedDataBase64);
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sessionKey, "AES"), new IvParameterSpec(iv));
		byte[] decrypted = cipher.doFinal(encrypted);
		return new String(decrypted, StandardCharsets.UTF_8);
	}

	/**
	 * 小程序 code2session，返回 openid、unionid、session_key 等。
	 */
	public Map<String, String> miniProgramCode2Session(String appid, String code) {
		return miniProgramCode2SessionInternal(appid, code, Code2SessionFailurePolicy.LOGIN_DEFAULT);
	}

	/**
	 * 仅 {@link cn.shopex.ecshopx.members.service.h5.MemberNoAuthDecryptPhoneService} 使用：无效 code、微信失败等与未绑定同一 400001 信封。
	 */
	public Map<String, String> miniProgramCode2SessionForDecryptPhone(String appid, String code) {
		return miniProgramCode2SessionInternal(appid, code, Code2SessionFailurePolicy.DECRYPT_PHONE_PARITY);
	}

	private Map<String, String> miniProgramCode2SessionInternal(
			String appid, String code, Code2SessionFailurePolicy policy) {
		if (policy == Code2SessionFailurePolicy.LOGIN_DEFAULT) {
			if (!StringUtils.hasText(appid) || !StringUtils.hasText(code)) {
				throw new ResourceException("缺少小程序ID");
			}
		} else {
			if (!StringUtils.hasText(appid)) {
				throw new ResourceException("缺少小程序ID");
			}
		}
		String secret = resolveMiniProgramSecret(appid);
		if (policy == Code2SessionFailurePolicy.DECRYPT_PHONE_PARITY && !StringUtils.hasText(code)) {
			throw new ResourceException(DECRYPT_PHONE_UNBOUND_MESSAGE, 400, 400001);
		}
		try {
			WxMaJscode2SessionResult session =
					wxJavaMaRuntime.maDirect(appid.trim(), secret.trim()).jsCode2SessionInfo(code.trim());
			Map<String, String> m = new HashMap<>();
			if (StringUtils.hasText(session.getOpenid())) {
				m.put("openid", session.getOpenid());
			}
			if (StringUtils.hasText(session.getUnionid())) {
				m.put("unionid", session.getUnionid());
			}
			if (StringUtils.hasText(session.getSessionKey())) {
				m.put("session_key", session.getSessionKey());
			}
			String sessionKey = m.get("session_key");
			if (!StringUtils.hasText(sessionKey)) {
				throw code2SessionTransportOrWechatFailure(policy);
			}
			String openid = m.get("openid");
			if (!StringUtils.hasText(openid)) {
				if (policy == Code2SessionFailurePolicy.LOGIN_DEFAULT) {
					throw new ResourceException("小程序授权错误，请联系供应商！");
				}
				throw new ResourceException(DECRYPT_PHONE_UNBOUND_MESSAGE, 400, 400001);
			}
			return m;
		} catch (WxErrorException e) {
			throw code2SessionTransportOrWechatFailure(policy);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw code2SessionTransportOrWechatFailure(policy);
		}
	}

	private static ResourceException code2SessionTransportOrWechatFailure(Code2SessionFailurePolicy policy) {
		if (policy == Code2SessionFailurePolicy.LOGIN_DEFAULT) {
			return new ResourceException("用户登录失败！");
		}
		return new ResourceException(DECRYPT_PHONE_UNBOUND_MESSAGE, 400, 400001);
	}

	/**
	 * 服务号/网站应用 OAuth2：通过 code 换 access_token 与 openid。
	 */
	public Map<String, String> oauth2SnsAccessToken(String appid, String secret, String code) {
		if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret) || !StringUtils.hasText(code)) {
			throw new ResourceException("code error");
		}
		try {
			WxOAuth2AccessToken tok =
					wxJavaMpRuntime
							.mpDirect(appid.trim(), secret.trim())
							.getOAuth2Service()
							.getAccessToken(code.trim());
			Map<String, String> m = new HashMap<>();
			putIfText(m, "access_token", tok.getAccessToken());
			putIfText(m, "openid", tok.getOpenId());
			putIfText(m, "unionid", tok.getUnionId());
			putIfText(m, "scope", tok.getScope());
			return m;
		} catch (WxErrorException e) {
			throw new ResourceException("公众号信息有误！");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("公众号信息有误！");
		}
	}

	public Map<String, String> snsUserinfo(String accessToken, String openid) {
		try {
			WxOAuth2AccessToken tok = new WxOAuth2AccessToken();
			tok.setAccessToken(accessToken);
			tok.setOpenId(openid);
			WxOAuth2UserInfo info =
					wxJavaMpRuntime.mpBearer(accessToken).getOAuth2Service().getUserInfo(tok, "zh_CN");
			Map<String, String> m = new HashMap<>();
			putIfText(m, "openid", info.getOpenid());
			putIfText(m, "unionid", info.getUnionId());
			putIfText(m, "nickname", info.getNickname());
			if (info.getSex() != null) {
				m.put("sex", String.valueOf(info.getSex()));
			}
			putIfText(m, "headimgurl", info.getHeadImgUrl());
			return m;
		} catch (WxErrorException e) {
			throw new ResourceException("公众号信息有误！");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("公众号信息有误！");
		}
	}

	private static void putIfText(Map<String, String> m, String field, String value) {
		if (StringUtils.hasText(value)) {
			m.put(field, value);
		}
	}

	private String resolveMiniProgramSecret(String appid) {
		String fromDb = wechatAuthorizerLookupMapper.selectAuthorizerSecretByAppid(appid);
		if (StringUtils.hasText(fromDb)) {
			return fromDb;
		}
		String fromYaml = h5WxProperties.getMiniProgramSecretByAppid().get(appid);
		if (StringUtils.hasText(fromYaml)) {
			return fromYaml;
		}
		Long boundCompanyId = wechatAuthorizerLookupMapper.selectCompanyIdByAuthorizerAppid(appid);
		if (boundCompanyId != null) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}
		throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
	}
}
