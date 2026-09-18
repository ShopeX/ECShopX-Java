package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappOrderCancelServiceTradeRefundPathProbeTest {

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
	void cancel_pending_invokesAdminNormalOrderFullCancelServiceExecuteOnce() {
		OrderAssociations assoc = baseAssoc();
		assoc.setDeliveryStatus("PENDING");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		when(adminNormalOrderFullCancelService.execute(
						eq(1L),
						eq("buyer"),
						eq(0L),
						eq(0L),
						eq(99L),
						eq("13900000000"),
						eq(200L),
						eq(""),
						any(),
						eq("buyer")))
				.thenReturn(Map.of());

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", "200");
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 1L);
		auth.put("user_id", 99L);
		auth.put("mobile", "13900000000");

		wxappOrderCancelService.cancelOrder(httpServletRequest, merged, auth);

		verify(adminNormalOrderFullCancelService, times(1))
				.execute(
						eq(1L),
						eq("buyer"),
						eq(0L),
						eq(0L),
						eq(99L),
						eq("13900000000"),
						eq(200L),
						eq(""),
						any(),
						eq("buyer"));
		verify(adminNormalOrderPartialCancelService, never())
				.execute(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyString());
	}

	@Test
	void cancel_nonPending_invokesAdminNormalOrderPartialCancelServiceNotFull() {
		OrderAssociations assoc = baseAssoc();
		assoc.setDeliveryStatus("PARTAIL");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		when(adminNormalOrderPartialCancelService.execute(
						eq(1L), eq("buyer"), eq(0L), eq(0L), eq(99L), eq(200L), eq("")))
				.thenReturn(Map.of());

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", "200");
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 1L);
		auth.put("user_id", 99L);
		auth.put("mobile", "13900000000");

		wxappOrderCancelService.cancelOrder(httpServletRequest, merged, auth);

		verify(adminNormalOrderPartialCancelService, times(1))
				.execute(eq(1L), eq("buyer"), eq(0L), eq(0L), eq(99L), eq(200L), eq(""));
		verify(adminNormalOrderFullCancelService, never())
				.execute(
						anyLong(),
						anyString(),
						anyLong(),
						anyLong(),
						anyLong(),
						anyString(),
						anyLong(),
						anyString(),
						any(),
						anyString());
	}

	private static OrderAssociations baseAssoc() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setOrderId(200L);
		assoc.setCompanyId(1L);
		assoc.setUserId(99L);
		assoc.setOrderType("normal");
		assoc.setOrderClass("normal");
		assoc.setOrderStatus("PAYED");
		assoc.setMobile("13900000000");
		return assoc;
	}
}
