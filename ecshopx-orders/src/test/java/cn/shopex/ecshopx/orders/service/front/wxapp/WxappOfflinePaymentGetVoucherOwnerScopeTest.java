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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.espier.mapper.OfflineBankAccountMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10877：获取线下凭证必须校验订单买家。 */
@ExtendWith(MockitoExtension.class)
class WxappOfflinePaymentGetVoucherOwnerScopeTest {

	@Mock
	private OfflinePaymentMapper offlinePaymentMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private OfflineBankAccountMapper offlineBankAccountMapper;

	@Mock
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	private WxappOfflinePaymentUploadVoucherService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, OfflinePayment.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new WxappOfflinePaymentUploadVoucherService(
						offlinePaymentMapper,
						normalOrdersMapper,
						offlineBankAccountMapper,
						orderProcessLogPublishPort,
						new ObjectMapper());
	}

	@Test
	void getOfflineVoucher_returnsEmptyWhenOrderOwnedBySomeoneElse() {
		NormalOrders foreign = new NormalOrders();
		foreign.setOrderId(55L);
		foreign.setCompanyId(100L);
		foreign.setUserId(9L);
		when(normalOrdersMapper.selectOne(any(Wrapper.class))).thenReturn(foreign);

		Object out = service.getOfflineVoucher(100L, 7L, "55");

		assertThat(out).isEqualTo(Collections.emptyList());
		verify(offlinePaymentMapper, never()).selectOne(any(Wrapper.class));
	}

	@Test
	void getOfflineVoucher_returnsRowForOrderOwner() {
		NormalOrders own = new NormalOrders();
		own.setOrderId(55L);
		own.setCompanyId(100L);
		own.setUserId(7L);
		when(normalOrdersMapper.selectOne(any(Wrapper.class))).thenReturn(own);

		OfflinePayment pay = new OfflinePayment();
		pay.setId(1L);
		pay.setCompanyId(100L);
		pay.setOrderId(55L);
		pay.setUserId(7L);
		pay.setVoucherPic("[\"http://cdn/a.png\"]");
		when(offlinePaymentMapper.selectOne(any(Wrapper.class))).thenReturn(pay);

		Object out = service.getOfflineVoucher(100L, 7L, "55");

		assertThat(out).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> row = (Map<String, Object>) out;
		assertThat(row.get("order_id")).isEqualTo(55L);
		assertThat(row.get("voucher_pic")).isInstanceOf(List.class);
	}
}
