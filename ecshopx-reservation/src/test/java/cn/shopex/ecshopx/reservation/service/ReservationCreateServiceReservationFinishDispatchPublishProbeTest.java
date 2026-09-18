package cn.shopex.ecshopx.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationCreateServiceReservationFinishDispatchPublishProbeTest {

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
								r.setRecordId(500L);
							}
							return 1;
						});
	}

	@Test
	void createReservation_afterInsert_invokesReservationFinishPublisherWithExpectedEntities() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 1L);
		params.put("shop_id", 2L);
		params.put("date_day", "2026-04-20");
		params.put("begin_time", "09:00");
		params.put("resource_level_id", 30L);
		params.put("status", "success");

		assertTrue(reservationCreateService.createReservation(params));

		InOrder order = inOrder(reservationFinishDispatchPublisher, reservationExpireCancelDispatchPublisher);
		ArgumentCaptor<Map<String, Object>> entitiesCaptor = ArgumentCaptor.captor();
		order.verify(reservationFinishDispatchPublisher).publish(entitiesCaptor.capture());
		Map<String, Object> entities = entitiesCaptor.getValue();
		assertNotNull(entities.get("postdata"));
		assertNotNull(entities.get("result"));
		assertNotNull(entities.get("setting_data"));
		@SuppressWarnings("unchecked")
		Map<String, Object> post = (Map<String, Object>) entities.get("postdata");
		assertEquals(1L, ((Number) post.get("company_id")).longValue());
		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) entities.get("result");
		assertEquals(500L, ((Number) result.get("recordId")).longValue());
		assertEquals(2L, ((Number) result.get("shopId")).longValue());

		order.verify(reservationExpireCancelDispatchPublisher).publishExpireCancelAfterCreate(eq(1L), any());
		verify(reservationFinishDispatchPublisher).publish(any());
	}
}
