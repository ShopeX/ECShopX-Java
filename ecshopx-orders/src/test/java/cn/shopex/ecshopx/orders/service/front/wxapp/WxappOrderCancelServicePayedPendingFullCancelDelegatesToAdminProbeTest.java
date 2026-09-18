package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.CommunityOrderRelActivity;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.CommunityOrderRelActivityMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderFullCancelService;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderPartialCancelService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Wxapp cancel: PAYED + PENDING delegates to AdminNormalOrderFullCancelService#execute")
class WxappOrderCancelServicePayedPendingFullCancelDelegatesToAdminProbeTest {

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private CommunityOrderRelActivityMapper communityOrderRelActivityMapper;

	@Mock
	private AdminNormalOrderFullCancelService adminNormalOrderFullCancelService;

	@Mock
	private AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService;

	@Mock
	private HttpServletRequest httpServletRequest;

	private WxappOrderCancelService wxappOrderCancelService;

	@BeforeEach
	void setUp() {
		wxappOrderCancelService =
				new WxappOrderCancelService(
						orderAssociationsMapper,
						normalOrdersMapper,
						communityOrderRelActivityMapper,
						adminNormalOrderFullCancelService,
						adminNormalOrderPartialCancelService);
	}

	@Test
	void payed_pending_delivery_buyer_cancel_delegates_to_admin_normal_order_full_cancel_execute() {
		OrderAssociations assoc = payedPendingAssoc();
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		when(adminNormalOrderFullCancelService.execute(
						eq(1L),
						eq("buyer"),
						eq(0L),
						eq(0L),
						eq(99L),
						eq("13900000000"),
						eq(200L),
						eq("协商一致"),
						any(),
						eq("buyer")))
				.thenReturn(Map.of("ok", true));

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", "200");
		merged.put("cancel_reason", "协商一致");
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 1L);
		auth.put("user_id", 99L);
		auth.put("mobile", "13900000000");

		Map<String, Object> result = wxappOrderCancelService.cancelOrder(httpServletRequest, merged, auth);

		assertThat(result).containsEntry("ok", true);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> mergedCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(adminNormalOrderFullCancelService, times(1))
				.execute(
						eq(1L),
						eq("buyer"),
						eq(0L),
						eq(0L),
						eq(99L),
						eq("13900000000"),
						eq(200L),
						eq("协商一致"),
						mergedCaptor.capture(),
						eq("buyer"));
		Map<String, Object> passedMerged = mergedCaptor.getValue();
		assertThat(passedMerged.get("cancel_from")).isEqualTo("buyer");
		assertThat(passedMerged.get("order_id")).isEqualTo("200");
		assertThat(passedMerged.get("company_id")).isEqualTo(1L);
		verify(adminNormalOrderPartialCancelService, never())
				.execute(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyString());
	}

	@Test
	void payed_pending_delivery_chief_cancel_delegates_to_admin_normal_order_full_cancel_execute() {
		OrderAssociations assoc = payedPendingAssoc();
		assoc.setUserId(50L);
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);

		NormalOrders normalOrder = new NormalOrders();
		normalOrder.setOrderId(200L);
		normalOrder.setCompanyId(1L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(normalOrder);

		CommunityOrderRelActivity rel = new CommunityOrderRelActivity();
		rel.setOrderId(200L);
		rel.setChiefId(777L);
		when(communityOrderRelActivityMapper.selectById(200L)).thenReturn(rel);

		when(adminNormalOrderFullCancelService.execute(
						eq(1L),
						eq("buyer"),
						eq(0L),
						eq(0L),
						eq(50L),
						eq("13900000000"),
						eq(200L),
						eq(""),
						any(),
						eq("chief")))
				.thenReturn(Map.of("ok", true));

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", "200");
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 1L);
		auth.put("chief_id", 777L);

		Map<String, Object> result = wxappOrderCancelService.cancelOrder(httpServletRequest, merged, auth);

		assertThat(result).containsEntry("ok", true);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> mergedCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(adminNormalOrderFullCancelService, times(1))
				.execute(
						eq(1L),
						eq("buyer"),
						eq(0L),
						eq(0L),
						eq(50L),
						eq("13900000000"),
						eq(200L),
						eq(""),
						mergedCaptor.capture(),
						eq("chief"));
		assertThat(mergedCaptor.getValue().get("cancel_from")).isEqualTo("chief");
		assertThat(mergedCaptor.getValue().get("chief_id")).isEqualTo(777L);
		verify(adminNormalOrderPartialCancelService, never())
				.execute(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyString());
	}

	private static OrderAssociations payedPendingAssoc() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setOrderId(200L);
		assoc.setCompanyId(1L);
		assoc.setUserId(99L);
		assoc.setOrderType("normal");
		assoc.setOrderClass("normal");
		assoc.setOrderStatus("PAYED");
		assoc.setDeliveryStatus("PENDING");
		assoc.setMobile("13900000000");
		return assoc;
	}
}
