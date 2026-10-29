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

package cn.shopex.ecshopx.aftersales.service.saaserp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundConfirmService;
import cn.shopex.ecshopx.aftersales.service.AftersalesReviewService;
import cn.shopex.ecshopx.common.port.order.OrderAssociationReadPort;
import cn.shopex.ecshopx.common.port.order.OrderConfirmCancelPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AftersalesSaasErpRefundStateAdapterTest {

	@Mock private AftersalesMapper aftersalesMapper;
	@Mock private AftersalesRefundMapper aftersalesRefundMapper;
	@Mock private AftersalesRefundConfirmService confirmService;
	@Mock private AftersalesReviewService reviewService;
	@Mock private OrderAssociationReadPort orderAssociationReadPort;
	@Mock private OrderConfirmCancelPort orderConfirmCancelPort;

	private AftersalesSaasErpRefundStateAdapter adapter;

	@BeforeAll
	static void initMybatisPlusMetadata() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
		TableInfoHelper.initTableInfo(assistant, Aftersales.class);
		TableInfoHelper.initTableInfo(assistant, AftersalesRefund.class);
	}

	@BeforeEach
	void setUp() {
		adapter =
				new AftersalesSaasErpRefundStateAdapter(
						aftersalesMapper,
						aftersalesRefundMapper,
						confirmService,
						reviewService,
						orderAssociationReadPort,
						orderConfirmCancelPort);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findRefund_positiveCompanyId_includesCompanyFilter() {
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(null);

		adapter.findRefund(5L, "100", "2001");

		ArgumentCaptor<LambdaQueryWrapper<AftersalesRefund>> captor =
				ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(aftersalesRefundMapper).selectOne(captor.capture());
		String sql = captor.getValue().getCustomSqlSegment();
		assertThat(sql).contains("company_id");
		assertThat(sql).contains("order_id");
		assertThat(sql).contains("refund_bn");
	}

	@Test
	@SuppressWarnings("unchecked")
	void findRefund_zeroCompanyId_omitsCompanyFilter() {
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(null);

		adapter.findRefund(0L, "100", "2001");

		ArgumentCaptor<LambdaQueryWrapper<AftersalesRefund>> captor =
				ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(aftersalesRefundMapper).selectOne(captor.capture());
		String sql = captor.getValue().getCustomSqlSegment();
		assertThat(sql).doesNotContain("company_id");
		assertThat(sql).contains("order_id");
	}

	@Test
	void confirmRefundGoods_refuse_usesOmsRejectMemo() {
		Map<String, Object> aftersales = Map.of("company_id", 1L, "aftersales_bn", 9L);
		Map<String, String> params = Map.of("order_id", "100");
		when(confirmService.confirmRefund(any())).thenReturn(Map.of("ok", true));

		adapter.confirmRefundGoods(aftersales, params, false);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(confirmService).confirmRefund(captor.capture());
		assertThat(captor.getValue().get("refund_memo")).isEqualTo("OMS拒绝退款");
		assertThat(captor.getValue().get("check_refund")).isEqualTo(0);
	}

	@Test
	void confirmRefundGoods_agree_missingMoney_setsFeeZeroAndOmsConfirmMemo() {
		Map<String, Object> aftersales = Map.of("company_id", 1L, "aftersales_bn", 9L);
		Map<String, String> params = Map.of("order_id", "100");
		when(confirmService.confirmRefund(any())).thenReturn(Map.of("ok", true));

		adapter.confirmRefundGoods(aftersales, params, true);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(confirmService).confirmRefund(captor.capture());
		assertThat(captor.getValue().get("refund_fee")).isEqualTo(0);
		assertThat(captor.getValue().get("refund_memo")).isEqualTo("OMS确认退款");
		assertThat(captor.getValue().get("check_refund")).isEqualTo(1);
	}

	@Test
	void reviewOnlyRefund_agree_callsReviewWithFeeAndMemo() {
		Map<String, Object> aftersales = Map.of("company_id", 1L, "aftersales_bn", 9L);
		Map<String, String> params = Map.of("money", "12.5");
		when(reviewService.review(any(), any())).thenReturn(Map.of("ok", true));

		adapter.reviewOnlyRefund(aftersales, params, true);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(reviewService).review(captor.capture(), any());
		assertThat(captor.getValue().get("is_approved")).isEqualTo(1);
		assertThat(captor.getValue().get("refund_point")).isEqualTo(0);
		assertThat(captor.getValue().get("refund_fee")).isEqualTo(1250);
		assertThat(captor.getValue().get("refund_memo")).isEqualTo("OMS确认退款");
	}

	@Test
	void findOrder_alwaysFiltersCompanyId() {
		when(orderAssociationReadPort.getAssociation(3L, 100L)).thenReturn(Optional.empty());

		adapter.findOrder(3L, "100");

		verify(orderAssociationReadPort).getAssociation(3L, 100L);
	}
}
