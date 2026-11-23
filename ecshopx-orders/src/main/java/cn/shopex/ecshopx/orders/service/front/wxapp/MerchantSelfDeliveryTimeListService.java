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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * PHP {@code OrderService::getDeliveryTimeList} 对齐：商家自配送结算可选时段。
 */
@Service
public class MerchantSelfDeliveryTimeListService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter YMD = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final int INTERVAL_MINUTES = 30;
	private static final String DEFAULT_HOUR = "08:20-21:00";
	/** Carbon dayOfWeek：0=日 … 6=六 */
	private static final String[] CHINESE_DAYS = {"日", "一", "二", "三", "四", "五", "六", "日"};

	private final Clock clock;

	public MerchantSelfDeliveryTimeListService(@Autowired(required = false) Clock clock) {
		this.clock = clock != null ? clock : Clock.system(SHANGHAI);
	}

	/**
	 * 当 {@code receipt_type=merchant} 且店铺开启自配且 {@code freight_time>0} 时写入 {@code deliveryTimeList}。
	 */
	public void applyIfEligible(Map<String, Object> orderData, Map<String, Object> distributorInfo) {
		if (orderData == null || orderData.isEmpty()) {
			return;
		}
		if (!"merchant".equalsIgnoreCase(trim(orderData.get("receipt_type")))) {
			return;
		}
		if (distributorInfo == null || distributorInfo.isEmpty()) {
			return;
		}
		if (!truthy(distributorInfo.get("is_self_delivery"))) {
			return;
		}
		int freightTime = intVal(distributorInfo.get("freight_time"), 0);
		if (freightTime <= 0) {
			return;
		}
		orderData.put("deliveryTimeList", getDeliveryTimeList(stringVal(distributorInfo.get("hour")), freightTime));
	}

	/** 对齐 PHP {@code getDeliveryTimeList($hour, $freight_time = 2)}。 */
	public Map<String, Object> getDeliveryTimeList(String hour, int freightTimeHours) {
		String hourRange = normalizeHourRange(hour);
		String[] parts = hourRange.split("-", 2);
		String startHour = parts.length > 0 ? parts[0].trim() : "08:20";
		String endHour = parts.length > 1 ? parts[1].trim() : "21:00";

		LocalDateTime now = LocalDateTime.now(clock);
		LocalDate todayDate = now.toLocalDate();
		LocalDate tomorrowDate = todayDate.plusDays(1);

		LocalTime start = parseHm(startHour);
		LocalTime end = parseHm(endHour);
		LocalTime nowHm = LocalTime.of(now.getHour(), now.getMinute());

		LinkedHashMap<String, Object> today = new LinkedHashMap<>();
		today.put("name", "今天");
		today.put("date", YMD.format(todayDate));
		List<String> todayList = new ArrayList<>();
		today.put("list", todayList);

		LinkedHashMap<String, Object> tomorrow = new LinkedHashMap<>();
		int carbonDow = carbonDayOfWeek(tomorrowDate.getDayOfWeek());
		tomorrow.put("name", "明天 周" + CHINESE_DAYS[carbonDow]);
		tomorrow.put("date", YMD.format(tomorrowDate));
		List<String> tomorrowList = new ArrayList<>();
		tomorrow.put("list", tomorrowList);

		todayList.add(HM.format(now.plusHours(freightTimeHours).toLocalTime().withSecond(0).withNano(0)));

		LocalTime current = start;
		while (current.isBefore(end)) {
			LocalTime next = current.plusMinutes(INTERVAL_MINUTES);
			if (!next.isBefore(end)) {
				next = end;
			}
			if (current.isAfter(nowHm)) {
				LocalTime tempCurrent = current.plusHours(freightTimeHours);
				LocalTime tempNext = next.plusHours(freightTimeHours);
				if (!tempNext.isBefore(end)) {
					tempNext = end;
				}
				if (tempCurrent.isBefore(end)) {
					todayList.add(HM.format(tempCurrent) + "-" + HM.format(tempNext));
				}
			}
			tomorrowList.add(HM.format(current) + "-" + HM.format(next));
			current = next;
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>(2);
		out.put("today", today);
		out.put("tomorrow", tomorrow);
		return out;
	}

	private static String normalizeHourRange(String hour) {
		if (!StringUtils.hasText(hour)) {
			return DEFAULT_HOUR;
		}
		String t = hour.trim();
		if (t.isEmpty() || "-".equals(t)) {
			return DEFAULT_HOUR;
		}
		return t;
	}

	private static LocalTime parseHm(String raw) {
		String s = raw == null ? "" : raw.trim();
		if (!StringUtils.hasText(s)) {
			return LocalTime.of(8, 20);
		}
		return LocalTime.parse(s, HM);
	}

	/** Carbon {@code dayOfWeek}: Sunday=0 … Saturday=6. */
	private static int carbonDayOfWeek(DayOfWeek dow) {
		return dow.getValue() % 7;
	}

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(v).trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static String trim(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
