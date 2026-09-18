package cn.shopex.ecshopx.deposit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.RechargeSendSmsNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class AdapayDepositTradeRechargeCallbackServiceSmsDispatchTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), DepositTrade.class);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void rechargeCallback_success_invokesPublishRechargeSendSmsNoticeAfterCommit() {
		DepositTradeMapper depositTradeMapper = mock(DepositTradeMapper.class);
		UserDepositBalanceMutationService mutation = mock(UserDepositBalanceMutationService.class);
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(redis.opsForHash()).thenReturn(hashOps);
		when(hashOps.increment(any(), any(), any(Long.class))).thenReturn(1L);
		RechargeSendSmsNoticeJobDispatchPublisher publisher =
				mock(RechargeSendSmsNoticeJobDispatchPublisher.class);

		DepositTrade pending = new DepositTrade();
		pending.setDepositTradeId("d1");
		pending.setCompanyId("10");
		pending.setUserId("20");
		pending.setMobile("13900001001");
		pending.setTradeStatus("WAIT_PAY");
		pending.setCurPayFee("5000");
		pending.setMoney("8000");

		DepositTrade success = new DepositTrade();
		success.setDepositTradeId("d1");
		success.setCompanyId("10");
		success.setUserId("20");
		success.setMobile("13900001001");
		success.setTradeStatus("SUCCESS");
		success.setCurPayFee("5000");
		success.setMoney("8000");

		when(depositTradeMapper.selectById("d1")).thenReturn(pending, success);
		when(depositTradeMapper.update(any(), any())).thenReturn(1);

		AdapayDepositTradeRechargeCallbackService svc =
				new AdapayDepositTradeRechargeCallbackService(
						depositTradeMapper, mutation, redis, publisher);

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
		Map<String, Object> options = new HashMap<>();
		options.put("bank_type", "ALIPAY");
		options.put("transaction_id", "tx1");
		options.put("pay_type", "alipay");

		tt.executeWithoutResult(
				st -> {
					svc.rechargeCallback("d1", "SUCCESS", options);
					verifyNoInteractions(publisher);
				});

		verify(publisher).publishRechargeSendSmsNotice(10L, 20L, "13900001001", 8000L);
	}

	@Test
	void rechargeCallback_nonSuccessStatus_doesNotInvokePublisher() {
		DepositTradeMapper depositTradeMapper = mock(DepositTradeMapper.class);
		UserDepositBalanceMutationService mutation = mock(UserDepositBalanceMutationService.class);
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		RechargeSendSmsNoticeJobDispatchPublisher publisher =
				mock(RechargeSendSmsNoticeJobDispatchPublisher.class);

		DepositTrade row = new DepositTrade();
		row.setDepositTradeId("d1");
		row.setTradeStatus("WAIT_PAY");
		when(depositTradeMapper.selectById("d1")).thenReturn(row);

		AdapayDepositTradeRechargeCallbackService svc =
				new AdapayDepositTradeRechargeCallbackService(
						depositTradeMapper, mutation, redis, publisher);

		svc.rechargeCallback("d1", "FAIL", Map.of());

		verifyNoInteractions(publisher);
	}
}
