package cn.shopex.ecshopx.kaquan.integration.members;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeBatchActiveDelayService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeOrderReceiveService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class AdminMemberBatchVipDelayPortImplBatchVipDelayNoFireJobTest {

	@Mock
	private VipGradeMapper vipGradeMapper;

	@Mock
	private VipGradeBatchActiveDelayService vipGradeBatchActiveDelayService;

	@Mock
	private VipGradeOrderReceiveService vipGradeOrderReceiveService;

	/** Not injected into {@link AdminMemberBatchVipDelayPortImpl}; used only for {@code never()} verification. */
	@Mock
	private FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher;

	private PlatformTransactionManager platformTransactionManager;
	private AdminMemberBatchVipDelayPortImpl port;

	@BeforeEach
	void setUp() {
		platformTransactionManager = noopTransactionManager();
		port = new AdminMemberBatchVipDelayPortImpl(
				vipGradeMapper, vipGradeBatchActiveDelayService, vipGradeOrderReceiveService, platformTransactionManager);
	}

	@Test
	void constructorParameterTypesExcludeFirePromotionsPublisher() {
		assertNotNull(port);
	}

	@Test
	void applyVipDelayForUserChunk_invokesReceiveAdminCustomDelay_neverPublishesFirePromotions() {
		long companyId = 1L;
		long vipGradeId = 99L;
		int addDay = 7;
		Map<Long, String> mobiles = Map.of(1L, "13800000000");

		port.applyVipDelayForUserChunk(companyId, vipGradeId, addDay, mobiles);

		verify(vipGradeOrderReceiveService)
				.receiveAdminCustomDelay(eq(companyId), eq(1L), eq("13800000000"), eq(vipGradeId), eq(addDay));
		verify(firePromotionsActivityDispatchPublisher, never()).publish(anyLong(), any(), any());
	}

	@Test
	void applyVipDelayForUserChunk_emptyMap_skipsReceive_neverPublishesFirePromotions() {
		port.applyVipDelayForUserChunk(1L, 1L, 1, Map.of());
		verifyNoInteractions(vipGradeOrderReceiveService);
		verify(firePromotionsActivityDispatchPublisher, never()).publish(anyLong(), any(), any());
	}

	private static PlatformTransactionManager noopTransactionManager() {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) {
				return new SimpleTransactionStatus(true);
			}

			@Override
			public void commit(TransactionStatus status) {}

			@Override
			public void rollback(TransactionStatus status) {}
		};
	}
}
