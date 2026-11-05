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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10905：修改线下凭证必须校验凭证归属的订单与租户。 */
@ExtendWith(MockitoExtension.class)
class WxappOfflinePaymentUpdateVoucherOrderOwnershipTest {

	@Mock
	private OfflinePaymentMapper offlinePaymentMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private OfflineBankAccountMapper offlineBankAccountMapper;

	@Mock
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	private WxappOfflinePaymentUpdateVoucherService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, OfflinePayment.class);
		TableInfoHelper.initTableInfo(assistant,
				cn.shopex.ecshopx.espier.domain.OfflineBankAccount.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new WxappOfflinePaymentUpdateVoucherService(
						offlinePaymentMapper,
						normalOrdersMapper,
						offlineBankAccountMapper,
						orderProcessLogPublishPort,
						new ObjectMapper());
	}

	private Map<String, Object> mergedFor(long paymentId, long orderId, long bankAccountId) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("id", paymentId);
		merged.put("order_id", orderId);
		merged.put("bank_account_id", bankAccountId);
		merged.put("voucher_pic", List.of("https://img.example/v2.jpg"));
		merged.put("transfer_remark", "note");
		return merged;
	}

	private static NormalOrders ownOrder(long companyId, long userId, long orderId) {
		NormalOrders order = new NormalOrders();
		order.setOrderId(orderId);
		order.setCompanyId(companyId);
		order.setUserId(userId);
		order.setOrderStatus("NOTPAY");
		order.setPayType("offline_pay");
		order.setSalesmanId(0L);
		order.setTotalFee("9900");
		order.setShopId(3L);
		order.setDistributorId(0L);
		order.setSupplierId(null);
		return order;
	}

	@Test
	void updateOfflineVoucher_rejectsVoucherOfAnotherOrder() {
		long companyId = 11L;
		long userId = 22L;
		long ownOrderId = 33_001L;

		OfflinePayment foreignVoucher = new OfflinePayment();
		foreignVoucher.setId(77L);
		foreignVoucher.setCompanyId(companyId);
		foreignVoucher.setOrderId(33_777L);
		foreignVoucher.setCheckStatus(2);
		when(offlinePaymentMapper.selectById(77L)).thenReturn(foreignVoucher);

		assertThatThrownBy(
						() ->
								service.updateOfflineVoucher(
										null,
										mergedFor(77L, ownOrderId, 5L),
										auth(companyId, userId)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("凭证不存在");

		verify(offlinePaymentMapper, never()).update(isNull(), any());
	}

	@Test
	void updateOfflineVoucher_rejectsVoucherOfAnotherCompany() {
		long companyId = 11L;
		long userId = 22L;
		long ownOrderId = 33_001L;

		OfflinePayment foreignVoucher = new OfflinePayment();
		foreignVoucher.setId(77L);
		foreignVoucher.setCompanyId(99L);
		foreignVoucher.setOrderId(ownOrderId);
		foreignVoucher.setCheckStatus(2);
		when(offlinePaymentMapper.selectById(77L)).thenReturn(foreignVoucher);

		assertThatThrownBy(
						() ->
								service.updateOfflineVoucher(
										null,
										mergedFor(77L, ownOrderId, 5L),
										auth(companyId, userId)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("凭证不存在");

		verify(offlinePaymentMapper, never()).update(isNull(), any());
	}

	@Test
	void updateOfflineVoucher_allowsVoucherOfSameOrderAndCompany() {
		long companyId = 11L;
		long userId = 22L;
		long orderId = 33_001L;
		NormalOrders order = ownOrder(companyId, userId, orderId);

		OfflinePayment before = new OfflinePayment();
		before.setId(77L);
		before.setCompanyId(companyId);
		before.setOrderId(orderId);
		before.setCheckStatus(2);
		when(offlinePaymentMapper.selectById(77L)).thenReturn(before, before);

		when(normalOrdersMapper.selectOne(any(Wrapper.class))).thenReturn(order);

		cn.shopex.ecshopx.espier.domain.OfflineBankAccount acc =
				new cn.shopex.ecshopx.espier.domain.OfflineBankAccount();
		acc.setId(5L);
		acc.setCompanyId(companyId);
		acc.setBankAccountName("acct");
		acc.setBankAccountNo("622200");
		acc.setBankName("ICBC");
		acc.setChinaUmsNo("");
		when(offlineBankAccountMapper.selectOne(any(Wrapper.class))).thenReturn(acc);

		when(offlinePaymentMapper.update(isNull(), any())).thenReturn(1);
		when(normalOrdersMapper.update(isNull(), any())).thenReturn(1);

		Map<String, Object> out = service.updateOfflineVoucher(null, mergedFor(77L, orderId, 5L), auth(companyId, userId));

		assertThat(out).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> row = (Map<String, Object>) out;
		assertThat(row.get("id")).isEqualTo(77L);
		verify(offlinePaymentMapper).update(isNull(), any());
	}

	private static Map<String, Object> auth(long companyId, long userId) {
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", companyId);
		auth.put("user_id", userId);
		return auth;
	}
}
