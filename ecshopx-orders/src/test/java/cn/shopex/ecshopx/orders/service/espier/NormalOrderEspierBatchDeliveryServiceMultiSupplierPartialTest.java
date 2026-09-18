package cn.shopex.ecshopx.orders.service.espier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class NormalOrderEspierBatchDeliveryServiceMultiSupplierPartialTest {

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
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(any())).thenReturn("");
	}

	@Test
	void deliverBatchForUpload_whenOtherSupplierItemsStillPending_setsMainOrderPartailNotDone() {
		long companyId = 1L;
		long orderId = 5345655000204860L;
		long supplierId = 18L;

		AtomicReference<String> normalSqlSet = new AtomicReference<>("");
		AtomicReference<String> assocSqlSet = new AtomicReference<>("");
		stubHappyPathBasics(companyId, orderId, supplierId, normalSqlSet, assocSqlSet);

		// 发完当前供应商行后，全单仍有其它供应商 PENDING
		lenient().when(normalOrdersItemsMapper.selectCount(any())).thenReturn(3L);

		service.deliverBatchForUpload(companyId, orderId, supplierId, "OTHER", "其他");

		String normalSet = normalSqlSet.get() == null ? "" : normalSqlSet.get();
		String assocSet = assocSqlSet.get() == null ? "" : assocSqlSet.get();
		assertTrue(normalSet.contains("PARTAIL"), "main order should be PARTAIL, sqlSet=" + normalSet);
		assertTrue(!normalSet.contains("WAIT_BUYER_CONFIRM"), "must not promote order_status on partial");
		assertTrue(assocSet.contains("PARTAIL"), "associations should be PARTAIL, sqlSet=" + assocSet);
		verify(normalOrdersItemsMapper, atLeastOnce()).selectCount(any());
	}

	@Test
	void deliverBatchForUpload_whenNoPendingItemsRemain_setsMainOrderDoneAndWaitBuyerConfirm() {
		long companyId = 1L;
		long orderId = 5345655000204860L;
		long supplierId = 18L;

		AtomicReference<String> normalSqlSet = new AtomicReference<>("");
		AtomicReference<String> assocSqlSet = new AtomicReference<>("");
		stubHappyPathBasics(companyId, orderId, supplierId, normalSqlSet, assocSqlSet);

		lenient().when(normalOrdersItemsMapper.selectCount(any())).thenReturn(0L);

		service.deliverBatchForUpload(companyId, orderId, supplierId, "OTHER", "其他");

		String normalSet = normalSqlSet.get() == null ? "" : normalSqlSet.get();
		String assocSet = assocSqlSet.get() == null ? "" : assocSqlSet.get();
		assertTrue(normalSet.contains("DONE"), "main order should be DONE, sqlSet=" + normalSet);
		assertTrue(normalSet.contains("WAIT_BUYER_CONFIRM"), "order_status should be WAIT_BUYER_CONFIRM, sqlSet=" + normalSet);
		assertTrue(assocSet.contains("DONE"), "associations should be DONE, sqlSet=" + assocSet);
		assertTrue(assocSet.contains("WAIT_BUYER_CONFIRM"), "associations order_status should be WAIT_BUYER_CONFIRM, sqlSet=" + assocSet);
	}

	private void stubHappyPathBasics(
			long companyId,
			long orderId,
			long supplierId,
			AtomicReference<String> normalSqlSet,
			AtomicReference<String> assocSqlSet) {
		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setOrderStatus("PAYED");
		order.setCancelStatus("NO_APPLY_CANCEL");
		order.setDeliveryStatus("PENDING");
		order.setReceiptType("logistics");
		order.setUserId(1L);
		order.setLeftAftersalesNum(0);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		SupplierOrder so = new SupplierOrder();
		so.setCompanyId(companyId);
		so.setOrderId(orderId);
		so.setSupplierId(18);
		so.setOrderStatus("PAYED");
		so.setCancelStatus("NO_APPLY_CANCEL");
		so.setDeliveryStatus("PENDING");
		so.setReceiptType("logistics");
		when(supplierOrderMapper.selectOne(any())).thenReturn(so);

		NormalOrdersItems shipLine = new NormalOrdersItems();
		shipLine.setId(45873L);
		shipLine.setCompanyId(companyId);
		shipLine.setOrderId(orderId);
		shipLine.setSupplierId(18);
		shipLine.setItemId(1935L);
		shipLine.setGoodsId(1935L);
		shipLine.setNum(1);
		shipLine.setItemName("milk");
		shipLine.setDeliveryStatus("PENDING");
		shipLine.setDeliveryItemNum(0);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(shipLine));

		when(deliveryCorpResolver.resolveDeliveryCorpName(companyId, "OTHER", supplierId))
				.thenReturn("OTHER");

		when(ordersDeliveryMapper.insert(any(OrdersDelivery.class)))
				.thenAnswer(
						(Answer<Integer>)
								inv -> {
									OrdersDelivery d = inv.getArgument(0);
									d.setOrdersDeliveryId(314L);
									return 1;
								});

		when(normalOrdersMapper.update(isNull(), any(Wrapper.class)))
				.thenAnswer(
						inv -> {
							Object w = inv.getArgument(1);
							if (w instanceof LambdaUpdateWrapper<?> uw) {
								normalSqlSet.set(
										String.valueOf(uw.getSqlSet()) + uw.getParamNameValuePairs());
							}
							return 1;
						});
		when(orderAssociationsMapper.update(isNull(), any(Wrapper.class)))
				.thenAnswer(
						inv -> {
							Object w = inv.getArgument(1);
							if (w instanceof LambdaUpdateWrapper<?> uw) {
								assocSqlSet.set(
										String.valueOf(uw.getSqlSet()) + uw.getParamNameValuePairs());
							}
							return 1;
						});
		when(normalOrdersItemsMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
		when(supplierOrderMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
		when(ordersDeliveryItemsMapper.insert(any(OrdersDeliveryItems.class))).thenReturn(1);
	}
}
