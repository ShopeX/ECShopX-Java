package cn.shopex.ecshopx.distribution.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.DistributionEditEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributorUpdateEventDispatchPublisher;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.distribution.integration.JushuitanSettingReadService;
import cn.shopex.ecshopx.distribution.integration.LocalDeliveryShopCreateClient;
import cn.shopex.ecshopx.distribution.integration.WdtErpShopQueryClient;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
class DistributorUpdateOrchestratorDistributionEditDispatchPublishProbeTest {

	@Mock
	private HfpayLedgerConfigReadService hfpayLedgerConfigReadService;

	@Mock
	private LocalDeliveryShopCreateClient localDeliveryShopCreateClient;

	@Mock
	private WdtErpShopQueryClient wdtErpShopQueryClient;

	@Mock
	private JushuitanSettingReadService jushuitanSettingReadService;

	@Mock
	private DistributorWriteRepository distributorWriteRepository;

	@Mock
	private DistributorUpdateService distributorUpdateService;

	@Mock
	private DistributorAftersalesAddressWriteService distributorAftersalesAddressWriteService;

	@Mock
	private DistributorAftersalesAddressReadService distributorAftersalesAddressReadService;

	@Mock
	private CompanysMapper companysMapper;

	@Mock
	private DistributionEditEventDispatchPublisher distributionEditEventDispatchPublisher;

	@Mock
	private DistributorUpdateEventDispatchPublisher distributorUpdateEventDispatchPublisher;

	private DistributorUpdateOrchestrator orchestrator;

	@BeforeEach
	void setUp() {
		orchestrator = new DistributorUpdateOrchestrator(
				hfpayLedgerConfigReadService,
				localDeliveryShopCreateClient,
				wdtErpShopQueryClient,
				jushuitanSettingReadService,
				distributorWriteRepository,
				distributorUpdateService,
				distributorAftersalesAddressWriteService,
				distributorAftersalesAddressReadService,
				companysMapper,
				distributionEditEventDispatchPublisher,
				distributorUpdateEventDispatchPublisher);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("afterCommit publish order — admin distributor update (entry-01 baseline)")
	void update_afterCommit_invokesDistributionEditPublisherThenDistributorUpdatePublisherWithRowSnapshot() {
		long companyId = 42L;
		long pathDistributorId = 910L;
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("is_open", "false");
		runUpdateThroughSyncCommitTransactionTemplate(companyId, pathDistributorId, merged);
		assertDistributionEditThenDistributorUpdatePublishOrder(companyId, pathDistributorId);
	}

	@Test
	@DisplayName("afterCommit publish order — converged shops update path (entry-02-api-distributorshop-updateshops)")
	void update_afterCommit_orderMatchesPhpDispatchEventsWhenUpdateForConvergedShopsUpdatePath() {
		long companyId = 99L;
		long pathDistributorId = 1001L;
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("is_open", "true");
		runUpdateThroughSyncCommitTransactionTemplate(companyId, pathDistributorId, merged);
		assertDistributionEditThenDistributorUpdatePublishOrder(companyId, pathDistributorId);
	}

	private void runUpdateThroughSyncCommitTransactionTemplate(
			long companyId, long pathDistributorId, Map<String, Object> merged) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", companyId);
		row.put("distributor_id", pathDistributorId);

		when(distributorWriteRepository.selectSimpleByCompanyAndId(companyId, pathDistributorId))
				.thenReturn(Optional.empty());
		doNothing().when(wdtErpShopQueryClient).assertShopExistsIfWdtNo(eq(companyId), any());
		doNothing().when(jushuitanSettingReadService).assertEnabledIfJstIdPositive(eq(companyId), any());
		when(distributorUpdateService.performUpdateAndEvents(eq(merged), eq(pathDistributorId), eq("zh-CN")))
				.thenReturn(row);

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(
				status -> orchestrator.update(merged, Map.of("merchant_id", 0L), pathDistributorId, "zh-CN", null));
	}

	private void assertDistributionEditThenDistributorUpdatePublishOrder(long companyId, long pathDistributorId) {
		InOrder inOrder = inOrder(distributionEditEventDispatchPublisher, distributorUpdateEventDispatchPublisher);
		inOrder
				.verify(distributionEditEventDispatchPublisher)
				.publish(
						argThat(m -> companyId == toLong(m.get("company_id"))
								&& pathDistributorId == toLong(m.get("distributor_id"))));
		inOrder
				.verify(distributorUpdateEventDispatchPublisher)
				.publish(
						argThat(m -> companyId == toLong(m.get("company_id"))
								&& pathDistributorId == toLong(m.get("distributor_id"))));
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

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
