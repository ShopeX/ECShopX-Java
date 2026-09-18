package cn.shopex.ecshopx.youshu.dispatch;

import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.youshu.service.YoushuMemberCreateSuccessSyncService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateMemberSuccessYoushuDispatchListenerTest {

	@Mock
	private YoushuMemberCreateSuccessSyncService syncService;

	@InjectMocks
	private CreateMemberSuccessYoushuDispatchListener listener;

	@Test
	void onEvent_delegatesToSyncService() {
		listener.onEvent(Map.of("company_id", 42L, "user_id", 100L));
		verify(syncService).syncMemberAfterCreate(42L, 100L);
	}
}
