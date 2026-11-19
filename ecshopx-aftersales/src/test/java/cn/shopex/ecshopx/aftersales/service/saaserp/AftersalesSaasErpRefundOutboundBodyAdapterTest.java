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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.openapi.OpenapiCancelOrderReasonPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpNormalOrderLookupPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AftersalesSaasErpRefundOutboundBodyAdapterTest {

	@Mock private AftersalesMapper aftersalesMapper;
	@Mock private AftersalesRefundMapper aftersalesRefundMapper;
	@Mock private SaasErpNormalOrderLookupPort orderLookupPort;
	@Mock private OpenapiCancelOrderReasonPort cancelOrderReasonPort;

	private AftersalesSaasErpRefundOutboundBodyAdapter adapter;

	@BeforeAll
	static void initMybatisPlusMetadata() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
		TableInfoHelper.initTableInfo(assistant, AftersalesRefund.class);
	}

	@BeforeEach
	void setUp() {
		adapter =
				new AftersalesSaasErpRefundOutboundBodyAdapter(
						aftersalesMapper,
						aftersalesRefundMapper,
						orderLookupPort,
						cancelOrderReasonPort);
	}

	@Test
	void refundAddBody_usesOrderUserIdAsBuyerId() {
		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundBn(2001L);
		refund.setOrderId(100L);
		refund.setCompanyId(7L);
		refund.setUserId(999L);
		refund.setRefundFee(100);
		refund.setCurrency("CNY");
		refund.setPayType("wxpay");
		refund.setCurPayFee("100");
		refund.setCreateTime(1_700_000_000);
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(refund);
		Map<String, Object> order = new LinkedHashMap<>();
		order.put("order_id", 100L);
		order.put("company_id", 7L);
		order.put("user_id", 42L);
		when(orderLookupPort.find(eq(7L), eq("100"))).thenReturn(Optional.of(order));
		when(cancelOrderReasonPort.findCancelReason(7L, 100L)).thenReturn("cancel reason");

		Map<String, Object> body = adapter.refundAddBody("2001", 7L, "100", null);

		assertThat(body.get("buyer_id")).isEqualTo(42L);
		assertThat(refund.getUserId()).isEqualTo(999L);
	}
}
