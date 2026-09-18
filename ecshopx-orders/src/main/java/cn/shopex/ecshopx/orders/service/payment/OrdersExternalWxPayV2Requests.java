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

package cn.shopex.ecshopx.orders.service.payment;

import com.github.binarywang.wxpay.bean.request.WxPayMicropayRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

/**
 * Builds v2 {@link WxPayUnifiedOrderRequest} / {@link WxPayMicropayRequest} payloads aligned with the legacy PHP
 * EasyWeChat field set (detail, time_expire, JSAPI {@code scene_info} = {@code []}, no MWEB scene_info, servicer sub_*).
 */
final class OrdersExternalWxPayV2Requests {

	static final DateTimeFormatter WX_TIME_EXPIRE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	private static final long FALLBACK_EXPIRE_SECONDS = 300L;

	private OrdersExternalWxPayV2Requests() {}

	/**
	 * WeChat v2 {@code time_expire}: prefer this order's {@code auto_cancel_time} (unix seconds), otherwise
	 * {@code now + 5 minutes}.
	 */
	static String formatTimeExpire(Object autoCancelTime) {
		Long epoch = parseEpochSeconds(autoCancelTime);
		ZonedDateTime when =
				epoch != null
						? Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault())
						: ZonedDateTime.now(ZoneId.systemDefault()).plusSeconds(FALLBACK_EXPIRE_SECONDS);
		return when.format(WX_TIME_EXPIRE_FORMATTER);
	}

	private static Long parseEpochSeconds(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s) || "null".equalsIgnoreCase(s)) {
			return null;
		}
		if (!s.chars().allMatch(Character::isDigit)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	static WxPayUnifiedOrderRequest unifiedOrderRequest(
			boolean servicer,
			String servicerAppId,
			String servicerMchId,
			String merchantId,
			String miniAppIdForUnified,
			String wxaAppId,
			String woaAppId,
			String cfgAppId,
			boolean h5OrJsPay,
			String tradeType,
			String openId,
			String body,
			String detailFromData,
			String tradeId,
			int payFeeFen,
			String ip,
			String notifyUrl,
			String attachEncodedOuter,
			String timeExpireFormatted,
			String nonceStr) {
		WxPayUnifiedOrderRequest unify = new WxPayUnifiedOrderRequest();
		if (servicer) {
			unify.setAppid(servicerAppId);
			unify.setMchId(servicerMchId);
			unify.setSubAppId(miniAppIdForUnified);
			unify.setSubMchId(merchantId);
			if ("JSAPI".equals(tradeType)) {
				unify.setSubOpenid(openId);
			}
		} else {
			String appIdForUnified = "APP".equals(tradeType) ? woaAppId : wxaAppId;
			if (!StringUtils.hasText(appIdForUnified) && "JSAPI".equals(tradeType)) {
				appIdForUnified = wxaAppId;
			}
			if (!"APP".equals(tradeType) && h5OrJsPay && StringUtils.hasText(cfgAppId)) {
				appIdForUnified = cfgAppId;
			}
			unify.setAppid(appIdForUnified);
			unify.setMchId(merchantId);
			if ("JSAPI".equals(tradeType)) {
				unify.setOpenid(openId);
			}
		}
		unify.setNonceStr(nonceStr);
		unify.setBody(body);
		String detail = detailFromData;
		if (!StringUtils.hasText(detail)) {
			detail = body;
		}
		unify.setDetail(detail);
		unify.setTimeExpire(timeExpireFormatted);
		unify.setOutTradeNo(tradeId);
		unify.setTotalFee(payFeeFen);
		unify.setSpbillCreateIp(ip);
		unify.setNotifyUrl(notifyUrl);
		unify.setTradeType(tradeType);
		unify.setAttach(attachEncodedOuter);
		if ("JSAPI".equals(tradeType)) {
			unify.setSceneInfo("[]");
		}
		return unify;
	}

	static WxPayMicropayRequest micropayRequest(
			boolean servicer,
			String servicerAppId,
			String servicerMchId,
			String merchantId,
			String wxaAppId,
			String woaAppId,
			String body,
			String tradeId,
			int payFeeFen,
			String ip,
			String authCode,
			String nonceStr) {
		WxPayMicropayRequest mic = new WxPayMicropayRequest();
		if (servicer) {
			mic.setAppid(servicerAppId);
			mic.setMchId(servicerMchId);
			mic.setSubAppId(wxaAppId);
			mic.setSubMchId(merchantId);
		} else {
			mic.setAppid(StringUtils.hasText(wxaAppId) ? wxaAppId : woaAppId);
			mic.setMchId(merchantId);
		}
		mic.setNonceStr(nonceStr);
		mic.setBody(body);
		mic.setOutTradeNo(tradeId);
		mic.setTotalFee(payFeeFen);
		mic.setSpbillCreateIp(ip);
		mic.setAuthCode(authCode);
		return mic;
	}
}
