package cn.shopex.ecshopx.orders.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.service.admin.PlatformSelfSubCancelSupport;
import cn.shopex.ecshopx.orders.service.admin.PartialDeliveryFulfillmentReconcileService;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class AdminOrderPassRefundServiceMainCancelStatusRevertTest {

	private static final long COMPANY_ID = 1L;
	private static final long ORDER_ID = 5352932000075097L;

	@Mock private ApplicationContext applicationContext;
	@Mock private AftersalesRefundService aftersalesRefundService;
	@Mock private CancelOrdersMapper cancelOrdersMapper;
	@Mock private NormalOrdersMapper normalOrdersMapper;
	@Mock private NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock private OrderAssociationsMapper orderAssociationsMapper;
	@Mock private SupplierOrderMapper supplierOrderMapper;
	@Mock private NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	@Mock private OrderProfitMapper orderProfitMapper;
	@Mock private PlatformSelfSubCancelSupport platformSelfSubCancelSupport;

	private NormalOrderStatusUpdateService normalOrderStatusUpdateService;
	private AdminOrderPassRefundService service;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), SupplierOrder.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
	}

	@BeforeEach
	void init() {
		normalOrderStatusUpdateService =
				new NormalOrderStatusUpdateService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						supplierOrderMapper,
						orderAssociationsMapper);
		service =
				new AdminOrderPassRefundService(
						applicationContext,
						aftersalesRefundService,
						cancelOrdersMapper,
						normalOrdersMapper,
						orderAssociationsMapper,
						supplierOrderMapper,
						normalOrdersRelDadaMapper,
						orderProfitMapper,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						mock(NormalOrderFullCancelItemStoreRestoreService.class),
						normalOrderStatusUpdateService,
						platformSelfSubCancelSupport,
						mock(PartialDeliveryFulfillmentReconcileService.class),
				mock(NormalOrderCancelDiscountRestoreService.class));
	}

	@Test
	@DisplayName("子单全审过且存在 supplier_id=0 整单退款：主单应完成 CANCEL+SUCCESS")
	void updateMainOrderCancelSuccess_fullOrderRefundApproved_completesMainCancel() {
		NormalOrders main = new NormalOrders();
		main.setCompanyId(COMPANY_ID);
		main.setOrderId(ORDER_ID);
		main.setOrderStatus("PAYED");
		main.setCancelStatus("NO_APPLY_CANCEL");
		when(normalOrdersMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(main);
		when(supplierOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cancelledSupplier(39));
		when(supplierOrderMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(java.util.List.of(cancelledSupplier(30), cancelledSupplier(39)));
		when(supplierOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(aftersalesRefundService.countReadySupplierSubCancelRefunds(COMPANY_ID, ORDER_ID))
				.thenReturn(0L);
		when(platformSelfSubCancelSupport.countReadyPlatformSelfSubCancelRefunds(COMPANY_ID, ORDER_ID))
				.thenReturn(0L);
		when(platformSelfSubCancelSupport.hasApprovedFullOrderCancelRefund(COMPANY_ID, ORDER_ID))
				.thenReturn(true);
		when(normalOrdersItemsMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);

		service.updateMainOrderCancelSuccess(COMPANY_ID, ORDER_ID, 39L);

		verify(platformSelfSubCancelSupport).markSupplierItemsCancelled(COMPANY_ID, ORDER_ID, 39L);
		verify(platformSelfSubCancelSupport).hasApprovedFullOrderCancelRefund(COMPANY_ID, ORDER_ID);
		verify(normalOrdersMapper, atLeastOnce()).update(any(), any());
	}

	@Test
	@DisplayName("仅子单取消、平台明细仍在：主单回退 PAYED+NO_APPLY_CANCEL")
	void updateMainOrderCancelSuccess_partialSupplierOnly_revertsMainToPayed() {
		NormalOrders main = new NormalOrders();
		main.setCompanyId(COMPANY_ID);
		main.setOrderId(ORDER_ID);
		main.setOrderStatus("CANCEL");
		main.setCancelStatus("SUCCESS");
		when(normalOrdersMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(main);
		when(supplierOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cancelledSupplier(39));
		when(supplierOrderMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(java.util.List.of(cancelledSupplier(30), cancelledSupplier(39)));
		when(supplierOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(aftersalesRefundService.countReadySupplierSubCancelRefunds(COMPANY_ID, ORDER_ID))
				.thenReturn(0L);
		when(platformSelfSubCancelSupport.countReadyPlatformSelfSubCancelRefunds(COMPANY_ID, ORDER_ID))
				.thenReturn(0L);
		when(platformSelfSubCancelSupport.hasApprovedFullOrderCancelRefund(COMPANY_ID, ORDER_ID))
				.thenReturn(false);
		when(normalOrdersItemsMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);

		service.updateMainOrderCancelSuccess(COMPANY_ID, ORDER_ID, 39L);

		verify(platformSelfSubCancelSupport).markSupplierItemsCancelled(COMPANY_ID, ORDER_ID, 39L);
		verify(platformSelfSubCancelSupport).hasApprovedFullOrderCancelRefund(COMPANY_ID, ORDER_ID);
		verify(normalOrdersMapper).update(any(), any());
	}

	private static SupplierOrder cancelledSupplier(int supplierId) {
		SupplierOrder row = new SupplierOrder();
		row.setSupplierId(supplierId);
		row.setOrderStatus("CANCEL");
		row.setCancelStatus("SUCCESS");
		return row;
	}
}
