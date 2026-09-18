package cn.shopex.ecshopx.kaquan.service.discount;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.kaquan.port.CouponAddEventDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
class DiscountStandardCardCreateServiceCouponAddDispatchPublishAfterCommitProbeTest {

	@Mock
	private DiscountStandardCardSetParamsService setParamsService;

	@Mock
	private DiscountStandardCardItemIdsService itemIdsService;

	@Mock
	private DiscountCardsMapper discountCardsMapper;

	@Mock
	private DiscountStandardCardRelItemsPersistenceService relItemsPersistenceService;

	@Mock
	private DiscountCardsRowMapperService rowMapperService;

	@Mock
	private DiscountCardsMultiLangWriteService multiLangWriteService;

	@Mock
	private CouponAddEventDispatchPublisher couponAddEventDispatchPublisher;

	private DiscountStandardCardCreateService service;

	private void buildService() {
		service = new DiscountStandardCardCreateService(
				setParamsService,
				itemIdsService,
				discountCardsMapper,
				relItemsPersistenceService,
				rowMapperService,
				multiLangWriteService,
				couponAddEventDispatchPublisher,
				new ObjectMapper());
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("createKaquan: afterCommit invokes CouponAddEventDispatchPublisher once with cardId/companyId")
	void createKaquan_afterCommit_invokesCouponAddPublisherWithIds() {
		buildService();
		long expectedCardId = 901L;
		long expectedCompanyId = 42L;

		doNothing().when(setParamsService).apply(any());
		when(itemIdsService.apply(any())).thenAnswer(inv -> inv.getArgument(0));
		doAnswer(
				invocation -> {
					DiscountCards e = invocation.getArgument(0);
					e.setCardId(expectedCardId);
					return 1;
				})
				.when(discountCardsMapper)
				.insert(any(DiscountCards.class));

		doNothing().when(relItemsPersistenceService).persist(any(), eq(expectedCardId), eq(expectedCompanyId));
		doNothing().when(multiLangWriteService).writeAfterCreate(anyLong(), anyLong(), any());

		DiscountCards loaded = new DiscountCards();
		loaded.setCardId(expectedCardId);
		loaded.setCompanyId(expectedCompanyId);
		when(discountCardsMapper.selectById(expectedCardId)).thenReturn(loaded);
		when(rowMapperService.toSnakeCaseMap(loaded)).thenReturn(Map.of("card_id", expectedCardId));

		Map<String, Object> postdata = minimalPostdata(expectedCompanyId);

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> service.createKaquan(postdata, "app"));

		verify(couponAddEventDispatchPublisher, times(1)).publish(eq(expectedCardId), eq(expectedCompanyId));
	}

	private static Map<String, Object> minimalPostdata(long companyId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", companyId);
		m.put("card_type", "normal");
		m.put("coupon_type", "cash");
		m.put("title", "probe");
		m.put("color", "Color010");
		m.put("description", "");
		m.put("date_type", "DATE_TYPE_FIX_TIME_RANGE");
		m.put("begin_date", 0);
		m.put("end_date", 0);
		m.put("use_bound", DiscountStandardCardSetParamsService.FOR_ALL_ITEMS);
		List<String> dist = new ArrayList<>();
		m.put("distributor_id", dist);
		return m;
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
