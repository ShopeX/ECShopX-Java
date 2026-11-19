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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OrdersSaasErpOrderStructAdapterTest {

	private static final DateTimeFormatter PHP_DATE =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	/** Keys assigned in PHP {@code $erpOrderStruct} at OrderService.php lines 201–250. */
	private static final List<String> ERP_ORDER_STRUCT_KEYS =
			List.of(
					"tid",
					"lastmodify",
					"promotion_details",
					"total_weight",
					"created",
					"payment_lists",
					"status",
					"pay_status",
					"ship_status",
					"payed_fee",
					"total_goods_fee",
					"total_trade_fee",
					"currency",
					"currency_rate",
					"buyer_obtain_point_fee",
					"is_protect",
					"protect_fee",
					"discount_fee",
					"is_cod",
					"payment_tid",
					"payment_type",
					"orders_number",
					"buyer_uname",
					"buyer_name",
					"buyer_mobile",
					"buyer_phone",
					"buyer_email",
					"buyer_state",
					"receiver_name",
					"receiver_phone",
					"receiver_mobile",
					"receiver_state",
					"receiver_city",
					"receiver_district",
					"receiver_address",
					"receiver_zip",
					"orders_discount_fee",
					"orders",
					"goods_discount_fee",
					"shipping_tid",
					"shipping_type",
					"shipping_fee",
					"has_invoice",
					"invoice_title",
					"invoice_fee",
					"pay_cost",
					"buyer_memo",
					"trade_memo");

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Mock private OrdersSaasErpOrderBundleReader orderReader;
	@Mock private MembersMapper membersMapper;
	@Mock private MembersInfoMapper membersInfoMapper;
	@Mock private StringRedisTemplate companysRedisTemplate;
	@Mock private ValueOperations<String, String> valueOperations;
	@Mock private NormalOrdersMapper normalOrdersMapper;

	private OrdersSaasErpOrderStructAdapter adapter;
	private OrdersSaasErpNormalOrderLookupAdapter lookupAdapter;

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
		adapter =
				new OrdersSaasErpOrderStructAdapter(
						orderReader, membersMapper, membersInfoMapper, companysRedisTemplate);
		lookupAdapter = new OrdersSaasErpNormalOrderLookupAdapter(normalOrdersMapper);
	}

	@Test
	void missingOrder_returnsEmpty() {
		when(orderReader.read(1L, "100", "normal")).thenReturn(Optional.empty());

		assertThat(adapter.getOrderStruct(1L, "100", "normal")).isEmpty();
	}

	@Test
	void notPayStatus_returnsEmpty() {
		when(orderReader.read(1L, "100", "normal")).thenReturn(Optional.of(bundle(order("NOTPAY", "logistics"), Map.of(), Map.of())));

		assertThat(adapter.getOrderStruct(1L, "100", "normal")).isEmpty();
	}

	@Test
	void zitiWithoutRedisFlag_returnsEmpty() {
		when(valueOperations.get("sendOmsSetting:1")).thenReturn(null);
		when(orderReader.read(1L, "100", "normal"))
				.thenReturn(Optional.of(bundle(order("PAYED", "ziti"), trade(), Map.of())));

		assertThat(adapter.getOrderStruct(1L, "100", "normal")).isEmpty();
	}

	@Test
	void zitiWithSendOmsFlag_isNotEmptySolelyBecauseZiti() {
		when(valueOperations.get("sendOmsSetting:1")).thenReturn("{\"ziti_send_oms\":true}");
		Map<String, Object> order = order("PAYED", "ziti");
		order.put("ziti_status", "DONE");
		when(orderReader.read(1L, "100", "normal"))
				.thenReturn(Optional.of(bundle(order, trade(), distributor())));

		Optional<Map<String, Object>> struct = adapter.getOrderStruct(1L, "100", "normal");

		assertThat(struct).isPresent();
		assertThat(struct.get().get("shipping_type")).isEqualTo("STORE_SELF_FETCH");
		assertThat(struct.get().get("receiver_name")).isEqualTo("");
		assertThat(struct.get().get("ship_status")).isEqualTo("SHIP_FINISH");
		assertThat(struct.get()).doesNotContainKey("delivery_corp");
	}

	@Test
	void localPay_returnsEmpty() {
		Map<String, Object> trade = trade();
		trade.put("payType", "localPay");
		when(orderReader.read(1L, "100", "normal"))
				.thenReturn(Optional.of(bundle(order("PAYED", "logistics"), trade, Map.of())));

		assertThat(adapter.getOrderStruct(1L, "100", "normal")).isEmpty();
	}

	@Test
	void happyPath_includesEveryPhpStructKeyAndJsonPromotionDetails() throws Exception {
		when(valueOperations.get("sendOmsSetting:7")).thenReturn("{}");
		Map<String, Object> order = happyOrder();
		when(orderReader.read(7L, "88001", "normal"))
				.thenReturn(Optional.of(bundle(order, happyTrade(), distributor())));

		Map<String, Object> struct = adapter.getOrderStruct(7L, "88001", "normal").orElseThrow();

		assertThat(struct.keySet()).containsAll(ERP_ORDER_STRUCT_KEYS);
		assertThat(struct.get("promotion_details")).isInstanceOf(String.class);
		JsonNode promotions = objectMapper.readTree((String) struct.get("promotion_details"));
		assertThat(promotions.isArray()).isTrue();
		assertThat(promotions.get(0).get("pmt_amount").asText()).isEqualTo("1.50");
		assertThat(promotions.get(0).get("pmt_id").asText()).isEqualTo("88001");
		assertThat(promotions.get(0).get("pmt_describe").asText()).isEqualTo("满减");
		assertThat((String) struct.get("promotion_details")).doesNotContain("promotiondetail");

		assertThat(struct.get("tid")).isEqualTo(88001L);
		assertThat(struct.get("created")).isEqualTo(PHP_DATE.format(Instant.ofEpochSecond(1_700_000_000L)));
		assertThat(struct.get("lastmodify")).isEqualTo(PHP_DATE.format(Instant.ofEpochSecond(1_700_000_100L)));
		assertThat(struct.get("status")).isEqualTo("TRADE_ACTIVE");
		assertThat(struct.get("pay_status")).isEqualTo("PAY_FINISH");
		assertThat(struct.get("ship_status")).isEqualTo("SHIP_NO");
		assertThat(struct.get("payed_fee")).isEqualTo("12.35");
		assertThat(struct.get("total_goods_fee")).isEqualTo("20.00");
		assertThat(struct.get("total_trade_fee")).isEqualTo("18.00");
		assertThat(struct.get("shipping_fee")).isEqualTo("2.50");
		assertThat(struct.get("orders_discount_fee")).isEqualTo("1.50");
		assertThat(struct.get("discount_fee")).isEqualTo(0);
		assertThat(struct.get("currency")).isEqualTo("CNY");
		assertThat(struct.get("currency_rate")).isEqualTo(1.25f);
		assertThat(struct.get("payment_tid")).isEqualTo("wxpay");
		assertThat(struct.get("payment_type")).isEqualTo("微信支付");
		assertThat(struct.get("buyer_name")).isEqualTo("匿名用户");
		assertThat(struct.get("buyer_uname")).isEqualTo("");
		assertThat(struct.get("buyer_email")).isEqualTo("");
		assertThat(struct.get("receiver_name")).isEqualTo("张三");
		assertThat(struct.get("has_invoice")).isEqualTo(1);
		assertThat(struct.get("invoice_title")).isEqualTo("抬头");
		assertThat(struct.get("invoice_fee")).isEqualTo("0.00");
		assertThat(struct.get("trade_memo")).isEqualTo("礼盒");
		assertThat(struct.get("buyer_memo")).isEqualTo("请尽快发货");
		assertThat(struct.get("shipping_type")).isEqualTo("SF");
		assertThat(struct.get("orders_number")).isEqualTo(1);
		assertThat(struct.get("total_weight")).isEqualTo("0.000");
		assertThat(struct.get("is_cod")).isEqualTo("false");

		JsonNode orders = objectMapper.readTree((String) struct.get("orders"));
		assertThat(orders.get("order").get(0).get("bn").asText()).isEqualTo("SKU-1");
		assertThat(orders.get("order").get(0).get("order_items").get("item").get(0).get("price").asText())
				.isEqualTo("10.00");
		assertThat(orders.get("order").get(0).get("store_code").asText()).isEqualTo("SHOP1");

		JsonNode payments = objectMapper.readTree((String) struct.get("payment_lists"));
		assertThat(payments.get("payment_list").get(0).get("pay_fee").asText()).isEqualTo("12.35");
		assertThat(payments.get("payment_list").get(0).get("payment_name").asText()).isEqualTo("微信支付");

		JsonNode o2o = objectMapper.readTree((String) struct.get("o2o_info"));
		assertThat(o2o.get("o2o_store_bn").asText()).isEqualTo("SHOP1");
		assertThat(o2o.get("o2o_store_name").asText()).isEqualTo("门店A");

		assertThat(OrdersSaasErpOrderStructAdapter.ALLOW_PARAMS).contains("invoice_desc   ");
	}

	@Test
	void dhpoint_setsTradeFeeFromItemPlusFreightAndCapsPayed() {
		Map<String, Object> trade = trade();
		trade.put("payType", "dhpoint");
		trade.put("payFee", 999999);
		Map<String, Object> order = order("PAYED", "logistics");
		order.put("item_fee", 1000);
		order.put("freight_fee", 200);
		order.put("total_fee", 50);
		when(orderReader.read(1L, "100", "normal"))
				.thenReturn(Optional.of(bundle(order, trade, Map.of())));

		Map<String, Object> struct = adapter.getOrderStruct(1L, "100", "normal").orElseThrow();

		assertThat(struct.get("total_trade_fee")).isEqualTo("12.00");
		assertThat(struct.get("payed_fee")).isEqualTo("12.00");
		assertThat(struct.get("payment_type")).isEqualTo("积分");
		assertThat(struct.get("shipping_type")).isEqualTo("SF");
	}

	@Test
	void payedFeeAboveTradeFee_isCapped() {
		Map<String, Object> trade = trade();
		trade.put("payFee", 5000);
		Map<String, Object> order = order("DONE", "logistics");
		order.put("total_fee", 1000);
		order.put("delivery_status", "DONE");
		when(orderReader.read(1L, "100", "normal"))
				.thenReturn(Optional.of(bundle(order, trade, Map.of())));

		Map<String, Object> struct = adapter.getOrderStruct(1L, "100", "normal").orElseThrow();

		assertThat(struct.get("status")).isEqualTo("TRADE_FINISHED");
		assertThat(struct.get("ship_status")).isEqualTo("SHIP_FINISH");
		assertThat(struct.get("total_trade_fee")).isEqualTo("10.00");
		assertThat(struct.get("payed_fee")).isEqualTo("10.00");
	}

	@Test
	void namedMember_usesMobileAndTrimmedUsername() {
		Map<String, Object> order = order("CANCEL", "dada");
		order.put("user_id", 9L);
		order.put("company_id", 1L);
		when(orderReader.read(1L, "100", "normal_shopguide"))
				.thenReturn(Optional.of(bundle(order, trade(), Map.of())));
		Members member = new Members();
		member.setUserId(9L);
		member.setCompanyId(1L);
		member.setMobile("13800000000");
		when(membersMapper.selectOne(any())).thenReturn(member);
		MembersInfo info = new MembersInfo();
		info.setUsername("  李四  ");
		when(membersInfoMapper.selectOne(any())).thenReturn(info);

		Map<String, Object> struct = adapter.getOrderStruct(1L, "100", "normal_shopguide").orElseThrow();

		assertThat(struct.get("status")).isEqualTo("TRADE_CLOSED");
		assertThat(struct.get("pay_status")).isEqualTo("REFUND_ALL");
		assertThat(struct.get("shipping_type")).isEqualTo("STORE_TONGCHEN_EXPRESS");
		assertThat(struct.get("buyer_uname")).isEqualTo("13800000000");
		assertThat(struct.get("buyer_name")).isEqualTo("李四");
		assertThat(struct.get("buyer_mobile")).isEqualTo("13800000000");
	}

	@Test
	void missingMember_returnsEmpty() {
		Map<String, Object> order = order("PAYED", "logistics");
		order.put("user_id", 9L);
		when(orderReader.read(1L, "100", "normal")).thenReturn(Optional.of(bundle(order, trade(), Map.of())));
		when(membersMapper.selectOne(any())).thenReturn(null);

		assertThat(adapter.getOrderStruct(1L, "100", "normal")).isEmpty();
	}

	@Test
	void lookup_missingRow_isEmpty() {
		when(normalOrdersMapper.selectOne(any())).thenReturn(null);

		assertThat(lookupAdapter.find(1L, "100")).isEmpty();
	}

	@Test
	void lookup_returnsOrderClass() {
		NormalOrders row = new NormalOrders();
		row.setOrderId(100L);
		row.setCompanyId(1L);
		row.setOrderClass("seckill");
		when(normalOrdersMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> found = lookupAdapter.find(1L, "100").orElseThrow();

		assertThat(found.get("order_class")).isEqualTo("seckill");
		assertThat(found.get("order_id")).isEqualTo(100L);
	}

	@Test
	@SuppressWarnings("unchecked")
	void lookup_addsCompanyIdOnlyWhenPositive() {
		when(normalOrdersMapper.selectOne(any())).thenReturn(null);

		lookupAdapter.find(0L, "100");
		ArgumentCaptor<LambdaQueryWrapper<NormalOrders>> withoutCompany =
				ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(normalOrdersMapper).selectOne(withoutCompany.capture());
		assertThat(withoutCompany.getValue().getSqlSegment()).contains("order_id");
		assertThat(withoutCompany.getValue().getSqlSegment()).doesNotContain("company_id");

		lookupAdapter.find(3L, "100");
		ArgumentCaptor<LambdaQueryWrapper<NormalOrders>> withCompany =
				ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(normalOrdersMapper, org.mockito.Mockito.times(2)).selectOne(withCompany.capture());
		assertThat(withCompany.getAllValues().get(1).getSqlSegment()).contains("company_id");
	}

	@Test
	void lookup_nonNumericOrderId_isEmpty() {
		assertThat(lookupAdapter.find(1L, "not-a-number")).isEmpty();
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

	private static Map<String, Object> happyOrder() {
		Map<String, Object> order = order("PAYED", "logistics");
		order.put("order_id", 88001L);
		order.put("company_id", 7L);
		Map<String, Object> promo = new LinkedHashMap<>();
		promo.put("discount_fee", 150);
		promo.put("rule", "满减");
		order.put("discount_info", List.of(promo));
		Map<String, Object> line = item();
		line.put("order_id", 88001L);
		line.put("item_name", "商品A");
		order.put("items", List.of(line));
		return order;
	}

	private static Map<String, Object> item() {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", 11L);
		item.put("item_bn", "SKU-1");
		item.put("price", 1000);
		item.put("item_name", "商品");
		item.put("num", 2);
		item.put("item_fee", 2000);
		item.put("order_item_type", "normal");
		item.put("discount_fee", 100);
		item.put("point_fee", 50);
		item.put("item_point", 3);
		item.put("total_fee", 1800);
		item.put("order_id", 100L);
		return item;
	}

	private static Map<String, Object> trade() {
		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("payType", "wxpay");
		trade.put("payFee", 1235);
		trade.put("orderId", "100");
		trade.put("tradeId", "T1");
		trade.put("openId", "buyer-open");
		trade.put("mchId", "mch");
		trade.put("timeStart", "1700000000");
		trade.put("timeExpire", "1700003600");
		trade.put("transactionId", "WX1");
		trade.put("curPayFee", 1235);
		return trade;
	}

	private static Map<String, Object> happyTrade() {
		Map<String, Object> trade = trade();
		trade.put("orderId", "88001");
		return trade;
	}

	private static Map<String, Object> distributor() {
		Map<String, Object> distributor = new LinkedHashMap<>();
		distributor.put("shop_code", "SHOP1");
		distributor.put("name", "门店A");
		return distributor;
	}
}
