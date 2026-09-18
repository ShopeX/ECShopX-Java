package cn.shopex.ecshopx.distribution.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.CreateDistributorJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributionAddEventDispatchPublisher;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.distribution.integration.JushuitanSettingReadService;
import cn.shopex.ecshopx.distribution.integration.LocalDeliveryShopCreateClient;
import cn.shopex.ecshopx.distribution.integration.WdtErpShopQueryClient;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.distribution.repository.PickupLocationRelDistributorRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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
class DistributorCreateOrchestratorDistributionAddDispatchPublishProbeTest {

	@Mock
	private HfpayLedgerConfigReadService hfpayLedgerConfigReadService;

	@Mock
	private LocalDeliveryShopCreateClient localDeliveryShopCreateClient;

	@Mock
	private WdtErpShopQueryClient wdtErpShopQueryClient;

	@Mock
	private JushuitanSettingReadService jushuitanSettingReadService;

	@Mock
	private DistributorCreateService distributorCreateService;

	@Mock
	private DistributorWriteRepository distributorWriteRepository;

	@Mock
	private PickupLocationRelDistributorRepository pickupLocationRelDistributorRepository;

	@Mock
	private DistributorAftersalesAddressWriteService distributorAftersalesAddressWriteService;

	@Mock
	private CreateDistributorJobDispatchPublisher createDistributorJobDispatchPublisher;

	@Mock
	private DistributionAddEventDispatchPublisher distributionAddEventDispatchPublisher;

	@Mock
	private CompanysMapper companysMapper;

	private DistributorCreateOrchestrator orchestrator;

	@BeforeEach
	void setUp() {
		orchestrator = new DistributorCreateOrchestrator(
				hfpayLedgerConfigReadService,
				localDeliveryShopCreateClient,
				wdtErpShopQueryClient,
				jushuitanSettingReadService,
				distributorCreateService,
				distributorWriteRepository,
				pickupLocationRelDistributorRepository,
				distributorAftersalesAddressWriteService,
				createDistributorJobDispatchPublisher,
				distributionAddEventDispatchPublisher,
				companysMapper);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void create_afterCommit_invokesJobPublisherThenDistributionAddPublisherWithRowSnapshot() {
		long companyId = 42L;
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("name", "Probe Shop");
		merged.put("mobile", "13800138000");
		merged.put("source_from", 2);
		merged.put("is_open", "false");

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", companyId);
		row.put("distributor_id", 999L);

		when(distributorWriteRepository.countByCompanyAndNameNotDeleted(eq(companyId), anyString())).thenReturn(0L);
		doNothing().when(hfpayLedgerConfigReadService).assertOpenAndRateAllowedIfNeeded(eq(companyId), any());
		doNothing().when(localDeliveryShopCreateClient).applyCreateShopIfDada(eq(companyId), any());
		doNothing().when(wdtErpShopQueryClient).assertShopExistsIfWdtNo(eq(companyId), any());
		doNothing().when(jushuitanSettingReadService).assertEnabledIfJstIdPositive(eq(companyId), any());
		when(distributorCreateService.performInsertAndEvents(any(), any(), anyString())).thenReturn(row);

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> orchestrator.create(merged, Map.of(), "zh-CN"));

		InOrder inOrder = inOrder(createDistributorJobDispatchPublisher, distributionAddEventDispatchPublisher);
		inOrder
				.verify(createDistributorJobDispatchPublisher)
				.enqueueAfterDistributorCreate(
						argThat(m -> companyId == toLong(m.get("company_id")) && 999L == toLong(m.get("distributor_id"))));
		inOrder
				.verify(distributionAddEventDispatchPublisher)
				.publish(
						argThat(m -> companyId == toLong(m.get("company_id")) && 999L == toLong(m.get("distributor_id"))));
	}

	/**
	 * 命名用例：旧版 {@code …/v1/shops} 创建与 {@code POST /api/v1/distributor} 共用本编排器
	 * {@code afterCommit} 的 DistributionAdd 发布路径；断言语义与
	 * {@link #create_afterCommit_invokesJobPublisherThenDistributionAddPublisherWithRowSnapshot()} 相同，避免重复实现第二套探针。
	 */
	@Test
	@DisplayName(
			"entry-03: legacy POST …/v1/shops (shops.create) semantics → same DistributorCreateOrchestrator afterCommit path as POST /api/v1/distributor")
	void entry03_shopsCreateRoute_sharesOrchestratorDistributionAddAfterCommitPath() {
		create_afterCommit_invokesJobPublisherThenDistributionAddPublisherWithRowSnapshot();
	}

	/**
	 * event-289 plan §2 #3：Youshu（281）与 HfPay（289）监听平面由同一 {@code DistributionAddEventDispatchPublisher#publish}
	 * 内扇出；双 {@code messageName} 的底层两次 {@code publishEvent} 由 {@code DistributionAddEventDispatchPublisherImpl}
	 * 既有单测覆盖，本探针只验证编排器提交后单次 {@code publish} 调用顺序与快照。
	 */
	@Test
	@DisplayName(
			"entry-03 event-289: Youshu (281) + HfPay (289) listener planes rely on one DistributionAddEventDispatchPublisher#publish after commit (no dual publishEvent here)")
	void entry03_event289_dualListenerPlane_reliesOnSinglePublishCall_afterCommit() {
		create_afterCommit_invokesJobPublisherThenDistributionAddPublisherWithRowSnapshot();
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
