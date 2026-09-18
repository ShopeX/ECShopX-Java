package cn.shopex.ecshopx.orders.service.espier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.domain.OrdersDeliveryItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryItemsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class NormalOrderEspierBatchDeliveryServicePlatformSelfOnlyTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), SupplierOrder.class);
	}

	@Mock NormalOrdersMapper normalOrdersMapper;
	@Mock NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock OrdersDeliveryMapper ordersDeliveryMapper;
	@Mock OrdersDeliveryItemsMapper ordersDeliveryItemsMapper;
	@Mock OrderAssociationsMapper orderAssociationsMapper;
	@Mock SupplierOrderMapper supplierOrderMapper;
	@Mock NormalOrdersEspierDeliveryCorpResolver deliveryCorpResolver;
	@Mock StringRedisTemplate stringRedisTemplate;
	@Mock ValueOperations<String, String> valueOperations;

	@InjectMocks NormalOrderEspierBatchDeliveryService service;

	@BeforeEach
	void stubRedis() {
		lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		lenient().when(valueOperations.get(any())).thenReturn("");
	}

	@Test
	void deliverBatchForUpload_whenPlatformAndOnlySupplierItems_throwsSupplierMustShip() {
		long companyId = 1L;
		long orderId = 9001L;

		NormalOrders order = baseOrder(companyId, orderId);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		// 平台查询自营行：空；但订单上仍有供应商商品
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(normalOrdersItemsMapper.selectCount(any())).thenReturn(2L);

		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() -> service.deliverBatchForUpload(companyId, orderId, 0L, "OTHER", "YD001"));
		assertEquals("供应商商品请由供应商发货", ex.getMessage());
		verify(ordersDeliveryMapper, never()).insert(any(OrdersDelivery.class));
	}

	@Test
	void deliverBatchForUpload_whenPlatformShipsSelfItems_leavesSupplierPendingAsPartail() {
		long companyId = 1L;
		long orderId = 9002L;

		NormalOrders order = baseOrder(companyId, orderId);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		NormalOrdersItems selfLine = new NormalOrdersItems();
		selfLine.setId(1L);
		selfLine.setCompanyId(companyId);
		selfLine.setOrderId(orderId);
		selfLine.setSupplierId(0);
		selfLine.setItemId(100L);
		selfLine.setGoodsId(100L);
		selfLine.setNum(1);
		selfLine.setItemName("self-sku");
		selfLine.setDeliveryStatus("PENDING");
		selfLine.setDeliveryItemNum(0);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(selfLine));
		// 发完自营后仍有供应商 PENDING
		when(normalOrdersItemsMapper.selectCount(any())).thenReturn(1L);

		when(deliveryCorpResolver.resolveDeliveryCorpName(companyId, "OTHER", 0L)).thenReturn("OTHER");
		when(ordersDeliveryMapper.insert(any(OrdersDelivery.class)))
				.thenAnswer(
						(Answer<Integer>)
								inv -> {
									OrdersDelivery d = inv.getArgument(0);
									d.setOrdersDeliveryId(1L);
									return 1;
								});

		AtomicReference<String> normalSqlSet = new AtomicReference<>("");
		when(normalOrdersMapper.update(isNull(), any(Wrapper.class)))
				.thenAnswer(
						inv -> {
							Object w = inv.getArgument(1);
							if (w instanceof LambdaUpdateWrapper<?> uw) {
								normalSqlSet.set(String.valueOf(uw.getSqlSet()) + uw.getParamNameValuePairs());
							}
							return 1;
						});
		when(orderAssociationsMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
		when(normalOrdersItemsMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
		when(ordersDeliveryItemsMapper.insert(any(OrdersDeliveryItems.class))).thenReturn(1);

		service.deliverBatchForUpload(companyId, orderId, 0L, "OTHER", "YD002");

		String normalSet = normalSqlSet.get() == null ? "" : normalSqlSet.get();
		assertTrue(normalSet.contains("PARTAIL"), "platform shipping self-only must leave PARTAIL, sql=" + normalSet);
		verify(supplierOrderMapper, never()).update(isNull(), any(Wrapper.class));
	}

	private static NormalOrders baseOrder(long companyId, long orderId) {
		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setOrderStatus("PAYED");
		order.setCancelStatus("NO_APPLY_CANCEL");
		order.setDeliveryStatus("PENDING");
		order.setReceiptType("logistics");
		order.setUserId(1L);
		order.setLeftAftersalesNum(0);
		return order;
	}
}
