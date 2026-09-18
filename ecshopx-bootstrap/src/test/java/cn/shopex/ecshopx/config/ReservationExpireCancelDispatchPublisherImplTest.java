package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.ReservationDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
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
class ReservationExpireCancelDispatchPublisherImplTest {

	@Mock
	private DispatchFacade dispatchFacade;

	@Test
	void publishExpireCancelAfterCreate_usesFormattedDelayFromToShopPlusTwoHours() {
		Instant now = Instant.ofEpochSecond(10_000L);
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);
		ReservationExpireCancelDispatchPublisherImpl impl =
				new ReservationExpireCancelDispatchPublisherImpl(dispatchFacade, clock);
		long toShop = 20_000L;
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("to_shop_time", toShop);
		snapshot.put("record_id", 55L);

		long raw = toShop + 7200L - now.getEpochSecond();
		long expectedSeconds = ReservationSmsQueueDelayFormatter.formatQueueDelaySeconds(raw);

		impl.publishExpireCancelAfterCreate(77L, snapshot);

		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(dispatchFacade)
				.dispatchJob(
						eq(ReservationDispatchJobNames.RESERVATION_EXPIRE_CANCEL_DELAYED), anyMap(), optionsCaptor.capture());
		assertEquals(Duration.ofSeconds(expectedSeconds), optionsCaptor.getValue().delay());
		assertEquals("default", optionsCaptor.getValue().queue());
	}

	@Test
	void publishExpireCancelAfterCreate_whenFormattedDelayZero_stillDispatchesWithZeroDuration() {
		Instant now = Instant.ofEpochSecond(50_000L);
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);
		ReservationExpireCancelDispatchPublisherImpl impl =
				new ReservationExpireCancelDispatchPublisherImpl(dispatchFacade, clock);
		long toShop = 40_000L;
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("to_shop_time", toShop);

		long raw = toShop + 7200L - now.getEpochSecond();
		long expectedSeconds = ReservationSmsQueueDelayFormatter.formatQueueDelaySeconds(raw);
		assertEquals(0L, expectedSeconds);

		impl.publishExpireCancelAfterCreate(1L, snapshot);

		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(dispatchFacade)
				.dispatchJob(
						eq(ReservationDispatchJobNames.RESERVATION_EXPIRE_CANCEL_DELAYED), anyMap(), optionsCaptor.capture());
		assertEquals(Duration.ZERO, optionsCaptor.getValue().delay());
	}
}
