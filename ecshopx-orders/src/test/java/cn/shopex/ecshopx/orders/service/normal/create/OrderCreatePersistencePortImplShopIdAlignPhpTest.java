package cn.shopex.ecshopx.orders.service.normal.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderProfitByOrderResultPort;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelSupplierMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderPromotionsMapper;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PHP OrderService::_formatOrderData sets order/item/association shop_id from shopInfo (0 when
 * isCheckShopValid=false for normal/shopadmin). Persist must not rewrite shop_id to distributor_id.
 */
@ExtendWith(MockitoExtension.class)
class OrderCreatePersistencePortImplShopIdAlignPhpTest {

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private OrderPromotionsMapper orderPromotionsMapper;

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper;

	@Mock
	private NormalOrderNumericIdService normalOrderNumericIdService;

	@Mock
	private OrderProfitByOrderResultPort orderProfitByOrderResultPort;

	@Mock
	private CompanyDefaultCurrencyService companyDefaultCurrencyService;

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
	}

	@Test
	void persistOrder_usesOrderDataShopId_notDistributorId() {
		when(normalOrderNumericIdService.generate(anyLong())).thenReturn(90001L);
		CurrencyExchangeRate cny = new CurrencyExchangeRate();
		cny.setCurrency("CNY");
		cny.setRate(1.0);
		cny.setSymbol("￥");
		when(companyDefaultCurrencyService.getCur(141L)).thenReturn(cny);

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

		port.persistOrder(p);

		ArgumentCaptor<NormalOrders> orderCaptor = ArgumentCaptor.forClass(NormalOrders.class);
		verify(normalOrdersMapper).insert(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getDistributorId()).isEqualTo(104L);
		assertThat(orderCaptor.getValue().getShopId()).isEqualTo(0L);

		ArgumentCaptor<NormalOrdersItems> itemCaptor = ArgumentCaptor.forClass(NormalOrdersItems.class);
		verify(normalOrdersItemsMapper).insert(itemCaptor.capture());
		assertThat(itemCaptor.getValue().getShopId()).isEqualTo(0L);

		ArgumentCaptor<OrderAssociations> assocCaptor = ArgumentCaptor.forClass(OrderAssociations.class);
		verify(orderAssociationsMapper).insert(assocCaptor.capture());
		assertThat(assocCaptor.getValue().getShopId()).isEqualTo(0L);
	}

	@Test
	void persistOrder_usesDefaultCurrencyWhenOrderDataMissingFeeType() {
		when(normalOrderNumericIdService.generate(anyLong())).thenReturn(90002L);
		CurrencyExchangeRate usd = new CurrencyExchangeRate();
		usd.setCurrency("USD");
		usd.setRate(1.0);
		usd.setSymbol("$");
		when(companyDefaultCurrencyService.getCur(38L)).thenReturn(usd);

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		Map<String, Object> od = p.getOrderData();
		od.put("company_id", 38L);
		od.put("user_id", 7L);
		od.put("mobile", "13800000000");
		od.put("distributor_id", 0L);
		od.put("shop_id", 0L);
		od.put("order_type", "normal");
		od.put("order_class", "normal");
		od.put("receipt_type", "logistics");
		od.put("title", "sku");
		od.put("total_fee", 100);
		od.put("item_fee", 100);
		od.put("pay_type", "doumen_intl");
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", 11L);
		item.put("goods_id", 11L);
		item.put("item_name", "sku");
		item.put("num", 1);
		item.put("price", 100);
		item.put("total_fee", 100);
		item.put("item_fee", 100);
		od.put("items", List.of(item));

		port.persistOrder(p);

		ArgumentCaptor<NormalOrders> orderCaptor = ArgumentCaptor.forClass(NormalOrders.class);
		verify(normalOrdersMapper).insert(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getFeeType()).isEqualTo("USD");
		assertThat(orderCaptor.getValue().getFeeSymbol()).isEqualTo("$");

		ArgumentCaptor<NormalOrdersItems> itemCaptor = ArgumentCaptor.forClass(NormalOrdersItems.class);
		verify(normalOrdersItemsMapper).insert(itemCaptor.capture());
		assertThat(itemCaptor.getValue().getFeeType()).isEqualTo("USD");

		ArgumentCaptor<OrderAssociations> assocCaptor = ArgumentCaptor.forClass(OrderAssociations.class);
		verify(orderAssociationsMapper).insert(assocCaptor.capture());
		assertThat(assocCaptor.getValue().getFeeType()).isEqualTo("USD");

		assertThat(p.getOrdersInsertResult().get("fee_type")).isEqualTo("USD");
	}

	@Test
	void persistOrder_savesItemSpecDescOnOrderItem() {
		when(normalOrderNumericIdService.generate(anyLong())).thenReturn(90003L);

		NormalOrderCreateParams p = new NormalOrderCreateParams();
		Map<String, Object> od = p.getOrderData();
		od.put("company_id", 7L);
		od.put("user_id", 9L);
		od.put("distributor_id", 0L);
		od.put("shop_id", 0L);
		od.put("order_type", "normal");
		od.put("order_class", "normal");
		od.put("receipt_type", "logistics");
		od.put("total_fee", 7120);
		od.put("item_fee", 7120);
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", 131L);
		item.put("item_name", "抽纸");
		item.put("num", 8);
		item.put("item_spec_desc", "规格:1880张/提");
		od.put("items", List.of(item));

		port.persistOrder(p);

		ArgumentCaptor<NormalOrdersItems> itemCaptor = ArgumentCaptor.forClass(NormalOrdersItems.class);
		verify(normalOrdersItemsMapper).insert(itemCaptor.capture());
		assertThat(itemCaptor.getValue().getItemSpecDesc()).isEqualTo("规格:1880张/提");
	}
}
