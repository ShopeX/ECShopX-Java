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

import cn.shopex.ecshopx.common.dispatch.ReservationDispatchJobNames;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.port.reservation.ReservationFinishSuccWxaTemplatePort;
import cn.shopex.ecshopx.common.port.reservation.ReservationUserWxappIdentityResolvePort;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import cn.shopex.ecshopx.reservation.support.ReservationSmsQueueDelayFormatter;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReservationRemindSendWxaTemplateDispatchService {

	private static final Logger log =
			LoggerFactory.getLogger(ReservationRemindSendWxaTemplateDispatchService.class);

	private final DispatchFacade dispatchFacade;
	private final Clock clock;
	private final ReservationShopDetailPort reservationShopDetailPort;
	private final ReservationFinishSuccWxaTemplatePort reservationFinishSuccWxaTemplatePort;
	private final ReservationUserWxappIdentityResolvePort reservationUserWxappIdentityResolvePort;
	private final ReservationRecordMapper reservationRecordMapper;

	public ReservationRemindSendWxaTemplateDispatchService(
			DispatchFacade dispatchFacade,
			@Autowired(required = false) Clock clock,
			ReservationShopDetailPort reservationShopDetailPort,
			ReservationFinishSuccWxaTemplatePort reservationFinishSuccWxaTemplatePort,
			ReservationUserWxappIdentityResolvePort reservationUserWxappIdentityResolvePort,
			ReservationRecordMapper reservationRecordMapper) {
		this.dispatchFacade = dispatchFacade;
		this.clock = clock != null ? clock : Clock.systemUTC();
		this.reservationShopDetailPort = reservationShopDetailPort;
		this.reservationFinishSuccWxaTemplatePort = reservationFinishSuccWxaTemplatePort;
		this.reservationUserWxappIdentityResolvePort = reservationUserWxappIdentityResolvePort;
		this.reservationRecordMapper = reservationRecordMapper;
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
			if (shouldEarlyReturn(merged)) {
				return;
			}
			long delaySeconds = computeRemindDelaySeconds(merged);
			if (delaySeconds > 0) {
				maybeDispatchDeferJob(payload, delaySeconds);
				return;
			}
			sendIfEligible(merged);
		} catch (BadRequestException ex) {
			throw ex;
		} catch (Exception ex) {
			log.debug("预约提醒小程序模板处理出错 {}", ex.getMessage());
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

	private boolean shouldEarlyReturn(Map<String, Object> merged) {
		long userId = longFrom(merged, "user_id", "userId");
		long companyId = longFrom(merged, "company_id", "companyId");
		return userId <= 0L || companyId <= 0L;
	}

	private long computeRemindDelaySeconds(Map<String, Object> merged) {
		try {
			long toShopEpoch = resolveToShopEpochSeconds(merged);
			int smsHours = smsDelayHours(merged);
			long endRemindEpoch = toShopEpoch - smsHours * 3600L;
			long now = clock.instant().getEpochSecond();
			long raw = endRemindEpoch - now;
			return ReservationSmsQueueDelayFormatter.formatQueueDelaySeconds(raw);
		} catch (Exception ex) {
			log.debug("预约提醒延迟计算跳过 {}", ex.getMessage());
			return 0L;
		}
	}

	private int smsDelayHours(Map<String, Object> merged) {
		Object v = firstPresent(merged, "smsDelay");
		if (v == null) {
			return 1;
		}
		int h = (int) longFromNumberOrString(v);
		return h > 0 ? h : 1;
	}

	private long resolveToShopEpochSeconds(Map<String, Object> merged) {
		Object ts = firstPresent(merged, "to_shop_time", "toShopTime");
		if (ts instanceof Number n) {
			return n.longValue();
		}
		String day = stringVal(firstPresent(merged, "date_day", "dateDay")).trim();
		String begin = stringVal(firstPresent(merged, "begin_time", "beginTime")).trim();
		LocalDate d = LocalDate.parse(day);
		LocalTime t = parseTimeFlexible(begin);
		return d.atTime(t).atZone(ZoneId.systemDefault()).toEpochSecond();
	}

	private static LocalTime parseTimeFlexible(String s) {
		String[] patterns = {"H:mm", "HH:mm", "H:m", "HH:m"};
		for (String p : patterns) {
			try {
				return LocalTime.parse(s, DateTimeFormatter.ofPattern(p));
			} catch (Exception ignored) {
				// try next
			}
		}
		throw new IllegalArgumentException("begin_time");
	}

	private void maybeDispatchDeferJob(Map<String, Object> payload, long delaySeconds) {
		if (delaySeconds <= 0L) {
			return;
		}
		dispatchFacade.dispatchJob(
				ReservationDispatchJobNames.JOB_RESERVATION_REMIND_SEND_WXA_TEMPLATE_DEFER,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						Duration.ofSeconds(delaySeconds),
						RetryPolicy.platformDefault()));
	}

	private void sendIfEligible(Map<String, Object> merged) {
		try {
			long recordId = longFrom(merged, "record_id", "recordId");
			if (recordId <= 0L) {
				return;
			}
			ReservationRecord record = reservationRecordMapper.selectById(recordId);
			if (record == null || !"success".equalsIgnoreCase(record.getStatus())) {
				return;
			}
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
			wxopen.put("scenes_name", "reservationRemind");
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
			log.debug("预约提醒小程序模板发送出错 {}", ex.getMessage());
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
		return stringVal(
						firstPresent(merged, "wxapp_appid", "wxappAppid", "authorizer_appid", "authorizerAppid"))
				.trim();
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

	private static long longFromNumberOrString(Object v) {
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
