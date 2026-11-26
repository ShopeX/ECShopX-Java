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

package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesOfflineRefund;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesOfflineRefundMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundOnlineRefundPort;
import cn.shopex.ecshopx.common.port.order.EmployeePurchasePrepaidAftersalesRestorePort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
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

@ExtendWith(MockitoExtension.class)
class AftersalesOfflineRefundSaveServiceIdempotencyTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, AftersalesRefund.class);
		TableInfoHelper.initTableInfo(assistant, Aftersales.class);
		TableInfoHelper.initTableInfo(assistant, AftersalesDetail.class);
		TableInfoHelper.initTableInfo(assistant, AftersalesOfflineRefund.class);
	}

	@Mock private AftersalesRefundMapper aftersalesRefundMapper;
	@Mock private AftersalesOfflineRefundMapper aftersalesOfflineRefundMapper;
	@Mock private AftersalesMapper aftersalesMapper;
	@Mock private AftersalesDetailMapper aftersalesDetailMapper;
	@Mock private TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher;
	@Mock
	private ObjectProvider<EmployeePurchasePrepaidAftersalesRestorePort>
			employeePurchasePrepaidAftersalesRestorePort;
	@Mock private EmployeePurchasePrepaidRefundLinesResolver employeePurchasePrepaidRefundLinesResolver;
	@Mock private AftersalesRefundOnlineRefundPort aftersalesRefundOnlineRefundPort;

	private AftersalesOfflineRefundSaveService service;

	@BeforeEach
	void setUp() {
		service =
				new AftersalesOfflineRefundSaveService(
						aftersalesRefundMapper,
						aftersalesOfflineRefundMapper,
						aftersalesMapper,
						aftersalesDetailMapper,
						tradeRefundStatisticsJobDispatchPublisher,
						employeePurchasePrepaidAftersalesRestorePort,
						employeePurchasePrepaidRefundLinesResolver,
						aftersalesRefundOnlineRefundPort);
	}

	@Test
	@DisplayName("置 SUCCESS 的 WHERE 须排除已 SUCCESS")
	void save_claimUpdateRequiresNotSuccess() {
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(refundRow());
		when(aftersalesOfflineRefundMapper.insert(any(AftersalesOfflineRefund.class))).thenReturn(1);
		when(aftersalesRefundMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		assertThatThrownBy(() -> service.saveOfflineRefund(validBody(), 1L))
				.isInstanceOf(ResourceException.class);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaUpdateWrapper<AftersalesRefund>> captor =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(aftersalesRefundMapper).update(isNull(), captor.capture());
		LambdaUpdateWrapper<AftersalesRefund> uw = captor.getValue();
		assertThat(uw.getSqlSegment()).containsIgnoringCase("refund_status");
		assertThat(uw.getSqlSet()).containsIgnoringCase("refund_status");
		assertThat(uw.getParamNameValuePairs().values()).contains("SUCCESS");
		assertThat(uw.getExpression().getNormal().size()).isGreaterThanOrEqualTo(2);
	}

	@Test
	@DisplayName("抢占影响行数为 0 时不发统计且不推进售后")
	void save_whenClaimAffectedZero_skipsSideEffects() {
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(refundRow());
		when(aftersalesOfflineRefundMapper.insert(any(AftersalesOfflineRefund.class))).thenReturn(1);
		when(aftersalesRefundMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		assertThatThrownBy(() -> service.saveOfflineRefund(validBody(), 1L))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("已处理");

		verify(tradeRefundStatisticsJobDispatchPublisher, never()).publish(any());
		verify(aftersalesMapper, never()).update(isNull(), any());
		verify(aftersalesDetailMapper, never()).update(isNull(), any());
	}

	private static AftersalesRefund refundRow() {
		AftersalesRefund row = new AftersalesRefund();
		row.setRefundBn(9001L);
		row.setCompanyId(1L);
		row.setOrderId(100L);
		row.setPayType("offline_pay");
		row.setRefundPoint(0);
		row.setRefundFee(100);
		row.setRefundStatus("AUDIT_SUCCESS");
		row.setAftersalesBn(8001L);
		row.setTradeId("T1");
		return row;
	}

	private static LinkedHashMap<String, Object> validBody() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("refund_bn", "9001");
		m.put("bank_account_name", "收款人");
		m.put("bank_account_no", "6222");
		m.put("bank_name", "工行");
		m.put("refund_account_name", "退款人");
		m.put("refund_account_bank", "6223");
		m.put("refund_account_no", "建行");
		return m;
	}
}
