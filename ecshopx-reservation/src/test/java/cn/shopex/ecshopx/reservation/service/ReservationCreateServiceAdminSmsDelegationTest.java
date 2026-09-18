package cn.shopex.ecshopx.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
import cn.shopex.ecshopx.reservation.support.ReservationSmsQueueDelayFormatter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class ReservationCreateServiceAdminSmsDelegationTest {

	private static final long COMPANY_ID = 77L;

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

	private ReservationAsyncNotifier spyNotifier;

	private ReservationCreateService reservationCreateService;

	private ReservationSetting setting;

	@BeforeEach
	void setUp() {
		setting = new ReservationSetting();
		setting.setId(10L);
		setting.setCompanyId(COMPANY_ID);
		setting.setReservationMode(0);
		setting.setTimeInterval(30);
		setting.setSmsDelay(null);

		ReservationAsyncNotifier realNotifier =
				new ReservationAsyncNotifier(
						reservationSmsOpenTemplatePort,
						reservationSendSmsNoticeDispatchPublisher,
						FIXED_CLOCK);
		spyNotifier = spy(realNotifier);
		reservationCreateService =
				new ReservationCreateService(
						reservationSettingQueryService,
						reservationShopDetailPort,
						reservationCheckService,
						reservationRecordMapper,
						reservationFinishDispatchPublisher,
						spyNotifier,
						reservationExpireCancelDispatchPublisher);

		when(reservationSettingQueryService.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(setting));
		when(reservationRecordMapper.insert(any(ReservationRecord.class)))
				.thenAnswer(
						invocation -> {
							ReservationRecord r = invocation.getArgument(0);
							if (r.getRecordId() == null) {
								r.setRecordId(9001L);
							}
							return 1;
						});
	}

	private Map<String, Object> baseParamsData() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", COMPANY_ID);
		params.put("shop_id", 42L);
		params.put("date_day", "2026-05-01");
		params.put("begin_time", "10:00");
		params.put("status", "success");
		params.put("shop_name", "Admin Shop");
		params.put("rights_name", "VIP");
		params.put("mobile", "13800138000");
		params.put("shop_address", "1 Admin Plaza");
		params.put("telephone", "010-88886666");
		params.put("user_name", "导购创建");
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
	 * {@code removeBlankOptionalParams} (non-blank optionals, normalized {@code user_id});
	 * {@code company_id} uses this admin test's {@link #COMPANY_ID}.
	 */
	private Map<String, Object> adminV1ControllerStyleParamsDataAfterOptionalCleanup(long toShopEpoch) {
		var zdt = Instant.ofEpochSecond(toShopEpoch).atZone(ZoneId.systemDefault());
		String ymd = zdt.toLocalDate().toString();
		String beginTime = String.format("%d:%02d", zdt.getHour(), zdt.getMinute());

		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", COMPANY_ID);
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
	void createReservation_afterInsert_notifiesAsyncNotifierWithSnakeCasePayloadIncludingPassthrough() {
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "reservation_notice"))
				.thenReturn(Map.of("open", true));
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "gotoShop_notice"))
				.thenReturn(Collections.emptyMap());

		String dateDay = "2026-05-07";
		String beginTime = "15:30";
		int expectedToShopTime =
				(int)
						LocalDate.parse(dateDay)
								.atTime(LocalTime.parse(beginTime, DateTimeFormatter.ofPattern("HH:mm")))
								.atZone(ZoneId.systemDefault())
								.toEpochSecond();

		Map<String, Object> paramsData = new LinkedHashMap<>();
		paramsData.put("company_id", COMPANY_ID);
		paramsData.put("shop_id", 42L);
		paramsData.put("date_day", dateDay);
		paramsData.put("begin_time", beginTime);
		paramsData.put("status", "success");
		paramsData.put("shop_name", "Admin Shop");
		paramsData.put("rights_name", "VIP");
		paramsData.put("mobile", "13800138000");
		paramsData.put("shop_address", "1 Admin Plaza");
		paramsData.put("telephone", "010-88886666");

		assertTrue(reservationCreateService.createReservation(paramsData));

		InOrder order = inOrder(reservationFinishDispatchPublisher, spyNotifier);
		ArgumentCaptor<Map<String, Object>> finishCaptor = ArgumentCaptor.captor();
		order.verify(reservationFinishDispatchPublisher).publish(finishCaptor.capture());
		assertNotNull(finishCaptor.getValue().get("postdata"));

		ArgumentCaptor<Map<String, Object>> snapshotCaptor = ArgumentCaptor.captor();
		order.verify(spyNotifier).afterReservationCreated(eq(COMPANY_ID), snapshotCaptor.capture(), eq(setting));
		Map<String, Object> m = snapshotCaptor.getValue();
		assertNotNull(m);
		assertEquals(COMPANY_ID, ((Number) m.get("company_id")).longValue());
		assertEquals(9001L, ((Number) m.get("record_id")).longValue());
		assertEquals(expectedToShopTime, ((Number) m.get("to_shop_time")).intValue());
		assertEquals("1 Admin Plaza", m.get("shop_address"));
		assertEquals("010-88886666", m.get("telephone"));
		assertNotNull(m.get("setting_data"));

		verify(reservationSendSmsNoticeDispatchPublisher, times(1))
				.publishReservationNoticeSms(eq(COMPANY_ID), eq(m));
		verify(reservationSendSmsNoticeDispatchPublisher, never())
				.publishGotoShopNoticeSmsDelayed(
						anyLong(),
						ArgumentMatchers.<Map<String, Object>>any(),
						ArgumentMatchers.any(Duration.class));

		verify(reservationExpireCancelDispatchPublisher, times(1))
				.publishExpireCancelAfterCreate(eq(COMPANY_ID), eq(m));
	}

	@Test
	void createReservation_whenReservationNoticeTemplateOpen_thenAfterReservationCreatedAndPublishNoticeSmsAreInvokedWithExpectedPayloads() {
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "reservation_notice"))
				.thenReturn(Map.of("open", true));
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "gotoShop_notice"))
				.thenReturn(Collections.emptyMap());

		boolean ok = reservationCreateService.createReservation(baseParamsData());
		assertEquals(true, ok);

		InOrder order = inOrder(reservationFinishDispatchPublisher, spyNotifier);
		ArgumentCaptor<Map<String, Object>> finishCaptor = ArgumentCaptor.captor();
		order.verify(reservationFinishDispatchPublisher).publish(finishCaptor.capture());
		Map<String, Object> published = finishCaptor.getValue();
		assertNotNull(published.get("postdata"));
		@SuppressWarnings("unchecked")
		Map<String, Object> settingData = (Map<String, Object>) published.get("setting_data");
		assertNotNull(settingData);
		assertEquals(0, settingData.get("reservationMode"));

		ArgumentCaptor<Map<String, Object>> snapshotCaptor = ArgumentCaptor.captor();
		order.verify(spyNotifier).afterReservationCreated(eq(COMPANY_ID), snapshotCaptor.capture(), eq(setting));
		Map<String, Object> snapshot = snapshotCaptor.getValue();
		assertEquals(COMPANY_ID, snapshot.get("company_id"));
		assertEquals(42L, snapshot.get("shop_id"));
		assertEquals(9001L, snapshot.get("record_id"));
		assertEquals("13800138000", snapshot.get("mobile"));
		assertEquals("Admin Shop", snapshot.get("shop_name"));
		assertEquals("VIP", snapshot.get("rights_name"));
		assertEquals("1 Admin Plaza", snapshot.get("shop_address"));
		assertEquals("010-88886666", snapshot.get("telephone"));
		assertEquals("导购创建", snapshot.get("user_name"));

		ArgumentCaptor<Map<String, Object>> publishCaptor = ArgumentCaptor.captor();
		verify(reservationSendSmsNoticeDispatchPublisher, times(1))
				.publishReservationNoticeSms(eq(COMPANY_ID), publishCaptor.capture());
		Map<String, Object> publishedPayload = publishCaptor.getValue();
		assertEquals(snapshot, publishedPayload);

		verify(reservationExpireCancelDispatchPublisher, times(1))
				.publishExpireCancelAfterCreate(eq(COMPANY_ID), eq(snapshot));
	}

	@Test
	void createReservation_whenReservationNoticeTemplateClosed_thenPublishReservationNoticeSmsNotCalled() {
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "reservation_notice"))
				.thenReturn(Collections.emptyMap());

		boolean ok = reservationCreateService.createReservation(baseParamsData());
		assertEquals(true, ok);

		verify(spyNotifier, times(1))
				.afterReservationCreated(
						eq(COMPANY_ID), ArgumentMatchers.<Map<String, Object>>any(), eq(setting));
		verify(reservationSendSmsNoticeDispatchPublisher, never())
				.publishReservationNoticeSms(anyLong(), any());
		verify(reservationSendSmsNoticeDispatchPublisher, never())
				.publishGotoShopNoticeSmsDelayed(
						anyLong(),
						ArgumentMatchers.<Map<String, Object>>any(),
						ArgumentMatchers.any(Duration.class));

		verify(reservationExpireCancelDispatchPublisher, times(1))
				.publishExpireCancelAfterCreate(eq(COMPANY_ID), any());
	}

	@Test
	void createReservation_whenReservationNoticeAndGotoShopOpen_thenPublishesGotoShopDelayedWithExpectedDelay() {
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "reservation_notice"))
				.thenReturn(Map.of("open", true));
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "gotoShop_notice"))
				.thenReturn(Map.of("open", true));
		setting.setSmsDelay("2");
		long clockSec = FIXED_CLOCK.instant().getEpochSecond();
		long toShop = clockSec + 11200L;
		Map<String, Object> params = paramsWithToShopEpoch(toShop);

		boolean ok = reservationCreateService.createReservation(params);
		assertEquals(true, ok);

		ArgumentCaptor<Map<String, Object>> snapshotCaptor = ArgumentCaptor.captor();
		verify(spyNotifier).afterReservationCreated(eq(COMPANY_ID), snapshotCaptor.capture(), eq(setting));
		Map<String, Object> snapshot = snapshotCaptor.getValue();
		long raw = (toShop - 2L * 3600L) - clockSec;
		long d = ReservationSmsQueueDelayFormatter.formatQueueDelaySeconds(raw);
		verify(reservationSendSmsNoticeDispatchPublisher, times(1))
				.publishGotoShopNoticeSmsDelayed(eq(COMPANY_ID), eq(snapshot), eq(Duration.ofSeconds(d)));

		verify(reservationExpireCancelDispatchPublisher, times(1))
				.publishExpireCancelAfterCreate(eq(COMPANY_ID), eq(snapshot));
	}

	@Test
	void createReservation_whenParamsMatchAdminV1ControllerBuildParamsData_andBothTemplatesOpen_thenPublishesGotoShopDelayedWithExpectedDelay() {
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "reservation_notice"))
				.thenReturn(Map.of("open", true));
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "gotoShop_notice"))
				.thenReturn(Map.of("open", true));
		setting.setSmsDelay("2");
		long clockSec = FIXED_CLOCK.instant().getEpochSecond();
		long toShop = clockSec + 11200L;
		Map<String, Object> params = adminV1ControllerStyleParamsDataAfterOptionalCleanup(toShop);

		boolean ok = reservationCreateService.createReservation(params);
		assertEquals(true, ok);

		ArgumentCaptor<Map<String, Object>> snapshotCaptor = ArgumentCaptor.captor();
		verify(spyNotifier).afterReservationCreated(eq(COMPANY_ID), snapshotCaptor.capture(), eq(setting));
		Map<String, Object> snapshot = snapshotCaptor.getValue();
		long raw = (toShop - 2L * 3600L) - clockSec;
		long d = ReservationSmsQueueDelayFormatter.formatQueueDelaySeconds(raw);
		verify(reservationSendSmsNoticeDispatchPublisher, times(1))
				.publishGotoShopNoticeSmsDelayed(eq(COMPANY_ID), eq(snapshot), eq(Duration.ofSeconds(d)));

		verify(reservationExpireCancelDispatchPublisher, times(1))
				.publishExpireCancelAfterCreate(eq(COMPANY_ID), eq(snapshot));
	}

	@Test
	void createReservation_whenParamsMatchAdminV1ControllerBuildParamsData_andReservationNoticeClosed_stillSchedulesExpireCancelOnce() {
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "reservation_notice"))
				.thenReturn(Collections.emptyMap());

		long clockSec = FIXED_CLOCK.instant().getEpochSecond();
		long toShop = clockSec + 11200L;
		Map<String, Object> params = adminV1ControllerStyleParamsDataAfterOptionalCleanup(toShop);

		assertTrue(reservationCreateService.createReservation(params));

		verify(reservationSendSmsNoticeDispatchPublisher, never())
				.publishReservationNoticeSms(anyLong(), any());
		verify(reservationSendSmsNoticeDispatchPublisher, never())
				.publishGotoShopNoticeSmsDelayed(
						anyLong(),
						ArgumentMatchers.<Map<String, Object>>any(),
						ArgumentMatchers.any(Duration.class));

		verify(reservationExpireCancelDispatchPublisher, times(1))
				.publishExpireCancelAfterCreate(
						eq(COMPANY_ID),
						ArgumentMatchers.argThat(
								m ->
										m != null
												&& ((Number) m.get("record_id")).longValue() == 9001L
												&& m.get("to_shop_time") != null));
	}

	@Test
	void createReservation_whenGotoShopOpenButReservationNoticeClosed_thenNoGotoShopDispatch() {
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "reservation_notice"))
				.thenReturn(Collections.emptyMap());
		lenient()
				.when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "gotoShop_notice"))
				.thenReturn(Map.of("open", true));

		boolean ok = reservationCreateService.createReservation(baseParamsData());
		assertEquals(true, ok);

		verify(reservationSendSmsNoticeDispatchPublisher, never())
				.publishGotoShopNoticeSmsDelayed(
						anyLong(),
						ArgumentMatchers.<Map<String, Object>>any(),
						ArgumentMatchers.any(Duration.class));

		verify(reservationExpireCancelDispatchPublisher, times(1))
				.publishExpireCancelAfterCreate(eq(COMPANY_ID), any());
	}

	@Test
	void createReservation_whenFormattedDelayNonPositive_thenNoGotoShopDispatch() {
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "reservation_notice"))
				.thenReturn(Map.of("open", true));
		when(reservationSmsOpenTemplatePort.getOpenTemplateInfo(COMPANY_ID, "gotoShop_notice"))
				.thenReturn(Map.of("open", true));
		setting.setSmsDelay("2");
		long clockSec = FIXED_CLOCK.instant().getEpochSecond();
		long toShop = clockSec + 7500L;
		Map<String, Object> params = paramsWithToShopEpoch(toShop);

		boolean ok = reservationCreateService.createReservation(params);
		assertEquals(true, ok);

		verify(reservationSendSmsNoticeDispatchPublisher, times(1))
				.publishReservationNoticeSms(anyLong(), any());
		verify(reservationSendSmsNoticeDispatchPublisher, never())
				.publishGotoShopNoticeSmsDelayed(
						anyLong(),
						ArgumentMatchers.<Map<String, Object>>any(),
						ArgumentMatchers.any(Duration.class));

		verify(reservationExpireCancelDispatchPublisher, times(1))
				.publishExpireCancelAfterCreate(eq(COMPANY_ID), any());
	}
}
