package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundQueueMessage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AftersalesRefundJobServiceTest {

	@Mock
	private AftersalesRefundMapper aftersalesRefundMapper;

	@Mock
	private AftersalesRefundJobSideEffects sideEffects;

	@InjectMocks
	private AftersalesRefundJobService jobService;

	@Test
	@DisplayName("§3.2-1,2,3 getInfo 空 → false，无 doRefund")
	void noRow_false() {
		when(aftersalesRefundMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
		assertThat(jobService.handle(new AftersalesRefundQueueMessage(1L, 1L, 1L))).isFalse();
		verify(sideEffects, never()).doOnlineRefund(any());
	}

	@Test
	@DisplayName("§3.2-3 非 AUDIT_SUCCESS → false")
	void statusNotAudit_false() {
		AftersalesRefund r = base(1L, 1L);
		r.setRefundStatus("READY");
		when(aftersalesRefundMapper.selectOne(any(QueryWrapper.class))).thenReturn(r);
		assertThat(jobService.handle(new AftersalesRefundQueueMessage(1L, 1L, 1L))).isFalse();
	}

	@Test
	@DisplayName("§3.2-4 offline_pay → true，无 doOnline 支付外呼")
	void offlinePay_true_noOnlineRefund() {
		AftersalesRefund r = base(1L, 1L);
		r.setPayType("offline_pay");
		when(aftersalesRefundMapper.selectOne(any(QueryWrapper.class))).thenReturn(r);
		assertThat(jobService.handle(new AftersalesRefundQueueMessage(1L, 1L, 2L))).isTrue();
		verify(sideEffects, never()).doOnlineRefund(any());
	}

	@Test
	@DisplayName("§3.2-5-2 售前非 bargain：不调用 processBargain")
	void preSaleNonBargainNoProcessBargain() {
		AftersalesRefund r = base(1L, 1L);
		r.setAftersalesBn(null);
		r.setPayType("wxpay");
		r.setTradeId("T1");
		when(aftersalesRefundMapper.selectOne(any(QueryWrapper.class))).thenReturn(r);
		when(sideEffects.loadTrade(eq("T1"), eq(1L)))
				.thenReturn(new TradeInfoSnapshot("T1", "normal"));
		assertThat(jobService.handle(new AftersalesRefundQueueMessage(1L, 1L, 3L))).isTrue();
		verify(sideEffects, never()).processBargainOrder(anyLong(), anyLong());
	}

	@Test
	@DisplayName("§3.2-5-1,2,3 售前：doRefund（内含 TradeRefundFinish）→ 若 bargain 则 processBargain，不再二次 notify")
	void preSaleBargainPath() {
		AftersalesRefund r = base(1L, 1L);
		r.setOrderId(9L);
		r.setAftersalesBn(null);
		r.setPayType("wxpay");
		r.setTradeId("T1");
		when(aftersalesRefundMapper.selectOne(any(QueryWrapper.class))).thenReturn(r);
		when(sideEffects.loadTrade(eq("T1"), eq(1L)))
				.thenReturn(new TradeInfoSnapshot("T1", "bargain"));
		assertThat(jobService.handle(new AftersalesRefundQueueMessage(1L, 1L, 9L))).isTrue();
		verify(sideEffects, times(1)).doOnlineRefund(r);
		verify(sideEffects, never()).notifyTradeRefundSettled(any());
		verify(sideEffects, times(1)).processBargainOrder(1L, 9L);
		verify(sideEffects, never()).updateAftersaleFinished(any());
	}

	@Test
	@DisplayName("§3.2-6-1 渠道从非 offline 被提示为 offline 后走 6-2（PHP 不发 TradeRefundFinish）")
	void afterSaleChannelPatchedToOffline() {
		AftersalesRefund r = base(1L, 1L);
		r.setAftersalesBn(99L);
		r.setPayType("wxpay");
		r.setRefundChannel("original");
		AftersalesRefund patched = base(1L, 1L);
		patched.setAftersalesBn(99L);
		patched.setPayType("wxpay");
		patched.setRefundChannel("offline");
		when(aftersalesRefundMapper.selectOne(any(QueryWrapper.class))).thenReturn(r);
		when(sideEffects.applyOfflineChannelHintFromOrder(r)).thenReturn(patched);
		assertThat(jobService.handle(new AftersalesRefundQueueMessage(1L, 1L, 3L))).isTrue();
		InOrder inOrder = inOrder(sideEffects);
		inOrder.verify(sideEffects).updateRefundToSuccess(patched);
		inOrder.verify(sideEffects).updateAftersaleFinished(patched);
		verify(sideEffects, never()).doOnlineRefund(any());
		verify(sideEffects, never()).notifyTradeRefundSettled(any());
	}

	@Test
	@DisplayName("§3.2-6-1,6-2,6-4 线下售后：只置成功后收尾（不对齐二次 notify）")
	void afterSaleOffline() {
		AftersalesRefund r = base(1L, 1L);
		r.setAftersalesBn(99L);
		r.setPayType("wxpay");
		r.setRefundChannel("offline");
		when(aftersalesRefundMapper.selectOne(any(QueryWrapper.class))).thenReturn(r);
		when(sideEffects.applyOfflineChannelHintFromOrder(r)).thenReturn(r);
		assertThat(jobService.handle(new AftersalesRefundQueueMessage(1L, 1L, 3L))).isTrue();
		InOrder inOrder = inOrder(sideEffects);
		inOrder.verify(sideEffects).updateRefundToSuccess(r);
		inOrder.verify(sideEffects).updateAftersaleFinished(r);
		verify(sideEffects, never()).doOnlineRefund(any());
		verify(sideEffects, never()).notifyTradeRefundSettled(any());
	}

	@Test
	@DisplayName("§3.2-6-1,6-3,6-4 original：doOnline（内含 Finish）+ 售后收尾，不再二次 notify")
	void afterSaleOriginal() {
		AftersalesRefund r = base(1L, 1L);
		r.setAftersalesBn(99L);
		r.setPayType("wxpay");
		r.setRefundChannel("original");
		when(aftersalesRefundMapper.selectOne(any(QueryWrapper.class))).thenReturn(r);
		when(sideEffects.applyOfflineChannelHintFromOrder(r)).thenReturn(r);
		assertThat(jobService.handle(new AftersalesRefundQueueMessage(1L, 1L, 3L))).isTrue();
		InOrder inOrder = inOrder(sideEffects);
		inOrder.verify(sideEffects).doOnlineRefund(r);
		inOrder.verify(sideEffects).updateAftersaleFinished(r);
		verify(sideEffects, never()).notifyTradeRefundSettled(any());
	}

	private static AftersalesRefund base(long refundBn, long companyId) {
		AftersalesRefund r = new AftersalesRefund();
		r.setRefundBn(refundBn);
		r.setCompanyId(companyId);
		r.setOrderId(3L);
		r.setRefundStatus("AUDIT_SUCCESS");
		return r;
	}
}
