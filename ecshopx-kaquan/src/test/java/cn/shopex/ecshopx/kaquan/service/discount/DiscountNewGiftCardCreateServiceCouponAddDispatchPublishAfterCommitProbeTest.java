package cn.shopex.ecshopx.kaquan.service.discount;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.KaquanDispatchEventNames;
import cn.shopex.ecshopx.common.kaquan.port.CouponAddEventDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelMemberTagsMapper;
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
class DiscountNewGiftCardCreateServiceCouponAddDispatchPublishAfterCommitProbeTest {

	@Mock
	private DiscountCardsMapper discountCardsMapper;

	@Mock
	private RelItemsMapper relItemsMapper;

	@Mock
	private RelMemberTagsMapper relMemberTagsMapper;

	@Mock
	private DiscountCardsRowMapperService rowMapperService;

	@Mock
	private DiscountCardsMultiLangWriteService multiLangWriteService;

	@Mock
	private CouponAddEventDispatchPublisher couponAddEventDispatchPublisher;

	@Mock
	private DiscountNewGiftCardSetParamsApplier newGiftSetParamsApplier;

	private DiscountNewGiftCardCreateService service;

	private void buildService() {
		service = new DiscountNewGiftCardCreateService(
				discountCardsMapper,
				relItemsMapper,
				relMemberTagsMapper,
				rowMapperService,
				multiLangWriteService,
				couponAddEventDispatchPublisher,
				new ObjectMapper(),
				newGiftSetParamsApplier);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("new_gift createKaquan: afterCommit publishes CouponAdd with event:284 message name and ids")
	void createKaquan_afterCommit_invokesCouponAddPublisherWithIds() {
		buildService();
		long expectedCardId = 903L;
		long expectedCompanyId = 44L;

		doNothing().when(newGiftSetParamsApplier).apply(any(), anyBoolean());
		doAnswer(
				invocation -> {
					DiscountCards e = invocation.getArgument(0);
					e.setCardId(expectedCardId);
					e.setCompanyId(expectedCompanyId);
					return 1;
				})
				.when(discountCardsMapper)
				.insert(any(DiscountCards.class));

		doNothing().when(multiLangWriteService).writeAfterCreate(anyLong(), anyLong(), any());

		DiscountCards loaded = new DiscountCards();
		loaded.setCardId(expectedCardId);
		loaded.setCompanyId(expectedCompanyId);
		when(discountCardsMapper.selectById(expectedCardId)).thenReturn(loaded);
		when(rowMapperService.toSnakeCaseMap(loaded)).thenReturn(Map.of("card_id", expectedCardId));

		Map<String, Object> data = minimalNewGiftPostdata(expectedCompanyId);

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> service.createKaquan(data));

		verify(couponAddEventDispatchPublisher, times(1))
				.publish(
						eq(expectedCardId),
						eq(expectedCompanyId),
						eq(KaquanDispatchEventNames.EVENT_COUPON_ADD_NEW_GIFT));
	}

	private static Map<String, Object> minimalNewGiftPostdata(long companyId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", companyId);
		m.put("card_type", "new_gift");
		m.put("coupon_type", "gift");
		m.put("title", "probe-new-gift");
		m.put("color", "Color010");
		m.put("description", "");
		m.put("date_type", "DATE_TYPE_FIX_TIME_RANGE");
		m.put("begin_date", 0);
		m.put("end_date", 0);
		m.put("use_bound", DiscountNewGiftCardSetParamsApplier.USE_BOUND_ALL_ITEMS);
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
