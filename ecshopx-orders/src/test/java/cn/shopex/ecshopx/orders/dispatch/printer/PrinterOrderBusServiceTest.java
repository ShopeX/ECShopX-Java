package cn.shopex.ecshopx.orders.dispatch.printer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.espier.integration.yilianyun.YilianyunPrintCommand;
import cn.shopex.ecshopx.espier.integration.yilianyun.YilianyunPrintPort;
import cn.shopex.ecshopx.espier.service.printer.PrinterCompanyConfigApplicationService;
import cn.shopex.ecshopx.espier.service.printer.PrinterShopApplicationService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelZitiMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PrinterOrderBusServiceTest {

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private NormalOrdersRelZitiMapper normalOrdersRelZitiMapper;

	@Mock
	private OperatorsMapper operatorsMapper;

	@Mock
	private AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;

	@Mock
	private PrinterCompanyConfigApplicationService printerCompanyConfigApplicationService;

	@Mock
	private PrinterShopApplicationService printerShopApplicationService;

	@Mock
	private YilianyunPrintPort yilianyunPrintPort;

	@InjectMocks
	private PrinterOrderBusService printerOrderBusService;

	@Test
	void handleTradeFinishRow_whenOrderMissing_skipsPrint() {
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

		Map<String, Object> tradeRow = new LinkedHashMap<>();
		tradeRow.put("order_id", 501L);
		tradeRow.put("company_id", 9L);
		tradeRow.put("distributor_id", 12L);

		printerOrderBusService.handleTradeFinishRow(tradeRow);

		verify(yilianyunPrintPort, never()).printTicket(any());
	}

	@Test
	void handleTradeFinishRow_whenCompanyPrintClosed_skipsPrint() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setOrderId(501L);
		assoc.setCompanyId(9L);
		assoc.setTitle("T");
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assoc);

		Map<String, Object> closedCfg = new LinkedHashMap<>();
		closedCfg.put("is_open", Boolean.FALSE);
		when(printerCompanyConfigApplicationService.info(9L, "yilianyun")).thenReturn(closedCfg);

		Map<String, Object> tradeRow = new LinkedHashMap<>();
		tradeRow.put("order_id", 501L);
		tradeRow.put("company_id", 9L);
		tradeRow.put("distributor_id", 12L);

		printerOrderBusService.handleTradeFinishRow(tradeRow);

		verify(yilianyunPrintPort, never()).printTicket(any());
	}

	@Test
	void handleTradeFinishRow_whenLogisticsReceipt_skipsPrint() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setOrderId(501L);
		assoc.setCompanyId(9L);
		assoc.setStoreName("测试店铺名称");
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assoc);

		NormalOrders order = new NormalOrders();
		order.setOrderId(501L);
		order.setCompanyId(9L);
		order.setReceiptType("logistics");
		when(normalOrdersMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

		Map<String, Object> tradeRow = new LinkedHashMap<>();
		tradeRow.put("order_id", 501L);
		tradeRow.put("company_id", 9L);
		tradeRow.put("distributor_id", 12L);

		printerOrderBusService.handleTradeFinishRow(tradeRow);

		verify(yilianyunPrintPort, never()).printTicket(any());
		verify(printerCompanyConfigApplicationService, never()).info(anyLong(), anyString());
	}

	@Test
	void handleTradeFinishRow_whenOpenAndDeviceMatched_printsShippingTicketLayout() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setOrderId(501L);
		assoc.setCompanyId(9L);
		assoc.setStoreName("测试店铺名称");
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assoc);

		Map<String, Object> openCfg = new LinkedHashMap<>();
		openCfg.put("is_open", Boolean.TRUE);
		openCfg.put("app_id", "aid");
		openCfg.put("app_key", "asec");
		when(printerCompanyConfigApplicationService.info(9L, "yilianyun")).thenReturn(openCfg);

		Map<String, Object> device = new LinkedHashMap<>();
		device.put("distributor_id", "12");
		device.put("app_terminal", "MC01");
		device.put("app_key", "devk");
		Map<String, Object> listOut = new LinkedHashMap<>();
		listOut.put("list", List.of(device));
		listOut.put("total_count", 1L);
		when(printerShopApplicationService.lists(
						anyLong(), anyInt(), anyInt(), ArgumentMatchers.anyString()))
				.thenReturn(listOut);

		NormalOrders order = new NormalOrders();
		order.setOrderId(501L);
		order.setCompanyId(9L);
		order.setCreateTime(1754361000);
		order.setPayType("wxpay");
		order.setReceiverName("测试张三");
		order.setReceiverMobile("13800000000");
		order.setReceiverAddress("打印测试地址");
		order.setRemark("请尽快发货");
		order.setReceiptType("merchant");
		when(normalOrdersMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

		NormalOrdersItems item = new NormalOrdersItems();
		item.setItemName("测试商品B");
		item.setItemSpecDesc("标准装");
		item.setPrice(500);
		item.setNum(3);
		item.setItemUnit("件");
		item.setItemFee(1500);
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

		Map<String, Object> distributor = new LinkedHashMap<>();
		distributor.put("store_name", "测试门店");
		distributor.put("mobile", "13800000002");
		distributor.put("store_address", "测试门店地址");
		when(adminOrderDetailDistributionSupportPort.getDistributorInfoFormatted(9L, 12L))
				.thenReturn(distributor);

		Map<String, Object> tradeRow = new LinkedHashMap<>();
		tradeRow.put("order_id", 501L);
		tradeRow.put("company_id", 9L);
		tradeRow.put("distributor_id", 12L);
		tradeRow.put("pay_fee", 1500);
		tradeRow.put("pay_type", "wxpay");

		printerOrderBusService.handleTradeFinishRow(tradeRow);

		ArgumentCaptor<YilianyunPrintCommand> captor = ArgumentCaptor.forClass(YilianyunPrintCommand.class);
		verify(yilianyunPrintPort).printTicket(captor.capture());
		String content = captor.getValue().content();
		assertThat(content)
				.contains("<FS2><center>发货小票</center></FS2>\n")
				.contains("测试店铺名称")
				.contains("单号:501\n")
				.contains("下单店铺:测试门店")
				.contains("测试商品B(标准装)\n")
				.contains("1件 * 3")
				.contains("支付方式:微信支付\n")
				.contains("<FS2>实付:15.00</FS2>")
				.contains("收件人:测试张三")
				.contains("店铺电话:13800000002")
				.doesNotContain("<BR>")
				.doesNotContain("BARCODE")
				.doesNotContain("卖家备注")
				.doesNotContain("已退差价");
	}
}
