package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DepositDispatchJobNames;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class RechargeSendSmsNoticeJobDispatchPublisherImplTest {

	@Test
	void publishRechargeSendSmsNotice_delegatesToDispatchFacade_withJob134AndSmsQueueRedisAsyncRetryDefault() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		RechargeSendSmsNoticeJobDispatchPublisherImpl impl =
				new RechargeSendSmsNoticeJobDispatchPublisherImpl(facade);

		impl.publishRechargeSendSmsNotice(11L, 22L, "13900001002", 900L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

		verify(facade).dispatchJob(nameCaptor.capture(), payloadCaptor.capture(), optionsCaptor.capture());

		assertEquals(DepositDispatchJobNames.RECHARGE_SEND_SMS_NOTICE, nameCaptor.getValue());
		Map<String, Object> pl = payloadCaptor.getValue();
		assertEquals(11L, pl.get("companyId"));
		assertEquals(22L, pl.get("userId"));
		assertEquals("13900001002", pl.get("mobile"));
		assertEquals(900L, pl.get("totalFee"));

		DispatchOptions opts = optionsCaptor.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("sms", opts.queue());
		assertNull(opts.delay());
		assertEquals(RetryPolicy.platformDefault(), opts.retryPolicy());
	}

	@Test
	void publishRechargeSendSmsNotice_rejectsNullMobile() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		RechargeSendSmsNoticeJobDispatchPublisherImpl impl =
				new RechargeSendSmsNoticeJobDispatchPublisherImpl(facade);

		assertThrows(
				BadRequestException.class, () -> impl.publishRechargeSendSmsNotice(1L, 2L, null, 100L));
	}
}
