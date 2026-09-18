package cn.shopex.ecshopx.orders.service.front.userinvoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import cn.shopex.ecshopx.common.dispatch.InvoicePushOmsJobDispatchPublisher;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceItem;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceLog;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceItemMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceLogMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserInvoiceCreateTxServiceInvoicePushOmsTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, OrderInvoice.class);
		TableInfoHelper.initTableInfo(assistant, OrderInvoiceItem.class);
		TableInfoHelper.initTableInfo(assistant, OrderInvoiceLog.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrdersItems.class);
	}

	@Mock
	private OrderInvoiceMapper orderInvoiceMapper;

	@Mock
	private OrderInvoiceItemMapper orderInvoiceItemMapper;

	@Mock
	private OrderInvoiceLogMapper orderInvoiceLogMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private MembersMapper membersMapper;

	@Mock
	private InvoiceSettingRedisService invoiceSettingRedisService;

	@Mock
	private InvoicePushOmsJobDispatchPublisher invoicePushOmsJobDispatchPublisher;

	@Mock
	private AdminNormalOrderDetailService adminNormalOrderDetailService;

	@Mock
	private NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;

	private UserInvoiceCreateTxService service;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper();
		service = new UserInvoiceCreateTxService(
				orderInvoiceMapper,
				orderInvoiceItemMapper,
				orderInvoiceLogMapper,
				normalOrdersMapper,
				normalOrdersItemsMapper,
				membersMapper,
				invoiceSettingRedisService,
				objectMapper,
				invoicePushOmsJobDispatchPublisher,
				adminNormalOrderDetailService,
				normalOrdersServiceOrderDataAssembler);
	}

	@Test
	void createUserInvoice_orderLimit_publishesOncePerTicket() {
		when(invoiceSettingRedisService.getInvoiceSetting(3L)).thenReturn(Map.of());
		Members mem = new Members();
		when(membersMapper.selectById(10L)).thenReturn(mem);
		when(normalOrdersServiceOrderDataAssembler.toServiceOrderData(any())).thenAnswer(invocation -> {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("order_status", "PAY");
			m.put("freight_fee", 0);
			m.put("order_class", "normal");
			m.put("pay_type", "online");
			m.put("offline_payment_status", "0");
			return m;
		});

		NormalOrders o1 = mkOrder(5001L, 3L, 10L);
		NormalOrders o2 = mkOrder(5002L, 3L, 10L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(o1, o2);

		when(adminNormalOrderDetailService.buildOrderBundle(eq(3L), eq("5001"), eq(false)))
				.thenReturn(bundleItems(orderLine(88L, 1000, 0)));
		when(adminNormalOrderDetailService.buildOrderBundle(eq(3L), eq("5002"), eq(false)))
				.thenReturn(bundleItems(orderLine(89L, 2000, 0)));

		AtomicLong idSeq = new AtomicLong(700L);
		when(orderInvoiceMapper.insert(isA(OrderInvoice.class)))
				.thenAnswer(invocation -> {
					OrderInvoice inv = invocation.getArgument(0);
					inv.setId(idSeq.getAndIncrement());
					return 1;
				});
		when(orderInvoiceMapper.selectById(any()))
				.thenAnswer(invocation -> {
					long id = invocation.getArgument(0);
					String oid = id == 700L ? "5001" : "5002";
					return savedHeader(id, oid, 3L, 10L);
				});

		when(orderInvoiceItemMapper.insert(isA(OrderInvoiceItem.class))).thenReturn(1);
		when(orderInvoiceMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersItemsMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderInvoiceLogMapper.insert(isA(OrderInvoiceLog.class))).thenReturn(1);

		List<Map<String, Object>> invoiceItems = new ArrayList<>();
		invoiceItems.add(lineReq("5001", "88"));
		invoiceItems.add(lineReq("5002", "89"));
		Map<String, Object> data = baseInvoiceData(3L, 10L, invoiceItems);

		List<Map<String, Object>> rows = service.createUserInvoice(data, "order");
		assertEquals(2, rows.size());
		verify(invoicePushOmsJobDispatchPublisher, times(1)).publish(700L, 3L);
		verify(invoicePushOmsJobDispatchPublisher, times(1)).publish(701L, 3L);
	}

	@Test
	void createUserInvoice_itemLimit_publishesOnce() {
		when(invoiceSettingRedisService.getInvoiceSetting(3L)).thenReturn(Map.of());
		when(membersMapper.selectById(10L)).thenReturn(new Members());
		when(normalOrdersServiceOrderDataAssembler.toServiceOrderData(any())).thenAnswer(invocation -> {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("order_status", "PAY");
			m.put("freight_fee", 0);
			m.put("order_class", "normal");
			m.put("pay_type", "online");
			m.put("offline_payment_status", "0");
			return m;
		});

		NormalOrders o1 = mkOrder(5001L, 3L, 10L);
		NormalOrders o2 = mkOrder(5002L, 3L, 10L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(o1, o2);

		when(adminNormalOrderDetailService.buildOrderBundle(eq(3L), eq("5001"), eq(false)))
				.thenReturn(bundleItems(orderLine(88L, 1000, 0)));
		when(adminNormalOrderDetailService.buildOrderBundle(eq(3L), eq("5002"), eq(false)))
				.thenReturn(bundleItems(orderLine(89L, 500, 0)));

		when(orderInvoiceMapper.insert(isA(OrderInvoice.class)))
				.thenAnswer(invocation -> {
					OrderInvoice inv = invocation.getArgument(0);
					inv.setId(900L);
					return 1;
				});
		when(orderInvoiceMapper.selectById(900L))
				.thenAnswer(invocation -> savedHeader(900L, "5001,5002", 3L, 10L));

		when(orderInvoiceItemMapper.insert(isA(OrderInvoiceItem.class))).thenReturn(1);
		when(orderInvoiceMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersItemsMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderInvoiceLogMapper.insert(isA(OrderInvoiceLog.class))).thenReturn(1);

		List<Map<String, Object>> invoiceItems = new ArrayList<>();
		invoiceItems.add(lineReq("5001", "88"));
		invoiceItems.add(lineReq("5002", "89"));
		Map<String, Object> data = baseInvoiceData(3L, 10L, invoiceItems);

		List<Map<String, Object>> rows = service.createUserInvoice(data, "item");
		assertEquals(1, rows.size());
		verify(invoicePushOmsJobDispatchPublisher, times(1)).publish(900L, 3L);
	}

	private static Map<String, Object> baseInvoiceData(long companyId, long userId, List<Map<String, Object>> invoiceItems) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("user_id", userId);
		data.put("invoice_type", "plain");
		data.put("company_title", "T");
		data.put("invoice_source", "user");
		data.put("invoice_method", "electronic");
		data.put("invoice_item", invoiceItems);
		return data;
	}

	private static Map<String, Object> lineReq(String orderId, String lineId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", orderId);
		m.put("id", lineId);
		return m;
	}

	private static Map<String, Object> orderLine(long id, int totalFee, int refunded) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", id);
		m.put("total_fee", totalFee);
		m.put("refunded_fee", refunded);
		m.put("num", 1);
		m.put("item_name", "n");
		m.put("item_bn", "bn");
		m.put("tax_rate", 6);
		m.put("pic", "");
		m.put("spec_info", "");
		m.put("item_spec_desc", "");
		return m;
	}

	private static Map<String, Object> bundleItems(Map<String, Object> line) {
		Map<String, Object> bundle = new LinkedHashMap<>();
		bundle.put("items", List.of(line));
		return bundle;
	}

	private static NormalOrders mkOrder(long orderId, long companyId, long userId) {
		NormalOrders o = new NormalOrders();
		o.setOrderId(orderId);
		o.setCompanyId(companyId);
		o.setUserId(userId);
		o.setOrderStatus("PAY");
		o.setFreightFee(0);
		o.setOrderClass("normal");
		return o;
	}

	private static OrderInvoice savedHeader(long id, String orderId, long companyId, long userId) {
		OrderInvoice inv = new OrderInvoice();
		inv.setId(id);
		inv.setOrderId(orderId);
		inv.setCompanyId(companyId);
		inv.setUserId(userId);
		inv.setInvoiceStatus("pending");
		inv.setInvoiceAmount(1);
		inv.setTryTimes(0);
		inv.setIsOms(0);
		inv.setInvoiceApplyBn("I");
		inv.setInvoiceType("plain");
		inv.setInvoiceSource("user");
		inv.setInvoiceMethod("electronic");
		inv.setCompanyTitle("T");
		return inv;
	}
}
