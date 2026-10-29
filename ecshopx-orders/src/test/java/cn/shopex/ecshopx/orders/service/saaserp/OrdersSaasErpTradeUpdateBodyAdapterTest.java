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

package cn.shopex.ecshopx.orders.service.saaserp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OrdersSaasErpTradeUpdateBodyAdapterTest {

	@Mock private OrdersSaasErpOrderBundleReader orderReader;
	@Mock private MembersMapper membersMapper;
	@Mock private MembersInfoMapper membersInfoMapper;
	@Mock private StringRedisTemplate companysRedisTemplate;
	@Mock private ValueOperations<String, String> valueOperations;

	private OrdersSaasErpOrderStructAdapter structAdapter;
	private OrdersSaasErpTradeUpdateBodyAdapter adapter;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Members.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), MembersInfo.class);
	}

	@BeforeEach
	void setUp() {
		org.mockito.Mockito.lenient().when(companysRedisTemplate.opsForValue()).thenReturn(valueOperations);
		structAdapter =
				new OrdersSaasErpOrderStructAdapter(
						orderReader, membersMapper, membersInfoMapper, companysRedisTemplate);
		adapter = new OrdersSaasErpTradeUpdateBodyAdapter(orderReader, structAdapter);
	}

	@Test
	void missingOrder_isEmpty() {
		when(orderReader.read(1L, "100", "normal")).thenReturn(Optional.empty());

		assertThat(adapter.tradeUpdateBody(1L, "100", "normal")).isEmpty();
	}

	@Test
	void notPay_isEmpty() {
		when(orderReader.read(1L, "100", "normal"))
				.thenReturn(Optional.of(bundle(order("NOTPAY", "logistics"), trade(), Map.of())));

		assertThat(adapter.tradeUpdateBody(1L, "100", "normal")).isEmpty();
	}

	@Test
	void waitBuyerConfirm_isEmpty() {
		when(orderReader.read(1L, "100", "normal"))
				.thenReturn(
						Optional.of(bundle(order("WAIT_BUYER_CONFIRM", "logistics"), trade(), Map.of())));

		assertThat(adapter.tradeUpdateBody(1L, "100", "normal")).isEmpty();
	}

	@Test
	void zitiWithoutRedisFlag_isEmpty() {
		when(valueOperations.get("sendOmsSetting:1")).thenReturn(null);
		when(orderReader.read(1L, "100", "normal"))
				.thenReturn(Optional.of(bundle(order("PAYED", "ziti"), trade(), Map.of())));

		assertThat(adapter.tradeUpdateBody(1L, "100", "normal")).isEmpty();
	}

	@Test
	void localPay_isEmpty() {
		Map<String, Object> trade = trade();
		trade.put("payType", "localPay");
		when(orderReader.read(1L, "100", "normal"))
				.thenReturn(Optional.of(bundle(order("PAYED", "logistics"), trade, Map.of())));

		assertThat(adapter.tradeUpdateBody(1L, "100", "normal")).isEmpty();
	}

	@Test
	void payableOrder_includesTid() {
		when(valueOperations.get("sendOmsSetting:7")).thenReturn("{}");
		Map<String, Object> order = order("PAYED", "logistics");
		order.put("order_id", 88001L);
		order.put("company_id", 7L);
		Map<String, Object> promo = new LinkedHashMap<>();
		promo.put("discount_fee", 150);
		promo.put("rule", "满减");
		order.put("discount_info", List.of(promo));
		when(orderReader.read(7L, "88001", "normal"))
				.thenReturn(Optional.of(bundle(order, trade(), distributor())));

		Map<String, Object> body = adapter.tradeUpdateBody(7L, "88001", "normal").orElseThrow();

		assertThat(body.get("tid")).isEqualTo(88001L);
	}

	private static Map<String, Object> bundle(
			Map<String, Object> orderInfo, Map<String, Object> tradeInfo, Map<String, Object> distributor) {
		Map<String, Object> bundle = new LinkedHashMap<>();
		bundle.put("orderInfo", orderInfo);
		bundle.put("tradeInfo", tradeInfo);
		bundle.put("distributor", distributor);
		return bundle;
	}

	private static Map<String, Object> order(String status, String receiptType) {
		Map<String, Object> order = new LinkedHashMap<>();
		order.put("order_id", 100L);
		order.put("company_id", 1L);
		order.put("user_id", 0L);
		order.put("order_status", status);
		order.put("receipt_type", receiptType);
		order.put("ziti_status", "PENDING");
		order.put("delivery_status", "PENDING");
		order.put("pay_type", "wxpay");
		order.put("update_time", 1_700_000_100);
		order.put("create_time", 1_700_000_000);
		order.put("item_fee", 2000);
		order.put("total_fee", 1800);
		order.put("freight_fee", 250);
		order.put("fee_rate", 1.25f);
		order.put("discount_fee", 100);
		order.put("point_fee", 50);
		order.put("receiver_name", "张三");
		order.put("receiver_mobile", "13900000000");
		order.put("receiver_state", "上海");
		order.put("receiver_city", "上海市");
		order.put("receiver_district", "徐汇");
		order.put("receiver_address", "宜山路");
		order.put("receiver_zip", "200000");
		order.put("delivery_corp", "SF");
		order.put("remark", "请尽快发货");
		order.put("pack", "{\"packDes\":\"礼盒\"}");
		order.put("invoice", Map.of("content", " 抬头 "));
		order.put("discount_info", List.of());
		order.put("items", List.of(item()));
		return order;
	}

	private static Map<String, Object> trade() {
		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("payType", "wxpay");
		trade.put("payFee", 1235);
		trade.put("trade_id", "T1");
		return trade;
	}

	private static Map<String, Object> distributor() {
		Map<String, Object> distributor = new LinkedHashMap<>();
		distributor.put("shop_code", "SHOP1");
		distributor.put("name", "门店A");
		return distributor;
	}

	private static Map<String, Object> item() {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", 1L);
		item.put("item_bn", "SKU-1");
		item.put("num", 1);
		item.put("price", 1000);
		item.put("item_fee", 1000);
		item.put("total_fee", 1000);
		item.put("item_name", "商品");
		return item;
	}
}
