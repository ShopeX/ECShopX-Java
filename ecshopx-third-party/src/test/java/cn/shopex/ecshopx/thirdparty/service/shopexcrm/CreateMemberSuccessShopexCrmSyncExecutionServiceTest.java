package cn.shopex.ecshopx.thirdparty.service.shopexcrm;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateMemberSuccessShopexCrmSyncExecutionServiceTest {

	@Mock
	private ShopexCrmSyncSingleMemberPort port;

	@Test
	void whenCrmSyncBlank_skipsPort() {
		CreateMemberSuccessShopexCrmSyncExecutionService service =
				new CreateMemberSuccessShopexCrmSyncExecutionService(port, "");
		service.executeAfterCreateMemberSuccess(Map.of("company_id", 1L, "user_id", 2L));
		verify(port, never()).syncUpdatedMember(anyLong(), anyLong());
	}

	@Test
	void whenCrmSyncSet_invokesSyncUpdatedMember() {
		CreateMemberSuccessShopexCrmSyncExecutionService service =
				new CreateMemberSuccessShopexCrmSyncExecutionService(port, "on");
		service.executeAfterCreateMemberSuccess(Map.of("company_id", 3L, "user_id", 4L));
		verify(port).syncUpdatedMember(3L, 4L);
	}
}
