package cn.shopex.ecshopx.kaquan.integration.dm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.kaquan.port.CouponDeleteEventDispatchPublisher;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.mapper.WechatRelCardMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardDeleteFacadeService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountStandardCardCreateService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountStandardCardSetParamsService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountStandardCardUpdateService;
import cn.shopex.ecshopx.wechat.service.openplatform.WechatOpenPlatformCardDeleteClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

@ExtendWith(MockitoExtension.class)
class DmCardTemplateMessageNotifyKaquanAdapterCouponDeleteFromDeleteTopicProbeTest {

	@Mock
	private DiscountStandardCardCreateService standardCardCreateService;

	@Mock
	private DiscountStandardCardUpdateService standardCardUpdateService;

	@Mock
	private DiscountCardsMapper discountCardsMapper;

	@Mock
	private UserDiscountMapper userDiscountMapper;

	@Mock
	private RelItemsMapper relItemsMapper;

	@Mock
	private WechatRelCardMapper wechatRelCardMapper;

	@Mock
	private WechatOpenPlatformCardDeleteClient wechatOpenPlatformCardDeleteClient;

	@Mock
	private CouponDeleteEventDispatchPublisher couponDeleteEventDispatchPublisher;

	private DmCardTemplateMessageNotifyKaquanAdapter adapter;

	private DiscountCardDeleteFacadeService deleteFacade;

	private void buildAdapterAndFacade() {
		deleteFacade = new DiscountCardDeleteFacadeService(
				userDiscountMapper,
				discountCardsMapper,
				relItemsMapper,
				wechatRelCardMapper,
				wechatOpenPlatformCardDeleteClient,
				couponDeleteEventDispatchPublisher,
				syncFiringTxManager());
		adapter = new DmCardTemplateMessageNotifyKaquanAdapter(
				standardCardCreateService,
				standardCardUpdateService,
				discountCardsMapper,
				deleteFacade,
				new ObjectMapper());
	}

	@Test
	@DisplayName("sync_card_template_delete: DM skip-check path; user_discount not queried; afterCommit publishes once")
	void handle_syncCardTemplateDelete_whenUserDiscountWouldBlockApiPath_skipsCheck_andAfterCommitPublishesOnce() {
		buildAdapterAndFacade();
		long expectedCardId = 902L;
		long expectedCompanyId = 43L;

		when(discountCardsMapper.delete(any())).thenReturn(1);
		when(relItemsMapper.delete(any())).thenReturn(1);
		when(wechatRelCardMapper.selectOne(any())).thenReturn(null);

		Map<String, Object> content = minimalPostdata(expectedCompanyId);
		content.put("card_id", expectedCardId);
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("topic", "sync_card_template_delete");
		merged.put("content", content);

		adapter.handle(expectedCompanyId, merged, "app-id");

		verify(userDiscountMapper, never()).selectCount(any());
		verify(couponDeleteEventDispatchPublisher, times(1)).publish(eq(expectedCardId), eq(expectedCompanyId));
		verify(standardCardCreateService, never()).createKaquan(any(), any());
		verify(standardCardUpdateService, never()).updateKaquan(any(), anyString(), any());
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
