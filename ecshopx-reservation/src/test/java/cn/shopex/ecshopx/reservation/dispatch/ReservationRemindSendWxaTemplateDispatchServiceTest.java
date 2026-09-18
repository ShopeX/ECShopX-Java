package cn.shopex.ecshopx.reservation.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.ReservationDispatchJobNames;
import cn.shopex.ecshopx.common.port.reservation.ReservationFinishSuccWxaTemplatePort;
import cn.shopex.ecshopx.common.port.reservation.ReservationUserWxappIdentityResolvePort;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import cn.shopex.ecshopx.reservation.support.ReservationSmsQueueDelayFormatter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationRemindSendWxaTemplateDispatchServiceTest {

	@Mock
	private DispatchFacade dispatchFacade;

	@Mock
	private ReservationShopDetailPort reservationShopDetailPort;

	@Mock
	private ReservationFinishSuccWxaTemplatePort reservationFinishSuccWxaTemplatePort;

	@Mock
	private ReservationUserWxappIdentityResolvePort reservationUserWxappIdentityResolvePort;

	@Mock
	private ReservationRecordMapper reservationRecordMapper;

	private ReservationRemindSendWxaTemplateDispatchService service;

	private void initService(Clock clock) {
		service =
				new ReservationRemindSendWxaTemplateDispatchService(
						dispatchFacade,
						clock,
						reservationShopDetailPort,
						reservationFinishSuccWxaTemplatePort,
						reservationUserWxappIdentityResolvePort,
						reservationRecordMapper);
	}

	@Test
	void handle_whenComputedDelayPositive_dispatchesDeferJobWithExpectedDelayAndReturnsEarly() {
		Clock clock = Clock.fixed(Instant.parse("2026-05-10T12:00:00Z"), ZoneOffset.UTC);
		initService(clock);
		long toShopEpoch = Instant.parse("2026-05-12T15:00:00Z").getEpochSecond();
		int smsHours = 1;
		long raw =
				(toShopEpoch - smsHours * 3600L) - clock.instant().getEpochSecond();
		long expectedDelaySeconds =
				ReservationSmsQueueDelayFormatter.formatQueueDelaySeconds(raw);

		Map<String, Object> payload = samplePayload(toShopEpoch, 1L, 2L, 99L);

		service.handle(payload);

		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(dispatchFacade)
				.dispatchJob(
						eq(ReservationDispatchJobNames.JOB_RESERVATION_REMIND_SEND_WXA_TEMPLATE_DEFER),
						eq(payload),
						optionsCaptor.capture());
		DispatchOptions opts = optionsCaptor.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("default", opts.queue());
		assertEquals(Duration.ofSeconds(expectedDelaySeconds), opts.delay());
		verify(reservationFinishSuccWxaTemplatePort, never()).send(any());
	}

	@Test
	void handle_whenRecordNotSuccess_skipsTemplatePort() {
		Clock clock = Clock.fixed(Instant.parse("2026-05-10T12:00:00Z"), ZoneOffset.UTC);
		initService(clock);
		long toShopEpoch = clock.instant().getEpochSecond() + 1800L;
		Map<String, Object> payload = samplePayload(toShopEpoch, 1L, 2L, 99L);

		ReservationRecord pending = new ReservationRecord();
		pending.setStatus("pending");
		when(reservationRecordMapper.selectById(99L)).thenReturn(pending);

		service.handle(payload);

		verify(dispatchFacade, never()).dispatchJob(any(), any(), any());
		verify(reservationFinishSuccWxaTemplatePort, never()).send(any());
	}

	@Test
	void handle_whenDelayNonPositive_sendsReservationRemindTemplate() {
		Clock clock = Clock.fixed(Instant.parse("2026-05-10T12:00:00Z"), ZoneOffset.UTC);
		initService(clock);
		long toShopEpoch = clock.instant().getEpochSecond() + 1800L;
		Map<String, Object> payload = samplePayload(toShopEpoch, 1L, 2L, 99L);

		ReservationRecord ok = new ReservationRecord();
		ok.setStatus("success");
		when(reservationRecordMapper.selectById(99L)).thenReturn(ok);
		Map<String, Object> shop = new LinkedHashMap<>();
		shop.put("shop_name", "S1");
		shop.put("shop_address", "Addr");
		shop.put("company_id", 1L);
		when(reservationShopDetailPort.getShopsDetail(2L, 1L)).thenReturn(shop);

		service.handle(payload);

		verify(dispatchFacade, never()).dispatchJob(any(), any(), any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> wxCaptor = ArgumentCaptor.forClass(Map.class);
		verify(reservationFinishSuccWxaTemplatePort).send(wxCaptor.capture());
		Map<String, Object> wx = wxCaptor.getValue();
		assertEquals("reservationRemind", wx.get("scenes_name"));
		verify(reservationUserWxappIdentityResolvePort).fillIfMissing(eq(1L), eq(3L), any());
	}

	private static Map<String, Object> samplePayload(
			long toShopEpoch, long companyId, long shopId, long recordId) {
		Map<String, Object> post = new LinkedHashMap<>();
		post.put("company_id", companyId);
		post.put("shop_id", shopId);
		post.put("user_id", 3L);
		post.put("date_day", "2026-05-10");
		post.put("begin_time", "12:00");
		post.put("wxapp_appid", "wxapp");
		post.put("open_id", "oid");
		post.put("rights_name", "Gold");
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("recordId", recordId);
		result.put("to_shop_time", toShopEpoch);
		Map<String, Object> setting = Map.of("smsDelay", 1);
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("postdata", post);
		entities.put("result", result);
		entities.put("setting_data", setting);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);
		return payload;
	}
}
