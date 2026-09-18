package cn.shopex.ecshopx.members.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.members.admin.AdminMemberBatchDiscountCardInventoryPort;
import cn.shopex.ecshopx.common.members.admin.AdminMemberBatchVipDelayPort;
import cn.shopex.ecshopx.members.dispatch.BatchActionMembersJobDispatchPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminMemberBatchOperatingServiceSmallBatchSyncTest {

	@Mock
	private AdminMemberBatchOperatingFilterService adminMemberBatchOperatingFilterService;

	@Mock
	private AdminMemberRelTagsUserIdsQueryService adminMemberRelTagsUserIdsQueryService;

	@Mock
	private AdminMemberBatchDiscountCardInventoryPort adminMemberBatchDiscountCardInventoryPort;

	@Mock
	private AdminMemberBatchVipDelayPort adminMemberBatchVipDelayPort;

	@Mock
	private AdminMemberBatchOperatingCountService adminMemberBatchOperatingCountService;

	@Mock
	private AdminMemberBatchOperatingUserIdsPageService adminMemberBatchOperatingUserIdsPageService;

	@Mock
	private AdminMemberBatchOperatingChunkExecutor adminMemberBatchOperatingChunkExecutor;

	@Mock
	private BatchActionMembersJobDispatchPublisher batchActionMembersJobDispatchPublisher;

	private AdminMemberBatchOperatingService newService() {
		return new AdminMemberBatchOperatingService(
				adminMemberBatchOperatingFilterService,
				adminMemberRelTagsUserIdsQueryService,
				adminMemberBatchDiscountCardInventoryPort,
				adminMemberBatchVipDelayPort,
				adminMemberBatchOperatingCountService,
				adminMemberBatchOperatingUserIdsPageService,
				adminMemberBatchOperatingChunkExecutor,
				batchActionMembersJobDispatchPublisher,
				new ObjectMapper());
	}

	@Test
	void whenMemberCountAtMost100_thenExecutesChunkInline_andDoesNotEnqueueBatchActionMembersJob() {
		when(adminMemberBatchOperatingCountService.countMembersForBatchOperating(anyLong(), any())).thenReturn(100L);

		AdminMemberBatchOperatingService service = newService();

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("user_id", 1L);
		merged.put("action_type", "send_sms");
		merged.put("sms_content", "hello");

		Map<String, Object> operatorJwt = new LinkedHashMap<>();
		operatorJwt.put("operator_type", "staff");
		operatorJwt.put("username", "alice");
		operatorJwt.put("mobile", "13800000000");
		operatorJwt.put("distributor_id", 0L);
		operatorJwt.put("company_id", "1");

		long companyId = 42L;
		service.batchProcessMemberData(companyId, operatorJwt, merged);

		verify(adminMemberBatchOperatingChunkExecutor, times(1))
				.executeChunk(eq(companyId), any(), eq("send_sms"), any(), eq(1), eq(100));
		verify(batchActionMembersJobDispatchPublisher, never())
				.enqueueBatchActionMembersChunk(anyLong(), any(), any(), any(), anyInt(), anyInt());
		verifyNoInteractions(adminMemberBatchOperatingUserIdsPageService);
		verifyNoInteractions(adminMemberBatchOperatingFilterService);
	}
}
