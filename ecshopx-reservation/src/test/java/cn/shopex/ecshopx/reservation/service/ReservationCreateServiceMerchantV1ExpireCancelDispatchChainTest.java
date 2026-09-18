package cn.shopex.ecshopx.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.ReservationExpireCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ReservationFinishDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ReservationSendSmsNoticeDispatchPublisher;
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import cn.shopex.ecshopx.reservation.port.ReservationSmsOpenTemplatePort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class ReservationCreateServiceMerchantV1ExpireCancelDispatchChainTest {

	private static final Clock FIXED_CLOCK =
			Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);

	@Mock
	private ReservationSettingQueryService reservationSettingQueryService;

	@Mock
	private ReservationShopDetailPort reservationShopDetailPort;

	@Mock
	private ReservationCheckService reservationCheckService;

	@Mock
	private ReservationRecordMapper reservationRecordMapper;

	@Mock
	private ReservationFinishDispatchPublisher reservationFinishDispatchPublisher;

	@Mock
	private ReservationSmsOpenTemplatePort reservationSmsOpenTemplatePort;

	@Mock
	private ReservationSendSmsNoticeDispatchPublisher reservationSendSmsNoticeDispatchPublisher;

	@Mock
	private ReservationExpireCancelDispatchPublisher reservationExpireCancelDispatchPublisher;

	private ReservationCreateService reservationCreateService;

	private ReservationSetting setting;

	@BeforeEach
	void setUp() {
		setting = new ReservationSetting();
		setting.setId(1L);
		setting.setCompanyId(1L);
		setting.setReservationMode(0);
		setting.setTimeInterval(30);
		setting.setSmsDelay(null);

		ReservationAsyncNotifier notifier =
				new ReservationAsyncNotifier(
						reservationSmsOpenTemplatePort,
						reservationSendSmsNoticeDispatchPublisher,
						FIXED_CLOCK);
		reservationCreateService =
				new ReservationCreateService(
						reservationSettingQueryService,
						reservationShopDetailPort,
						reservationCheckService,
						reservationRecordMapper,
						reservationFinishDispatchPublisher,
						notifier,
						reservationExpireCancelDispatchPublisher);

		when(reservationSettingQueryService.findByCompanyId(1L)).thenReturn(Optional.of(setting));
		when(reservationRecordMapper.insert(any(ReservationRecord.class)))
				.thenAnswer(
						invocation -> {
							ReservationRecord r = invocation.getArgument(0);
							if (r.getRecordId() == null) {
								r.setRecordId(100L);
							}
							return 1;
						});
	}

	private Map<String, Object> baseParamsData() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 1L);
		params.put("shop_id", 2L);
		params.put("date_day", "2026-05-01");
		params.put("begin_time", "10:00");
		params.put("status", "success");
		params.put("shop_name", "演示门店");
		params.put("rights_name", "体验权益");
		params.put("mobile", "13900001001");
		return params;
	}

	private Map<String, Object> paramsWithToShopEpoch(long toShopEpoch) {
		var zdt = Instant.ofEpochSecond(toShopEpoch).atZone(ZoneId.systemDefault());
		Map<String, Object> p = new LinkedHashMap<>(baseParamsData());
		p.put("date_day", zdt.toLocalDate().toString());
		p.put("begin_time", String.format("%d:%02d", zdt.getHour(), zdt.getMinute()));
		return p;
	}

	/**
	 * Same key set and typical values as {@code ReservationController#buildParamsData} after
	 * {@code removeBlankOptionalParams} (non-blank optionals, normalized {@code user_id}).
	 */
	private Map<String, Object> adminV1ControllerStyleParamsDataAfterOptionalCleanup(long toShopEpoch) {
		var zdt = Instant.ofEpochSecond(toShopEpoch).atZone(ZoneId.systemDefault());
		String ymd = zdt.toLocalDate().toString();
		String beginTime = String.format("%d:%02d", zdt.getHour(), zdt.getMinute());

		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", 1L);
		m.put("shop_id", 2L);
		m.put("shop_name", "演示门店");
		m.put("resource_level_id", 10L);
		m.put("resource_level_name", "VIP资源位");
		m.put("label_id", 305L);
		m.put("label_name", "上午档");
		m.put("rights_id", 990L);
		m.put("rights_name", "体验权益");
		m.put("date_day", ymd);
		m.put("begin_time", beginTime);
		m.put("end_time", "12:30");
		m.put("num", 1);
		m.put("status", "success");
		m.put("user_name", "张三");
		m.put("mobile", "13900001001");
		m.put("sex", 1);
		m.put("user_id", 10_001L);
		return m;
	}

	@Test
	void createReservation_alwaysSchedulesExpireCancel_independentlyOfSmsTemplates() {
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(1L, "reservation_notice"))
				.thenReturn(null);

		assertEquals(true, reservationCreateService.createReservation(baseParamsData()));

		verify(reservationExpireCancelDispatchPublisher, times(1))
				.publishExpireCancelAfterCreate(
						eq(1L),
						argThat(
								map ->
										map != null
												&& map.get("record_id") != null
												&& ((Number) map.get("record_id")).longValue() == 100L
												&& map.get("to_shop_time") != null));
	}

	@Test
	void createReservation_whenToShopAndClockYieldZeroFormattedDelay_stillCallsPublishExpireCancel() {
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(1L, "reservation_notice"))
				.thenReturn(Collections.emptyMap());

		long nearNow = Instant.now().getEpochSecond();
		long toShopEpoch = nearNow - 7000L;
		Map<String, Object> params = paramsWithToShopEpoch(toShopEpoch);

		assertEquals(true, reservationCreateService.createReservation(params));

		verify(reservationExpireCancelDispatchPublisher, times(1))
				.publishExpireCancelAfterCreate(eq(1L), any(Map.class));
	}

	@Test
	void createReservation_whenParamsMatchAdminV1ControllerBuildParamsData_andSmsTemplatesClosed_stillSchedulesExpireCancelOnce() {
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(1L, "reservation_notice"))
				.thenReturn(Collections.emptyMap());

		long clockSec = FIXED_CLOCK.instant().getEpochSecond();
		long toShop = clockSec + 11200L;
		Map<String, Object> params = adminV1ControllerStyleParamsDataAfterOptionalCleanup(toShop);

		assertEquals(true, reservationCreateService.createReservation(params));

		verify(reservationExpireCancelDispatchPublisher, times(1))
				.publishExpireCancelAfterCreate(
						eq(1L),
						argThat(
								map ->
										map != null
												&& map.get("record_id") != null
												&& ((Number) map.get("record_id")).longValue() == 100L
												&& map.get("to_shop_time") != null));
	}
}
