package cn.shopex.ecshopx.youshu.dispatch;

import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.youshu.service.YoushuItemCategoryAddSyncService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemCategoryAddDispatchListenerTest {

	@Mock
	private YoushuItemCategoryAddSyncService syncService;

	@InjectMocks
	private ItemCategoryAddDispatchListener listener;

	@Test
	void onEvent_delegatesCompanyIdToSyncService() {
		listener.onEvent(Map.of("company_id", 42L));
		verify(syncService).syncAfterCategoryAdd(42L);
	}
}
