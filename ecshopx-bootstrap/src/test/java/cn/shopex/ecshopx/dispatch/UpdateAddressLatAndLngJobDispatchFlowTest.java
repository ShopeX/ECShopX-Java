package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersBundleDispatchJobNames;
import cn.shopex.ecshopx.common.distribution.CompanyMapGeocodePort;
import cn.shopex.ecshopx.members.dispatch.UpdateAddressLatAndLngJobDispatchPublisher;
import cn.shopex.ecshopx.members.dispatch.UpdateAddressLatAndLngJobHandler;
import cn.shopex.ecshopx.members.domain.MembersAddress;
import cn.shopex.ecshopx.members.mapper.MembersAddressMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class UpdateAddressLatAndLngJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch job 184 async enqueues on slow queue and consumer invokes handler")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesHandler() {
		CompanyMapGeocodePort geocodePort = Mockito.mock(CompanyMapGeocodePort.class);
		MembersAddressMapper membersAddressMapper = Mockito.mock(MembersAddressMapper.class);
		StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(valueOps);
		when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

		UpdateAddressLatAndLngJobHandler handler =
				new UpdateAddressLatAndLngJobHandler(geocodePort, membersAddressMapper, redis);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(MembersBundleDispatchJobNames.UPDATE_ADDRESS_LAT_AND_LNG_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = samplePayload();
		facade.dispatchJob(
				MembersBundleDispatchJobNames.UPDATE_ADDRESS_LAT_AND_LNG_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(MembersBundleDispatchJobNames.UPDATE_ADDRESS_LAT_AND_LNG_JOB, msg.messageName());
		assertNull(msg.listenerName());

		MembersAddress row = new MembersAddress();
		row.setAddressId(300L);
		row.setCompanyId(100L);
		row.setUserId(200L);
		row.setCity("Shanghai");
		row.setAdrdetail("Nanjing Road");
		when(membersAddressMapper.selectOne(Mockito.any())).thenReturn(row);
		when(geocodePort.geocode(100L, "Shanghai", "Nanjing Road"))
				.thenReturn(new CompanyMapGeocodePort.GeocodeLatLng("31.23", "121.47"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(geocodePort).geocode(100L, "Shanghai", "Nanjing Road");
		ArgumentCaptor<MembersAddress> patchCap = ArgumentCaptor.forClass(MembersAddress.class);
		verify(membersAddressMapper).update(patchCap.capture(), Mockito.any());
		assertEquals("31.23", patchCap.getValue().getLat());
		assertEquals("121.47", patchCap.getValue().getLng());
		verify(redis).delete(eq("ecshopx:members:address_lat_lng:100:300"));
	}

	@Test
	void dispatchJob_publishPayloadMatchesUpdateAddressLatAndLngEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		UpdateAddressLatAndLngJobDispatchPublisher publisher =
				new UpdateAddressLatAndLngJobDispatchPublisher(dispatchFacade);

		publisher.enqueueUpdateAddressLatAndLng(100L, 200L, 300L);

		verify(dispatchFacade)
				.dispatchJob(
						eq(MembersBundleDispatchJobNames.UPDATE_ADDRESS_LAT_AND_LNG_JOB),
						Mockito.argThat(
								m ->
										m != null
												&& 100L == asLong(m.get("company_id"))
												&& 200L == asLong(m.get("user_id"))
												&& 300L == asLong(m.get("address_id"))),
						Mockito.argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& opts.retryPolicy() != null));
	}

	private static Map<String, Object> samplePayload() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 100L);
		payload.put("user_id", 200L);
		payload.put("address_id", 300L);
		return payload;
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
