package cn.shopex.ecshopx.orders.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Drug process orders: reject path delegates to full vs partial cancel")
class AdminProcessDrugOrdersServiceTradeRefundPathProbeTest {

	@Test
	void reject_pending_invokesFullCancelWithShopReason() {
		OrderAssociationsMapper orderAssociationsMapper = mock(OrderAssociationsMapper.class);
		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		AdminNormalOrderFullCancelService adminNormalOrderFullCancelService =
				mock(AdminNormalOrderFullCancelService.class);
		AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService =
				mock(AdminNormalOrderPartialCancelService.class);

		long companyId = 10L;
		String operatorType = "shop";
		long operatorId = 7L;
		long orderInnerId = 9001L;
		long userId = 440L;
		String mobile = "13800138000";
		String pathOrderId = "ORD-9001";
		String rejectReason = "drug_audit_rejected_sample";

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(companyId);
		assoc.setOrderId(orderInnerId);
		assoc.setOrderType("normal_drug");
		assoc.setDeliveryStatus("PENDING");
		assoc.setUserId(userId);
		assoc.setMobile(mobile);

		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("status", "false");
		merged.put("reject_reason", rejectReason);

		AdminProcessDrugOrdersService svc =
				new AdminProcessDrugOrdersService(
						orderAssociationsMapper,
						normalOrdersMapper,
						supplierOrderMapper,
						adminNormalOrderFullCancelService,
						adminNormalOrderPartialCancelService);

		svc.processDrugOrders(companyId, operatorType, operatorId, pathOrderId, merged);

		verify(adminNormalOrderFullCancelService, times(1))
				.execute(
						eq(companyId),
						eq(operatorType),
						eq(operatorId),
						eq(0L),
						eq(userId),
						eq(mobile),
						eq(orderInnerId),
						eq(rejectReason),
						argThat(
								map ->
										map != null
												&& rejectReason.equals(
														String.valueOf(map.get("cancel_reason")))));
		verify(adminNormalOrderPartialCancelService, never())
				.execute(any(long.class), any(), any(long.class), any(long.class), any(long.class), any(long.class), any());
	}

	@Test
	void reject_nonPending_invokesPartialCancelOnly() {
		OrderAssociationsMapper orderAssociationsMapper = mock(OrderAssociationsMapper.class);
		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		AdminNormalOrderFullCancelService adminNormalOrderFullCancelService =
				mock(AdminNormalOrderFullCancelService.class);
		AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService =
				mock(AdminNormalOrderPartialCancelService.class);

		long companyId = 11L;
		String operatorType = "shop";
		long operatorId = 8L;
		long orderInnerId = 9002L;
		long userId = 441L;
		String pathOrderId = "ORD-9002";
		String rejectReason = "other_reason_sample";

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(companyId);
		assoc.setOrderId(orderInnerId);
		assoc.setOrderType("normal_drug");
		assoc.setDeliveryStatus("PARTAIL");
		assoc.setUserId(userId);
		assoc.setMobile(null);

		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);

		Map<String, Object> merged = Map.of("status", Boolean.FALSE, "reject_reason", rejectReason);

		AdminProcessDrugOrdersService svc =
				new AdminProcessDrugOrdersService(
						orderAssociationsMapper,
						normalOrdersMapper,
						supplierOrderMapper,
						adminNormalOrderFullCancelService,
						adminNormalOrderPartialCancelService);

		svc.processDrugOrders(companyId, operatorType, operatorId, pathOrderId, merged);

		verify(adminNormalOrderPartialCancelService, times(1))
				.execute(
						eq(companyId),
						eq(operatorType),
						eq(operatorId),
						eq(0L),
						eq(userId),
						eq(orderInnerId),
						eq(rejectReason));
		verify(adminNormalOrderFullCancelService, never())
				.execute(
						any(long.class),
						any(),
						any(long.class),
						any(long.class),
						any(long.class),
						any(),
						any(long.class),
						any(),
						any());
	}
}
