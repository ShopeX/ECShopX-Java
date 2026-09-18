package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrderStatusUpdateServicePartialSupplierCancelTest {

	private static final long COMPANY_ID = 1L;
	private static final long ORDER_ID = 5352863000075097L;

	@Mock private NormalOrdersMapper normalOrdersMapper;
	@Mock private NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock private SupplierOrderMapper supplierOrderMapper;
	@Mock private OrderAssociationsMapper orderAssociationsMapper;

	private NormalOrderStatusUpdateService service;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), SupplierOrder.class);
	}

	@BeforeEach
	void init() {
		service =
				new NormalOrderStatusUpdateService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						supplierOrderMapper,
						orderAssociationsMapper);
	}

	@Test
	@DisplayName("全部 supplier 子单已取消但仍有 supplier_id=0 明细：不回写主单 CANCEL")
	void applyStatusUpdate_allSupplierCancel_withSelfItem_doesNotCancelMain() {
		when(supplierOrderMapper.selectOne(any())).thenReturn(cancelledSupplier(30));
		when(supplierOrderMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(cancelledSupplier(30), cancelledSupplier(39)));
		when(normalOrdersItemsMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

		boolean updated =
				service.applyStatusUpdate(COMPANY_ID, ORDER_ID, 30L, "CANCEL", "SUCCESS");

		assertThat(updated).isFalse();
		verify(normalOrdersMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("仍有活跃 supplier 子单：不回写主单 CANCEL")
	void applyStatusUpdate_activeSupplierRemains_doesNotCancelMain() {
		when(supplierOrderMapper.selectOne(any())).thenReturn(activeSupplier(30));
		when(supplierOrderMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(cancelledSupplier(30), activeSupplier(39)));

		boolean updated =
				service.applyStatusUpdate(COMPANY_ID, ORDER_ID, 30L, "CANCEL", "SUCCESS");

		assertThat(updated).isFalse();
		verify(normalOrdersMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("误标 CANCEL 的主单可回退为 PAYED + NO_APPLY_CANCEL")
	void revertMainOrderToPayedNoApplyCancel_updatesMainAndAssociationsOnly() {
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		service.revertMainOrderToPayedNoApplyCancel(COMPANY_ID, ORDER_ID);

		verify(normalOrdersMapper).update(any(), any());
		verify(orderAssociationsMapper).update(any(), any());
		verify(supplierOrderMapper, never()).update(any(), any());
	}

	private static SupplierOrder cancelledSupplier(int supplierId) {
		SupplierOrder row = new SupplierOrder();
		row.setSupplierId(supplierId);
		row.setOrderStatus("CANCEL");
		row.setCancelStatus("SUCCESS");
		return row;
	}

	private static SupplierOrder activeSupplier(int supplierId) {
		SupplierOrder row = new SupplierOrder();
		row.setSupplierId(supplierId);
		row.setOrderStatus("PAYED");
		row.setCancelStatus("NO_APPLY_CANCEL");
		return row;
	}
}
