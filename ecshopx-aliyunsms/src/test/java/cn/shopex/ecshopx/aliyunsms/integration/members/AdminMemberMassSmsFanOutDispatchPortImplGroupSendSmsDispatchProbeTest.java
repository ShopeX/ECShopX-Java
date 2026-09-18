package cn.shopex.ecshopx.aliyunsms.integration.members;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminMemberMassSmsFanOutDispatchPortImplGroupSendSmsDispatchProbeTest {

	@Test
	@DisplayName("dispatchFanOutAfterPersist invokes GroupSendSmsJobDispatchPublisher with controller SMS payload shape")
	void dispatchFanOutAfterPersist_invokesGroupSendSmsJobDispatchPublisherWithControllerSmsPayloadShape() {
		GroupSendSmsJobDispatchPublisher publisher = mock(GroupSendSmsJobDispatchPublisher.class);
		AdminMemberMassSmsFanOutDispatchPortImpl impl = new AdminMemberMassSmsFanOutDispatchPortImpl(publisher);

		impl.dispatchFanOutAfterPersist(100L, List.of("13800138000", "13900139000"), "body text");

		verify(publisher)
				.enqueueGroupSendSms(
						argThat(
								m ->
										m != null
												&& 100L == asLong(m.get("company_id"))
												&& "管理员".equals(m.get("operator"))
												&& "body text".equals(m.get("sms_content"))
												&& m.get("send_to_phones") instanceof List<?> phones
												&& phones.size() == 2
												&& "13800138000".equals(phones.get(0))
												&& "13900139000".equals(phones.get(1))
												&& "".equals(m.get("sender"))
												&& 0L == asLong(m.get("distributor_id"))));
	}

	@Test
	void dispatchFanOutAfterPersist_nullSmsContent_mapsToEmptyString() {
		GroupSendSmsJobDispatchPublisher publisher = mock(GroupSendSmsJobDispatchPublisher.class);
		AdminMemberMassSmsFanOutDispatchPortImpl impl = new AdminMemberMassSmsFanOutDispatchPortImpl(publisher);

		impl.dispatchFanOutAfterPersist(42L, List.of("15500001111"), null);

		verify(publisher)
				.enqueueGroupSendSms(
						argThat(
								m ->
										m != null
												&& 42L == asLong(m.get("company_id"))
												&& "".equals(m.get("sms_content"))));
	}

	@Test
	void dispatchFanOutAfterPersist_noEnqueueWhenAllMobilesBlank() {
		GroupSendSmsJobDispatchPublisher publisher = mock(GroupSendSmsJobDispatchPublisher.class);
		AdminMemberMassSmsFanOutDispatchPortImpl impl = new AdminMemberMassSmsFanOutDispatchPortImpl(publisher);

		List<String> allBlank = new ArrayList<>();
		allBlank.add("  ");
		allBlank.add("");
		allBlank.add(null);
		impl.dispatchFanOutAfterPersist(1L, allBlank, "x");

		verify(publisher, never()).enqueueGroupSendSms(any());
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
