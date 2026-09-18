package cn.shopex.ecshopx.kaquan.service.discount;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.KaquanDispatchEventNames;
import cn.shopex.ecshopx.common.kaquan.port.CouponEditEventDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelMemberTagsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
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
class DiscountNewGiftCardUpdateServiceCouponEditDispatchPublishAfterCommitProbeTest {

	@Mock
	private DiscountCardsMapper discountCardsMapper;

	@Mock
	private RelItemsMapper relItemsMapper;

	@Mock
	private RelMemberTagsMapper relMemberTagsMapper;

	@Mock
	private UserDiscountMapper userDiscountMapper;

	@Mock
	private DiscountCardsRowMapperService rowMapperService;

	@Mock
	private DiscountCardsMultiLangWriteService multiLangWriteService;

	@Mock
	private CouponEditEventDispatchPublisher couponEditEventDispatchPublisher;

	@Mock
	private DiscountNewGiftCardSetParamsApplier newGiftSetParamsApplier;

	private DiscountNewGiftCardUpdateService service;

	private void buildService() {
		service = new DiscountNewGiftCardUpdateService(
				discountCardsMapper,
				relItemsMapper,
				relMemberTagsMapper,
				userDiscountMapper,
				rowMapperService,
				multiLangWriteService,
				couponEditEventDispatchPublisher,
				newGiftSetParamsApplier,
				new ObjectMapper());
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("updateKaquan: afterCommit invokes CouponEdit publisher once with event:286 message name")
	void updateKaquan_afterCommit_invokesCouponEditPublisherOnce_withEvent286MessageName() {
		buildService();
		long expectedCardId = 904L;
		long expectedCompanyId = 45L;

		doNothing().when(newGiftSetParamsApplier).apply(any(), eq(false));

		DiscountCards existing = new DiscountCards();
		existing.setCardId(expectedCardId);
		existing.setCompanyId(expectedCompanyId);
		existing.setCardType("new_gift");
		existing.setKqStatus(DiscountNewGiftCardUpdateService.STATUS_NORMAL);
		existing.setDateType(DiscountCardActionValidationService.DATE_TYPE_SHORT);
		existing.setBeginDate(2_000_000_000);
		existing.setEndDate(2_000_003_600);
		existing.setQuantity(10);

		when(discountCardsMapper.selectOne(any())).thenReturn(existing);
		when(discountCardsMapper.updateById(existing)).thenReturn(1);
		when(relMemberTagsMapper.delete(any())).thenReturn(1);
		doNothing().when(multiLangWriteService).writeAfterUpdate(anyLong(), anyLong(), any());

		DiscountCards loaded = new DiscountCards();
		loaded.setCardId(expectedCardId);
		loaded.setCompanyId(expectedCompanyId);
		when(discountCardsMapper.selectById(expectedCardId)).thenReturn(loaded);
		when(rowMapperService.toSnakeCaseMap(loaded)).thenReturn(Map.of("card_id", expectedCardId));

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> service.updateKaquan(minimalUpdatePayload(expectedCompanyId, expectedCardId)));

		verify(couponEditEventDispatchPublisher, times(1))
				.publish(
						eq(expectedCardId),
						eq(expectedCompanyId),
						eq(KaquanDispatchEventNames.EVENT_COUPON_EDIT_NEW_GIFT));
	}

	private static Map<String, Object> minimalUpdatePayload(long companyId, long cardId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", companyId);
		m.put("card_id", cardId);
		m.put("title", "probe-new-gift-update");
		m.put("coupon_type", "gift");
		m.put("color", "Color010");
		m.put("description", "");
		m.put("date_type", DiscountCardActionValidationService.DATE_TYPE_SHORT);
		m.put("begin_time", 0);
		m.put("end_time", 3600);
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
