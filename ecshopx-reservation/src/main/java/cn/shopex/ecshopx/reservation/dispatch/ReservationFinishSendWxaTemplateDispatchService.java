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

package cn.shopex.ecshopx.reservation.dispatch;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.port.reservation.ReservationFinishSuccWxaTemplatePort;
import cn.shopex.ecshopx.common.port.reservation.ReservationUserWxappIdentityResolvePort;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReservationFinishSendWxaTemplateDispatchService {

	private static final Logger log =
			LoggerFactory.getLogger(ReservationFinishSendWxaTemplateDispatchService.class);

	private final ReservationShopDetailPort reservationShopDetailPort;
	private final ReservationFinishSuccWxaTemplatePort reservationFinishSuccWxaTemplatePort;
	private final ReservationUserWxappIdentityResolvePort reservationUserWxappIdentityResolvePort;

	public ReservationFinishSendWxaTemplateDispatchService(
			ReservationShopDetailPort reservationShopDetailPort,
			ReservationFinishSuccWxaTemplatePort reservationFinishSuccWxaTemplatePort,
			ReservationUserWxappIdentityResolvePort reservationUserWxappIdentityResolvePort) {
		this.reservationShopDetailPort = reservationShopDetailPort;
		this.reservationFinishSuccWxaTemplatePort = reservationFinishSuccWxaTemplatePort;
		this.reservationUserWxappIdentityResolvePort = reservationUserWxappIdentityResolvePort;
	}

	public void handle(Map<String, Object> payload) {
		try {
			Object rawEntities = payload.get("entities");
			if (!(rawEntities instanceof Map<?, ?>)) {
				throw new BadRequestException("entities is required");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> entities = (Map<String, Object>) rawEntities;
			Map<String, Object> merged = mergeEntities(entities);
			if (shouldSkip(merged)) {
				return;
			}
			sendIfEligible(merged);
		} catch (BadRequestException ex) {
			throw ex;
		} catch (Exception ex) {
			log.debug("预约成功小程序模板处理出错 {}", ex.getMessage());
		}
	}

	private Map<String, Object> mergeEntities(Map<String, Object> entities) {
		Object postRaw = entities.get("postdata");
		Object resultRaw = entities.get("result");
		Object settingRaw = entities.get("setting_data");
		if (!(postRaw instanceof Map<?, ?>) || !(resultRaw instanceof Map<?, ?>)) {
			throw new BadRequestException("postdata and result are required");
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ((Map<?, ?>) postRaw).entrySet()) {
			merged.put(String.valueOf(e.getKey()), e.getValue());
		}
		for (Map.Entry<?, ?> e : ((Map<?, ?>) resultRaw).entrySet()) {
			merged.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (settingRaw instanceof Map<?, ?> sd) {
			for (Map.Entry<?, ?> e : sd.entrySet()) {
				merged.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return merged;
	}

	private boolean shouldSkip(Map<String, Object> merged) {
		long userId = longFrom(merged, "user_id", "userId");
		long companyId = longFrom(merged, "company_id", "companyId");
		if (userId <= 0L || companyId <= 0L) {
			return true;
		}
		String status = stringVal(firstPresent(merged, "status"));
		return "system".equalsIgnoreCase(status);
	}

	private void sendIfEligible(Map<String, Object> merged) {
		try {
			long companyId = longFrom(merged, "company_id", "companyId");
			long userId = longFrom(merged, "user_id", "userId");
			long shopId = longFrom(merged, "shop_id", "shopId");
			reservationUserWxappIdentityResolvePort.fillIfMissing(companyId, userId, merged);
			String appid = wxappAppIdFrom(merged);
			String openId = openIdFrom(merged);
			if (!StringUtils.hasText(appid) || !StringUtils.hasText(openId)) {
				return;
			}
			Map<String, Object> shopInfo = reservationShopDetailPort.getShopsDetail(shopId, companyId);
			long shopCompanyId = longFrom(shopInfo, "company_id", "companyId");
			if (shopCompanyId <= 0L) {
				shopCompanyId = companyId;
			}

			Map<String, Object> wxopen = new LinkedHashMap<>(8);
			wxopen.put("scenes_name", "reservationSucc");
			wxopen.put("company_id", shopCompanyId);
			wxopen.put("appid", appid);
			wxopen.put("openid", openId);

			Map<String, Object> data = new LinkedHashMap<>(8);
			data.put("name", pickDisplayName(merged));
			data.put("date", buildArrivalTimeLabel(merged));
			data.put("shop_name", stringVal(firstPresent(shopInfo, "shop_name", "store_name")));
			data.put("shop_address", stringVal(firstPresent(shopInfo, "shop_address", "address")));
			data.put("remarks", buildRemarks(shopInfo));
			wxopen.put("data", data);

			reservationFinishSuccWxaTemplatePort.send(wxopen);
		} catch (Exception ex) {
			log.debug("预约成功小程序模板发送出错 {}", ex.getMessage());
		}
	}

	private static String pickDisplayName(Map<String, Object> merged) {
		String rights = stringVal(firstPresent(merged, "rights_name", "rightsName"));
		if (StringUtils.hasText(rights)) {
			return rights;
		}
		return stringVal(firstPresent(merged, "user_name", "userName"));
	}

	private static String buildArrivalTimeLabel(Map<String, Object> merged) {
		String day = stringVal(firstPresent(merged, "date_day", "dateDay")).trim();
		String begin = stringVal(firstPresent(merged, "begin_time", "beginTime")).trim();
		if (StringUtils.hasText(day) && StringUtils.hasText(begin)) {
			return day + " " + begin;
		}
		if (StringUtils.hasText(day)) {
			return day;
		}
		return begin;
	}

	private static String buildRemarks(Map<String, Object> shopInfo) {
		String phone = stringVal(firstPresent(shopInfo, "contract_phone", "contractPhone")).trim();
		if (StringUtils.hasText(phone)) {
			return "门店电话：" + phone;
		}
		return "欢迎按时到店";
	}

	private static String wxappAppIdFrom(Map<String, Object> merged) {
		String v = stringVal(firstPresent(merged, "wxapp_appid", "wxappAppid", "authorizer_appid", "authorizerAppid"))
				.trim();
		return v;
	}

	private static String openIdFrom(Map<String, Object> merged) {
		return stringVal(firstPresent(merged, "open_id", "openId", "openid")).trim();
	}

	private static Object firstPresent(Map<String, Object> map, String... keys) {
		for (String k : keys) {
			if (map.containsKey(k)) {
				return map.get(k);
			}
		}
		return null;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long longFrom(Map<String, Object> map, String snake, String camel) {
		Object v = firstPresent(map, snake, camel);
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(Objects.toString(v, "0").trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
