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

package cn.shopex.ecshopx.orders.service.normal.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderProfitByOrderResultPort;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelSupplierMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderPromotionsMapper;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/** ECX-11443: order_id PK collision must regenerate and retry insert. */
@ExtendWith(MockitoExtension.class)
class OrderCreatePersistencePortImplDuplicateKeyRetryTest {

	@Mock private NormalOrdersMapper normalOrdersMapper;
	@Mock private NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock private OrderPromotionsMapper orderPromotionsMapper;
	@Mock private OrderAssociationsMapper orderAssociationsMapper;
	@Mock private NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper;
	@Mock private NormalOrderNumericIdService normalOrderNumericIdService;
	@Mock private OrderProfitByOrderResultPort orderProfitByOrderResultPort;
	@Mock private CompanyDefaultCurrencyService companyDefaultCurrencyService;

	private OrderCreatePersistencePortImpl port;

	@BeforeEach
	void setUp() {
		port =
				new OrderCreatePersistencePortImpl(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderPromotionsMapper,
						orderAssociationsMapper,
						normalOrdersRelSupplierMapper,
						normalOrderNumericIdService,
						orderProfitByOrderResultPort,
						companyDefaultCurrencyService,
						new ObjectMapper());
		CurrencyExchangeRate cny = new CurrencyExchangeRate();
		cny.setCurrency("CNY");
		cny.setRate(1.0);
		cny.setSymbol("￥");
		when(companyDefaultCurrencyService.getCur(141L)).thenReturn(cny);
	}

	@Test
	@DisplayName("preassigned order_id collision regenerates and persists the new id")
	void persistOrder_duplicateKeyOnPreassigned_retriesWithGeneratedId() {
		when(normalOrderNumericIdService.generate(7L)).thenReturn(99002L);
		List<Long> insertedIds = stubInsertRecordingIds(/* failFirst */ true);

		NormalOrderCreateParams p = minimalParams();
		p.getOrderData().put("order_id", 88001L);

		port.persistOrder(p);

		verify(normalOrdersMapper, times(2)).insert(any(NormalOrders.class));
		assertThat(insertedIds).containsExactly(88001L, 99002L);
		assertThat(p.getOrderData().get("order_id")).isEqualTo(99002L);
		assertThat(p.getOrdersInsertResult().get("order_id")).isEqualTo(99002L);
		verify(normalOrderNumericIdService, times(1)).generate(7L);
	}

	@Test
	@DisplayName("generated order_id collision regenerates and persists the new id")
	void persistOrder_duplicateKeyOnGenerated_retriesWithNextId() {
		when(normalOrderNumericIdService.generate(7L)).thenReturn(111L, 222L);
		List<Long> insertedIds = stubInsertRecordingIds(/* failFirst */ true);

		NormalOrderCreateParams p = minimalParams();

		port.persistOrder(p);

		verify(normalOrdersMapper, times(2)).insert(any(NormalOrders.class));
		assertThat(insertedIds).containsExactly(111L, 222L);
		verify(normalOrderNumericIdService, times(2)).generate(7L);
	}

	/** Records order_id at each insert call (before mutation); first call throws DuplicateKey. */
	private List<Long> stubInsertRecordingIds(boolean failFirst) {
		List<Long> insertedIds = new ArrayList<>();
		when(normalOrdersMapper.insert(any(NormalOrders.class)))
				.thenAnswer(
						inv -> {
							NormalOrders row = inv.getArgument(0);
							insertedIds.add(row.getOrderId());
							if (failFirst && insertedIds.size() == 1) {
								throw new DuplicateKeyException("uk_order_id");
							}
							return 1;
						});
		return insertedIds;
	}

	private static NormalOrderCreateParams minimalParams() {
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		Map<String, Object> od = p.getOrderData();
		od.put("company_id", 141L);
		od.put("user_id", 7L);
		od.put("mobile", "13800000000");
		od.put("distributor_id", 104L);
		od.put("shop_id", 0L);
		od.put("order_type", "normal");
		od.put("order_class", "normal");
		od.put("receipt_type", "logistics");
		od.put("order_source", "member");
		od.put("title", "sku");
		od.put("total_fee", 100);
		od.put("item_fee", 100);
		od.put("pay_type", "wxpay");
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", 11L);
		item.put("goods_id", 11L);
		item.put("item_name", "sku");
		item.put("num", 1);
		item.put("price", 100);
		item.put("total_fee", 100);
		item.put("item_fee", 100);
		item.put("distributor_id", 104L);
		item.put("shop_id", 0L);
		od.put("items", List.of(item));
		return p;
	}
}
