package cn.shopex.ecshopx.kaquan.service.discount;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.kaquan.port.CouponDeleteEventDispatchPublisher;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.mapper.WechatRelCardMapper;
import cn.shopex.ecshopx.wechat.service.openplatform.WechatOpenPlatformCardDeleteClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class DiscountCardDeleteFacadeServiceCouponDeleteDispatchPublishAfterCommitProbeTest {

	@Mock
	private UserDiscountMapper userDiscountMapper;

	@Mock
	private DiscountCardsMapper discountCardsMapper;

	@Mock
	private RelItemsMapper relItemsMapper;

	@Mock
	private WechatRelCardMapper wechatRelCardMapper;

	@Mock
	private WechatOpenPlatformCardDeleteClient wechatOpenPlatformCardDeleteClient;

	@Mock
	private CouponDeleteEventDispatchPublisher couponDeleteEventDispatchPublisher;

	private DiscountCardDeleteFacadeService service;

	private void buildService() {
		service = new DiscountCardDeleteFacadeService(
				userDiscountMapper,
				discountCardsMapper,
				relItemsMapper,
				wechatRelCardMapper,
				wechatOpenPlatformCardDeleteClient,
				couponDeleteEventDispatchPublisher,
				syncFiringTxManager());
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("deleteDiscountCard: afterCommit invokes CouponDeleteEventDispatchPublisher once even when main delete affects 0 rows")
	void deleteDiscountCard_afterCommit_invokesCouponDeleteEventDispatchPublisher_once_withExpectedIds() {
		buildService();
		long expectedCardId = 701L;
		long expectedCompanyId = 100L;

		when(userDiscountMapper.selectCount(any())).thenReturn(0L);
		when(discountCardsMapper.delete(any())).thenReturn(0);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("card_id", expectedCardId);

		service.deleteDiscountCard(merged, expectedCompanyId, null);

		verify(relItemsMapper, never()).delete(any());
		verify(couponDeleteEventDispatchPublisher, times(1)).publish(eq(expectedCardId), eq(expectedCompanyId));
	}

	@Test
	@DisplayName("deleteDiscountCard(..., checkUserReceived=false): never queries user_discount; afterCommit publishes")
	void deleteDiscountCard_skipUserDiscountCheck_stillDeletesAndPublishesAfterCommit() {
		buildService();
		long expectedCardId = 701L;
		long expectedCompanyId = 100L;

		when(discountCardsMapper.delete(any())).thenReturn(1);
		when(relItemsMapper.delete(any())).thenReturn(1);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("card_id", expectedCardId);

		service.deleteDiscountCard(merged, expectedCompanyId, null, false);

		verify(userDiscountMapper, never()).selectCount(any());
		verify(couponDeleteEventDispatchPublisher, times(1)).publish(eq(expectedCardId), eq(expectedCompanyId));
	}

	@Test
	@DisplayName("deleteDiscountCard default path: user_discount rows present throws before publish")
	void deleteDiscountCard_default_whenUserDiscountPresent_throwsWithoutPublish() {
		buildService();
		when(userDiscountMapper.selectCount(any())).thenReturn(3L);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("card_id", 701L);

		assertThrows(ResourceException.class, () -> service.deleteDiscountCard(merged, 100L, null));

		verify(couponDeleteEventDispatchPublisher, never()).publish(anyLong(), anyLong());
	}

	private static PlatformTransactionManager syncFiringTxManager() {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					throw new IllegalStateException("nested tx not expected in unit test");
				}
				TransactionSynchronizationManager.initSynchronization();
				return new SimpleTransactionStatus(true);
			}

			@Override
			public void commit(TransactionStatus status) throws TransactionException {
				try {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization s :
								new ArrayList<>(TransactionSynchronizationManager.getSynchronizations())) {
							s.afterCommit();
						}
					}
				} finally {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						TransactionSynchronizationManager.clearSynchronization();
					}
				}
			}

			@Override
			public void rollback(TransactionStatus status) throws TransactionException {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					TransactionSynchronizationManager.clearSynchronization();
				}
			}
		};
	}
}
