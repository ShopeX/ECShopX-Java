package cn.shopex.ecshopx.orders.dispatch.printer;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelZiti;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class YilianyunShippingTicketAssemblerTest {

	private final YilianyunShippingTicketAssembler assembler = new YilianyunShippingTicketAssembler();

	@Test
	void mapsOrderFieldsAndFormatsQuantityWithUnit() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setOrderId(20250805001L);
		assoc.setStoreName("测试店铺名称");

		NormalOrders order = new NormalOrders();
		order.setOrderId(20250805001L);
		order.setCreateTime(unix("2025-08-05 10:30:00"));
		order.setPayType("wxpay");
		order.setReceiverName("测试张三");
		order.setReceiverMobile("13800000000");
		order.setReceiverState("上海市");
		order.setReceiverCity("上海市");
		order.setReceiverDistrict("徐汇区");
		order.setReceiverAddress("打印测试地址");
		order.setRemark("测试买家留言: 请尽快发货");
		order.setReceiptType("logistics");
		order.setSelfDeliveryTime(0);
		order.setOperatorId(8);

		NormalOrdersItems weigh = new NormalOrdersItems();
		weigh.setItemName("测试商品A");
		weigh.setItemSpecDesc("测试规格");
		weigh.setPrice(100);
		weigh.setNum(10);
		weigh.setItemUnit("KG");
		weigh.setItemFee(1000);

		NormalOrdersItems pack = new NormalOrdersItems();
		pack.setItemName("测试商品B");
		pack.setItemSpecDesc("标准装");
		pack.setPrice(500);
		pack.setNum(3);
		pack.setItemUnit("件");
		pack.setItemFee(1500);

		Operators operator = new Operators();
		operator.setUsername("测试操作员");

		Map<String, Object> distributor = new LinkedHashMap<>();
		distributor.put("store_name", "测试门店");
		distributor.put("mobile", "13800000002");
		distributor.put("store_address", "测试门店地址");

		YilianyunShippingTicketModel model =
				assembler.assemble(
						assoc,
						order,
						List.of(weigh, pack),
						null,
						distributor,
						operator,
						2500L,
						"wxpay",
						false);

		assertThat(model.storeName()).isEqualTo("测试店铺名称");
		assertThat(model.orderId()).isEqualTo("20250805001");
		assertThat(model.orderTime()).isEqualTo("2025-08-05 10:30:00");
		assertThat(model.orderStoreName()).isEqualTo("测试门店");
		assertThat(model.printerOperator()).isEqualTo("测试操作员");
		assertThat(model.items()).hasSize(2);
		assertThat(model.items().get(0).displayName()).isEqualTo("测试商品A(测试规格)");
		assertThat(model.items().get(0).unitPriceYuan()).isEqualTo("1.00");
		assertThat(model.items().get(0).quantityText()).isEqualTo("1KG * 10");
		assertThat(model.items().get(0).linePaidYuan()).isEqualTo("10.00");
		assertThat(model.items().get(1).quantityText()).isEqualTo("1件 * 3");
		assertThat(model.paidYuan()).isEqualTo("25.00");
		assertThat(model.payTypeLabel()).isEqualTo("微信支付");
		assertThat(model.receiverName()).isEqualTo("测试张三");
		assertThat(model.receiverAddress()).contains("打印测试地址");
		assertThat(model.buyerRemark()).isEqualTo("测试买家留言: 请尽快发货");
		assertThat(model.storePhone()).isEqualTo("13800000002");
		assertThat(model.storeAddress()).isEqualTo("测试门店地址");
		assertThat(model.deliveryTime()).isEmpty();
	}

	@Test
	void appendsEachAssociatedSpecAfterItemName() {
		assertThat(YilianyunShippingTicketAssembler.displayName("美汁源果粒橙【LSD】", "容量:1.5L"))
				.isEqualTo("美汁源果粒橙【LSD】(1.5L)");
		assertThat(YilianyunShippingTicketAssembler.displayName("测试商品", "颜色:红,尺寸:L"))
				.isEqualTo("测试商品(红、L)");
		assertThat(YilianyunShippingTicketAssembler.displayName("测试商品", "颜色:红，尺寸:L，口味:原味"))
				.isEqualTo("测试商品(红、L、原味)");
		assertThat(YilianyunShippingTicketAssembler.displayName("测试商品", "标准装"))
				.isEqualTo("测试商品(标准装)");
		assertThat(YilianyunShippingTicketAssembler.displayName("可口可乐", "")).isEqualTo("可口可乐");
		assertThat(YilianyunShippingTicketAssembler.displayName("可口可乐", "单规格")).isEqualTo("可口可乐");
	}

	@Test
	void usesZitiPickupAndMasksReceiverWhenHidden() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setOrderId(1001L);
		assoc.setStoreName("自提店");

		NormalOrders order = new NormalOrders();
		order.setOrderId(1001L);
		order.setCreateTime(unix("2025-08-05 10:30:00"));
		order.setReceiptType("ziti");
		order.setReceiverName("张三");
		order.setReceiverMobile("13900000000");
		order.setMobile("13900000000");

		NormalOrdersRelZiti ziti = new NormalOrdersRelZiti();
		ziti.setName("自提点A");
		ziti.setProvince("上海市");
		ziti.setCity("上海市");
		ziti.setArea("徐汇区");
		ziti.setAddress("宜山路1号");
		ziti.setPickupDate("2025-08-06");
		ziti.setPickupTime("10:00-12:00");
		ziti.setContractPhone("02111112222");

		YilianyunShippingTicketModel model =
				assembler.assemble(assoc, order, List.of(), ziti, Map.of(), null, 100L, "alipay", true);

		assertThat(model.receiverName()).startsWith("张");
		assertThat(model.receiverName()).contains("*");
		assertThat(model.receiverAddress()).contains("宜山路1号");
		assertThat(model.deliveryTime()).isEqualTo("2025-08-06 10:00-12:00");
		assertThat(model.payTypeLabel()).isEqualTo("支付宝");
	}

	private static int unix(String localDateTime) {
		return (int)
				java.time.LocalDateTime.parse(localDateTime.replace(" ", "T"))
						.atZone(java.time.ZoneId.of("Asia/Shanghai"))
						.toEpochSecond();
	}
}
