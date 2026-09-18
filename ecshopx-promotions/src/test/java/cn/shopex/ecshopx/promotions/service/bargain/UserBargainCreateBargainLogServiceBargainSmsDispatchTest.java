package cn.shopex.ecshopx.promotions.service.bargain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.BargainFinishSendSmsNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.BargainLog;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.UserBargains;
import cn.shopex.ecshopx.promotions.mapper.BargainLogMapper;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class UserBargainCreateBargainLogServiceBargainSmsDispatchTest {

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void createBargainLog_whenCutdownComplete_invokesPublishOnceAfterCommitPath() {
		long endSec = Instant.now().getEpochSecond() + 3600;
		BargainPromotions promotion = new BargainPromotions();
		promotion.setBargainId(10L);
		promotion.setCompanyId(1L);
		promotion.setMktPrice(10_000);
		promotion.setPrice(5_000);
		promotion.setEndTime(endSec);

		UserBargains ub = new UserBargains();
		ub.setBargainId(10L);
		ub.setCompanyId(1L);
		ub.setUserId(20L);
		ub.setMktPrice(10_000);
		ub.setPrice(5_000);
		ub.setCutdownAmount(4_900);
		ub.setIsOrdered(false);
		ub.setItemName("砍价商品");
		ub.setCutpriceRange("[{\"cut\":100,\"used\":0}]");

		BargainPromotionsMapper bargainPromotionsMapper = mock(BargainPromotionsMapper.class);
		BargainLogMapper bargainLogMapper = mock(BargainLogMapper.class);
		UserBargainsMapper userBargainsMapper = mock(UserBargainsMapper.class);
		MessageSource messageSource = mock(MessageSource.class);
		when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("err");
		BargainFinishSendSmsNoticeJobDispatchPublisher publisher =
				mock(BargainFinishSendSmsNoticeJobDispatchPublisher.class);
		ObjectMapper objectMapper = new ObjectMapper();

		when(bargainPromotionsMapper.selectOne(any())).thenReturn(promotion);
		when(bargainLogMapper.selectOne(any())).thenReturn(null);
		when(bargainLogMapper.insert(any(BargainLog.class)))
				.thenAnswer(
						inv -> {
							BargainLog log = inv.getArgument(0);
							if (log.getBargainLogId() == null) {
								log.setBargainLogId(9001L);
							}
							return 1;
						});
		when(userBargainsMapper.selectOne(any())).thenReturn(ub, ub);
		when(userBargainsMapper.update(any(UserBargains.class), any())).thenReturn(1);

		UserBargainCreateBargainLogService svc =
				new UserBargainCreateBargainLogService(
						bargainPromotionsMapper,
						bargainLogMapper,
						userBargainsMapper,
						messageSource,
						objectMapper,
						publisher);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any(TransactionStatus.class));

		TransactionTemplate tt = new TransactionTemplate(txMgr);
		Map<String, Object> merged = baselineMergedParams();

		tt.executeWithoutResult(
				st -> {
					Map<String, Object> row = svc.createBargainLog(merged, Locale.SIMPLIFIED_CHINESE);
					assertNotNull(row);
					verifyNoInteractions(publisher);
				});

		verify(publisher)
				.publishBargainFinishSendSmsNotice(
						eq(1L),
						eq(20L),
						argThat(
								m ->
										m != null
												&& "砍价商品".equals(m.get("item_name"))
												&& Integer.valueOf(5000).equals(m.get("price"))),
						eq(endSec),
						eq(Locale.SIMPLIFIED_CHINESE));
		verifyNoMoreInteractions(publisher);
	}

	@Test
	void createBargainLog_whenCutdownComplete_invokesPublishOnceWithoutTx() {
		long endSec = Instant.now().getEpochSecond() + 3600;
		BargainPromotions promotion = new BargainPromotions();
		promotion.setBargainId(10L);
		promotion.setCompanyId(1L);
		promotion.setMktPrice(10_000);
		promotion.setPrice(5_000);
		promotion.setEndTime(endSec);

		UserBargains ub = new UserBargains();
		ub.setBargainId(10L);
		ub.setCompanyId(1L);
		ub.setUserId(20L);
		ub.setMktPrice(10_000);
		ub.setPrice(5_000);
		ub.setCutdownAmount(4_900);
		ub.setIsOrdered(false);
		ub.setItemName("砍价商品");
		ub.setCutpriceRange("[{\"cut\":100,\"used\":0}]");

		BargainPromotionsMapper bargainPromotionsMapper = mock(BargainPromotionsMapper.class);
		BargainLogMapper bargainLogMapper = mock(BargainLogMapper.class);
		UserBargainsMapper userBargainsMapper = mock(UserBargainsMapper.class);
		MessageSource messageSource = mock(MessageSource.class);
		when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("err");
		BargainFinishSendSmsNoticeJobDispatchPublisher publisher =
				mock(BargainFinishSendSmsNoticeJobDispatchPublisher.class);
		ObjectMapper objectMapper = new ObjectMapper();

		when(bargainPromotionsMapper.selectOne(any())).thenReturn(promotion);
		when(bargainLogMapper.selectOne(any())).thenReturn(null);
		when(bargainLogMapper.insert(any(BargainLog.class)))
				.thenAnswer(
						inv -> {
							BargainLog log = inv.getArgument(0);
							if (log.getBargainLogId() == null) {
								log.setBargainLogId(9002L);
							}
							return 1;
						});
		when(userBargainsMapper.selectOne(any())).thenReturn(ub, ub);
		when(userBargainsMapper.update(any(UserBargains.class), any())).thenReturn(1);

		UserBargainCreateBargainLogService svc =
				new UserBargainCreateBargainLogService(
						bargainPromotionsMapper,
						bargainLogMapper,
						userBargainsMapper,
						messageSource,
						objectMapper,
						publisher);

		Map<String, Object> merged = baselineMergedParams();
		svc.createBargainLog(merged, Locale.SIMPLIFIED_CHINESE);

		verify(publisher)
				.publishBargainFinishSendSmsNotice(
						eq(1L),
						eq(20L),
						argThat(
								m ->
										m != null
												&& "砍价商品".equals(m.get("item_name"))
												&& Integer.valueOf(5000).equals(m.get("price"))),
						eq(endSec),
						eq(Locale.SIMPLIFIED_CHINESE));
		verifyNoMoreInteractions(publisher);
	}

	private static Map<String, Object> baselineMergedParams() {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", 1L);
		merged.put("bargain_id", "10");
		merged.put("user_id", "20");
		merged.put("open_id", "o-test-1");
		merged.put("authorizer_appid", "wxauth");
		merged.put("wxa_appid", "wxa");
		merged.put("nickname", "n");
		merged.put("headimgurl", "h");
		return merged;
	}
}
