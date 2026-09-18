package cn.shopex.ecshopx.thirdparty.service.shopexcrm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateMemberSuccessShopexCrmSyncExecutionServiceTest {

	@Mock
	private ShopexCrmSyncSingleMemberPort port;

	@Test
	void whenCrmSyncBlank_skipsPort() {
		UpdateMemberSuccessShopexCrmSyncExecutionService service =
				new UpdateMemberSuccessShopexCrmSyncExecutionService(port, "");
		service.executeAfterUpdateMemberSuccess(Map.of("company_id", 1L, "user_id", 2L));
		verify(port, never()).syncUpdatedMember(anyLong(), anyLong());
	}

	@Test
	void whenCrmSyncSet_invokesSyncUpdatedMember() {
		UpdateMemberSuccessShopexCrmSyncExecutionService service =
				new UpdateMemberSuccessShopexCrmSyncExecutionService(port, "on");
		service.executeAfterUpdateMemberSuccess(Map.of("company_id", 3L, "user_id", 4L));
		verify(port).syncUpdatedMember(3L, 4L);
	}

	@Test
	void whenMissingCompanyId_throwsBadRequest() {
		UpdateMemberSuccessShopexCrmSyncExecutionService service =
				new UpdateMemberSuccessShopexCrmSyncExecutionService(port, "on");
		assertThrows(
				BadRequestException.class,
				() -> service.executeAfterUpdateMemberSuccess(Map.of("user_id", 1L)));
		verify(port, never()).syncUpdatedMember(anyLong(), anyLong());
	}

	@Test
	void whenMissingUserId_throwsBadRequest() {
		UpdateMemberSuccessShopexCrmSyncExecutionService service =
				new UpdateMemberSuccessShopexCrmSyncExecutionService(port, "on");
		assertThrows(
				BadRequestException.class,
				() -> service.executeAfterUpdateMemberSuccess(Map.of("company_id", 1L)));
		verify(port, never()).syncUpdatedMember(anyLong(), anyLong());
	}
}
