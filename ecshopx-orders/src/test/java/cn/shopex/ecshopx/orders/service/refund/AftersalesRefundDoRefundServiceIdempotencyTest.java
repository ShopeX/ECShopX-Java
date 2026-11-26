/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.orders.service.refund;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.EmployeePurchasePrepaidAftersalesRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AftersalesRefundDoRefundServiceIdempotencyTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, AftersalesRefund.class);
		TableInfoHelper.initTableInfo(assistant, Trade.class);
	}

	@Mock private OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService;
	@Mock private AftersalesRefundMapper aftersalesRefundMapper;
	@Mock private TradeMapper tradeMapper;
	@Mock private NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock private AftersalesDetailMapper aftersalesDetailMapper;
	@Mock private ApplicationEventPublisher applicationEventPublisher;
	@Mock private PointMemberAddPointService pointMemberAddPointService;
	@Mock private TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher;
	@Mock private OrderProcessLogPublishPort orderProcessLogPublishPort;
	@Mock private HfPayOrderApplyIdGenerator hfPayOrderApplyIdGenerator;
	@Mock private TradeRefundFinishEventDispatchPublisher tradeRefundFinishEventDispatchPublisher;
	@Mock private RefundErrorLogsRecorder refundErrorLogsRecorder;
	@Mock
	private ObjectProvider<EmployeePurchasePrepaidAftersalesRestorePort>
			employeePurchasePrepaidAftersalesRestorePort;
	@Mock private EmployeePurchasePrepaidRefundLinesResolver employeePurchasePrepaidRefundLinesResolver;

	private AftersalesRefundDoRefundService service;

	@BeforeEach
	void setUp() {
		service =
				new AftersalesRefundDoRefundService(
						ordersRefundPaymentDispatchService,
						aftersalesRefundMapper,
						tradeMapper,
						normalOrdersItemsMapper,
						aftersalesDetailMapper,
						applicationEventPublisher,
						pointMemberAddPointService,
						tradeRefundStatisticsJobDispatchPublisher,
						orderProcessLogPublishPort,
						hfPayOrderApplyIdGenerator,
						tradeRefundFinishEventDispatchPublisher,
						refundErrorLogsRecorder,
						employeePurchasePrepaidAftersalesRestorePort,
						employeePurchasePrepaidRefundLinesResolver);
	}

	@Test
	@DisplayName("SUCCESS 更新 WHERE 须排除已 SUCCESS")
	void doRefund_successUpdateRequiresNotAlreadySuccess() {
		stubRefundAndTrade(50);
		Map<String, Object> payRes = new LinkedHashMap<>();
		payRes.put("status", "SUCCESS");
		payRes.put("refund_id", "r1");
		when(ordersRefundPaymentDispatchService.dispatch(
						anyLong(), any(), any(), any(), anyInt(), anyInt(), eq(false)))
				.thenReturn(payRes);
		when(aftersalesRefundMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		assertThatThrownBy(() -> service.doRefund(1L, 42L, false))
				.isInstanceOf(ResourceException.class);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaUpdateWrapper<AftersalesRefund>> captor =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(aftersalesRefundMapper).update(isNull(), captor.capture());
		LambdaUpdateWrapper<AftersalesRefund> u = captor.getValue();
		assertThat(u.getSqlSegment()).containsIgnoringCase("refund_status");
		assertThat(u.getSqlSet()).containsIgnoringCase("refund_status");
		assertThat(u.getExpression().getNormal().size()).isGreaterThanOrEqualTo(3);
	}

	@Test
	@DisplayName("抢占失败时不退积分")
	void doRefund_whenClaimAffectedZero_doesNotAddPoints() {
		stubRefundAndTrade(50);
		Map<String, Object> payRes = new LinkedHashMap<>();
		payRes.put("status", "SUCCESS");
		payRes.put("refund_id", "r1");
		when(ordersRefundPaymentDispatchService.dispatch(
						anyLong(), any(), any(), any(), anyInt(), anyInt(), eq(false)))
				.thenReturn(payRes);
		when(aftersalesRefundMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		assertThatThrownBy(() -> service.doRefund(1L, 42L, false))
				.isInstanceOf(ResourceException.class);

		verify(pointMemberAddPointService, never())
				.addPointForAftersalesRefund(anyLong(), anyLong(), anyInt(), anyLong(), anyLong(), any());
		verify(tradeRefundStatisticsJobDispatchPublisher, never()).publish(any());
	}

	private void stubRefundAndTrade(int refundPoint) {
		AftersalesRefund refund = new AftersalesRefund();
		refund.setCompanyId(1L);
		refund.setRefundBn(42L);
		refund.setOrderId(9001L);
		refund.setTradeId("tr-1");
		refund.setPayType("wxpay");
		refund.setRefundFee(100);
		refund.setFreightType("cash");
		refund.setFreight(0);
		refund.setRefundPoint(refundPoint);
		refund.setUserId(3L);
		refund.setAftersalesBn(8L);
		refund.setRefundStatus("AUDIT_SUCCESS");
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(refund);

		Trade trade = new Trade();
		trade.setCompanyId("1");
		trade.setTradeId("tr-1");
		trade.setTradeState("SUCCESS");
		trade.setPayFee(200);
		trade.setWxaAppid("wx");
		when(tradeMapper.selectOne(any())).thenReturn(trade);
	}
}
