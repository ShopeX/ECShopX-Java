package cn.shopex.ecshopx.orders.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.service.AftersalesShopPartialCancelCreateService;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitDealNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminNormalOrderPartialCancelSupplierReconcileProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), SupplierOrder.class);
	}

	@Mock NormalOrdersMapper normalOrdersMapper;
	@Mock NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock SupplierOrderMapper supplierOrderMapper;
	@Mock AftersalesShopPartialCancelCreateService aftersalesShopPartialCancelCreateService;
	@Mock OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	@Mock SendAfterSaleWaitDealNoticeJobDispatchPublisher noticePublisher;
	@Mock PlatformSelfSubCancelSupport platformSelfSubCancelSupport;
	@Mock PartialDeliveryFulfillmentReconcileService partialDeliveryFulfillmentReconcileService;

	@InjectMocks AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService;

	@Test
	@DisplayName("单供部分取消剩余：只更新 supplier_order，主单收口交给 reconcile，不按 supplier_id 改 normal_orders")
	void supplierPartialCancel_updatesSupplierOrderAndReconciles_withoutMainSupplierIdFilter() {
		long companyId = 1L;
		long orderId = 5395644000215097L;
		long userId = 101L;
		long supplierId = 39L;

		NormalOrders order = payedPartail(companyId, orderId, userId);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		NormalOrdersItems shipped = line(46371L, companyId, orderId, 39, 1, 1, 0);
		NormalOrdersItems unshipped = line(46372L, companyId, orderId, 39, 1, 0, 0);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(shipped, unshipped));
		when(normalOrdersItemsMapper.selectOne(any())).thenReturn(unshipped);
		when(normalOrdersItemsMapper.update(any(), any())).thenReturn(1);
		when(supplierOrderMapper.update(any(), any())).thenReturn(1);

		stubCreate(companyId, orderId, userId, supplierId, "supplier", 39L);

		adminNormalOrderPartialCancelService.execute(
				companyId, "supplier", 39L, supplierId, userId, orderId, "买家取消");

		verify(supplierOrderMapper, times(1)).update(any(), any());
		verify(partialDeliveryFulfillmentReconcileService, times(1))
				.reconcileIfFullyProcessed(eq(companyId), eq(orderId));
		verify(platformSelfSubCancelSupport, never())
				.finalizeSupplierOrdersAfterRemainingCancelPass(anyLong(), anyLong());
		verify(normalOrdersMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("多供：一供取消剩余时仍 reconcile（未齐单则 no-op），且不 finalize 全部子单")
	void multiSupplierPartialCancel_reconcilesOnce_doesNotFinalizeAllSuppliers() {
		long companyId = 1L;
		long orderId = 90001L;
		long userId = 202L;
		long supplierA = 30L;

		NormalOrders order = payedPartail(companyId, orderId, userId);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		// A: 1 已发 + 1 未发；B: 1 未发仍待发
		NormalOrdersItems aShipped = line(1L, companyId, orderId, 30, 1, 1, 0);
		NormalOrdersItems aUnshipped = line(2L, companyId, orderId, 30, 1, 0, 0);
		NormalOrdersItems bPending = line(3L, companyId, orderId, 39, 1, 0, 0);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(aShipped, aUnshipped, bPending));
		when(normalOrdersItemsMapper.selectOne(any())).thenReturn(aUnshipped);
		when(normalOrdersItemsMapper.update(any(), any())).thenReturn(1);
		when(supplierOrderMapper.update(any(), any())).thenReturn(1);

		stubCreate(companyId, orderId, userId, supplierA, "supplier", 30L);

		adminNormalOrderPartialCancelService.execute(
				companyId, "supplier", 30L, supplierA, userId, orderId, "买家取消");

		verify(supplierOrderMapper, times(1)).update(any(), any());
		verify(partialDeliveryFulfillmentReconcileService, times(1))
				.reconcileIfFullyProcessed(eq(companyId), eq(orderId));
		verify(platformSelfSubCancelSupport, never())
				.finalizeSupplierOrdersAfterRemainingCancelPass(anyLong(), anyLong());
		verify(normalOrdersMapper, never()).update(any(), any());
	}

	private static NormalOrders payedPartail(long companyId, long orderId, long userId) {
		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setUserId(userId);
		order.setOrderStatus("PAYED");
		order.setDeliveryStatus("PARTAIL");
		order.setType(0);
		order.setSupplierId(0);
		return order;
	}

	private static NormalOrdersItems line(
			long id, long companyId, long orderId, int supplierId, int num, int delivered, int cancelled) {
		NormalOrdersItems item = new NormalOrdersItems();
		item.setId(id);
		item.setCompanyId(companyId);
		item.setOrderId(orderId);
		item.setSupplierId(supplierId);
		item.setNum(num);
		item.setDeliveryItemNum(delivered);
		item.setCancelItemNum(cancelled);
		return item;
	}

	private void stubCreate(
			long companyId, long orderId, long userId, long supplierId, String operatorType, long operatorId) {
		Map<String, Object> createOut = new LinkedHashMap<>();
		createOut.put("aftersales_bn", 202610099999L);
		createOut.put("refund_fee", 0);
		createOut.put("refund_point", 100);
		when(aftersalesShopPartialCancelCreateService.createForShopPartialCancel(
						eq(companyId),
						eq(orderId),
						eq(userId),
						eq(supplierId),
						eq(operatorType),
						eq(operatorId),
						eq("买家取消"),
						any()))
				.thenReturn(createOut);
		when(orderValiditySettingRedisReadService.readPlatformSetting(eq(companyId)))
				.thenReturn(new LinkedHashMap<>());
	}
}
