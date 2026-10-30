package cn.shopex.ecshopx.kaquan.service.discount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.kaquan.port.CouponEditEventDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelMemberTagsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class DiscountNewGiftCardUpdateServiceInitPromoteTest {

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

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("INIT + items + distributor_ids=[]：升为 STATUS_NORMAL")
	void updateKaquan_initWithItemsAndEmptyDistributors_promotesToNormal() {
		DiscountNewGiftCardUpdateService service = buildService();
		long cardId = 433L;
		long companyId = 38L;
		DiscountCards existing = baseInitCard(cardId, companyId);

		when(discountCardsMapper.selectOne(any())).thenReturn(existing);
		when(userDiscountMapper.selectCount(any())).thenReturn(0L);
		when(discountCardsMapper.updateById(any(DiscountCards.class))).thenReturn(1);
		when(relItemsMapper.delete(any())).thenReturn(1);
		when(relItemsMapper.insertBatch(any())).thenReturn(1);
		when(relMemberTagsMapper.delete(any())).thenReturn(0);
		doNothing().when(multiLangWriteService).writeAfterUpdate(anyLong(), anyLong(), any());
		when(discountCardsMapper.selectById(cardId)).thenReturn(existing);
		when(rowMapperService.toSnakeCaseMap(existing)).thenReturn(Map.of("card_id", cardId));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", companyId);
		payload.put("card_id", cardId);
		payload.put("kq_status", DiscountNewGiftCardUpdateService.STATUS_INIT);
		payload.put("distributor_ids", "[]");
		payload.put("items", "[{\"id\":\"6938\",\"limit\":5785}]");
		payload.put("quantity", 100);
		payload.put("receive", true);
		payload.put("lock_time", 72);

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> service.updateKaquan(payload));

		ArgumentCaptor<DiscountCards> captor = ArgumentCaptor.forClass(DiscountCards.class);
		verify(discountCardsMapper).updateById(captor.capture());
		assertEquals(DiscountNewGiftCardUpdateService.STATUS_NORMAL, captor.getValue().getKqStatus());
	}

	@Test
	@DisplayName("INIT + 仅 quantity、无 items：保持 STATUS_INIT")
	void updateKaquan_initWithoutItems_staysInit() {
		DiscountNewGiftCardUpdateService service = buildService();
		long cardId = 433L;
		long companyId = 38L;
		DiscountCards existing = baseInitCard(cardId, companyId);

		when(discountCardsMapper.selectOne(any())).thenReturn(existing);
		when(userDiscountMapper.selectCount(any())).thenReturn(0L);
		when(discountCardsMapper.updateById(any(DiscountCards.class))).thenReturn(1);
		when(relMemberTagsMapper.delete(any())).thenReturn(0);
		doNothing().when(multiLangWriteService).writeAfterUpdate(anyLong(), anyLong(), any());
		when(discountCardsMapper.selectById(cardId)).thenReturn(existing);
		when(rowMapperService.toSnakeCaseMap(existing)).thenReturn(Map.of("card_id", cardId));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", companyId);
		payload.put("card_id", cardId);
		payload.put("kq_status", DiscountNewGiftCardUpdateService.STATUS_INIT);
		payload.put("quantity", 100);
		payload.put("lock_time", 72);

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> service.updateKaquan(payload));

		ArgumentCaptor<DiscountCards> captor = ArgumentCaptor.forClass(DiscountCards.class);
		verify(discountCardsMapper).updateById(captor.capture());
		assertEquals(DiscountNewGiftCardUpdateService.STATUS_INIT, captor.getValue().getKqStatus());
	}

	private DiscountNewGiftCardUpdateService buildService() {
		return new DiscountNewGiftCardUpdateService(
				discountCardsMapper,
				relItemsMapper,
				relMemberTagsMapper,
				userDiscountMapper,
				rowMapperService,
				multiLangWriteService,
				couponEditEventDispatchPublisher,
				new DiscountNewGiftCardSetParamsApplier(new ObjectMapper()),
				new ObjectMapper());
	}

	private static DiscountCards baseInitCard(long cardId, long companyId) {
		DiscountCards existing = new DiscountCards();
		existing.setCardId(cardId);
		existing.setCompanyId(companyId);
		existing.setCardType("new_gift");
		existing.setKqStatus(DiscountNewGiftCardUpdateService.STATUS_INIT);
		existing.setDateType(DiscountCardActionValidationService.DATE_TYPE_LONG);
		existing.setSendBeginTime(1_780_848_000);
		existing.setBeginDate(0);
		existing.setFixedTerm(30);
		existing.setQuantity(100);
		existing.setLockTime(72);
		return existing;
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
