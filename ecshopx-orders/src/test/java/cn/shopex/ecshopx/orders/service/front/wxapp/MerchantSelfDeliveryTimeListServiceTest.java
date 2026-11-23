package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MerchantSelfDeliveryTimeListServiceTest {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	@Test
	void getDeliveryTimeList_fixedClock_buildsTodayTomorrowSlotsAlignedWithPhp() {
		// 2026-11-23 Monday 10:00 Asia/Shanghai → tomorrow 周二
		Clock clock = Clock.fixed(Instant.parse("2026-11-23T02:00:00Z"), SHANGHAI);
		MerchantSelfDeliveryTimeListService svc = new MerchantSelfDeliveryTimeListService(clock);

		Map<String, Object> ranges = svc.getDeliveryTimeList("08:20-21:00", 2);

		@SuppressWarnings("unchecked")
		Map<String, Object> today = (Map<String, Object>) ranges.get("today");
		@SuppressWarnings("unchecked")
		Map<String, Object> tomorrow = (Map<String, Object>) ranges.get("tomorrow");
		@SuppressWarnings("unchecked")
		List<String> todayList = (List<String>) today.get("list");
		@SuppressWarnings("unchecked")
		List<String> tomorrowList = (List<String>) tomorrow.get("list");

		assertThat(today.get("name")).isEqualTo("今天");
		assertThat(today.get("date")).isEqualTo(LocalDate.of(2026, 11, 23).toString());
		assertThat(todayList.get(0)).isEqualTo("12:00");
		assertThat(todayList).contains("12:20-12:50");

		assertThat(tomorrow.get("name")).isEqualTo("明天 周二");
		assertThat(tomorrow.get("date")).isEqualTo(LocalDate.of(2026, 11, 24).toString());
		assertThat(tomorrowList.get(0)).isEqualTo("08:20-08:50");
		assertThat(tomorrowList).contains("20:50-21:00");
	}

	@Test
	void getDeliveryTimeList_blankHour_usesPhpDefaultRange() {
		Clock clock = Clock.fixed(Instant.parse("2026-11-23T02:00:00Z"), SHANGHAI);
		MerchantSelfDeliveryTimeListService svc = new MerchantSelfDeliveryTimeListService(clock);

		Map<String, Object> ranges = svc.getDeliveryTimeList("-", 2);
		@SuppressWarnings("unchecked")
		Map<String, Object> tomorrow = (Map<String, Object>) ranges.get("tomorrow");
		@SuppressWarnings("unchecked")
		List<String> tomorrowList = (List<String>) tomorrow.get("list");
		assertThat(tomorrowList.get(0)).isEqualTo("08:20-08:50");
	}

	@Test
	void applyIfEligible_onlyWhenMerchantSelfDeliveryWithFreightTime() {
		Clock clock = Clock.fixed(Instant.parse("2026-11-23T02:00:00Z"), SHANGHAI);
		MerchantSelfDeliveryTimeListService svc = new MerchantSelfDeliveryTimeListService(clock);

		Map<String, Object> di = new LinkedHashMap<>();
		di.put("is_self_delivery", Boolean.TRUE);
		di.put("freight_time", 2);
		di.put("hour", "08:20-21:00");

		Map<String, Object> od = new LinkedHashMap<>();
		od.put("receipt_type", "merchant");
		svc.applyIfEligible(od, di);
		assertThat(od).containsKey("deliveryTimeList");

		Map<String, Object> odZiti = new LinkedHashMap<>();
		odZiti.put("receipt_type", "ziti");
		svc.applyIfEligible(odZiti, di);
		assertThat(odZiti).doesNotContainKey("deliveryTimeList");

		Map<String, Object> odNoFreight = new LinkedHashMap<>();
		odNoFreight.put("receipt_type", "merchant");
		Map<String, Object> diZero = new LinkedHashMap<>(di);
		diZero.put("freight_time", 0);
		svc.applyIfEligible(odNoFreight, diZero);
		assertThat(odNoFreight).doesNotContainKey("deliveryTimeList");
	}
}
