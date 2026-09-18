package cn.shopex.ecshopx.orders.service.espier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderPartialCancelService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrdersCancelUploadImportRowServicePartailBranchTest {

	@Mock
	NormalOrdersMapper normalOrdersMapper;

	@Mock
	OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	CancelOrdersMapper cancelOrdersMapper;

	@Mock
	AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService;

	@InjectMocks
	NormalOrdersCancelUploadImportRowService service;

	@Test
	void acceptRow_whenDeliveryPartail_delegatesToAdminNormalOrderPartialCancelServiceOnceWithMappedArgs() {
		long companyId = 10L;
		long operatorId = 7L;
		String operatorType = "supplier";
		NormalOrders order = new NormalOrders();
		order.setOrderId(9001L);
		order.setCompanyId(companyId);
		order.setOrderType("normal");
		order.setDeliveryStatus("PARTAIL");
		order.setUserId(100L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("order_id", "9001");
		row.put("cancel_reason", " buyer changed mind ");

		service.acceptRow(companyId, operatorId, 0L, 0L, 0L, row, operatorType);

		verify(adminNormalOrderPartialCancelService)
				.execute(eq(companyId), eq("supplier"), eq(operatorId), eq(7L), eq(100L), eq(9001L), eq("buyer changed mind"));
		verify(cancelOrdersMapper, never()).insert(ArgumentMatchers.<CancelOrders>any());
	}

	@Test
	void acceptRow_whenDeliveryNotPartail_doesNotInvokeAdminNormalOrderPartialCancelService() {
		long companyId = 10L;
		NormalOrders order = new NormalOrders();
		order.setOrderId(8002L);
		order.setCompanyId(companyId);
		order.setOrderType("normal");
		order.setDeliveryStatus("SHIPPED");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("order_id", "8002");
		row.put("cancel_reason", "x");

		assertThrows(
				BadRequestException.class,
				() -> service.acceptRow(companyId, 1L, 0L, 0L, 0L, row, "admin"));

		verify(adminNormalOrderPartialCancelService, never()).execute(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyString());
	}
}
