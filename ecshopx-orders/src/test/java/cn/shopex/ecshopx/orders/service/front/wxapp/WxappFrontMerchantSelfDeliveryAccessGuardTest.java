package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.port.WxappMemberSelfDeliveryStaffListPort;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappFrontMerchantSelfDeliveryAccessGuardTest {

	@Mock
	private MemberAccountService memberAccountService;

	@Mock
	private WxappMemberSelfDeliveryStaffListPort selfDeliveryStaffListPort;

	@Mock
	private OperatorsMapper operatorsMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private OrdersDeliveryMapper ordersDeliveryMapper;

	private WxappFrontMerchantSelfDeliveryAccessGuard guard;

	@BeforeEach
	void setUp() {
		guard =
				new WxappFrontMerchantSelfDeliveryAccessGuard(
						memberAccountService,
						selfDeliveryStaffListPort,
						operatorsMapper,
						normalOrdersMapper,
						ordersDeliveryMapper,
						new ObjectMapper());
	}

	@Test
	void assertAuthorizedForOrder_whenNoSelfDeliveryStaffIdentity_throwsForbidden() {
		when(memberAccountService.getMemberInfo(55L, 100L)).thenReturn(Map.of("mobile", "13800000000"));
		when(selfDeliveryStaffListPort.listByPlainMobile(100L, "13800000000")).thenReturn(List.of());

		NormalOrders order = merchantOrder(200L, 9L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		assertThatThrownBy(() -> guard.assertAuthorizedForOrder(100L, 55L, 200L))
				.isInstanceOf(ForbiddenException.class)
				.hasMessageContaining("无权");
		verify(operatorsMapper, never()).selectList(any());
	}

	@Test
	void assertAuthorizedForOrder_whenDistributorNotInStaffScope_throwsForbidden() {
		when(memberAccountService.getMemberInfo(55L, 100L)).thenReturn(Map.of("mobile", "13800000000"));
		when(selfDeliveryStaffListPort.listByPlainMobile(100L, "13800000000"))
				.thenReturn(List.of(Map.of("operator_id", 77L)));
		Operators staff = new Operators();
		staff.setOperatorId(77L);
		staff.setDistributorIds("[{\"distributor_id\":8}]");
		when(operatorsMapper.selectList(any())).thenReturn(List.of(staff));
		when(normalOrdersMapper.selectOne(any())).thenReturn(merchantOrder(200L, 9L));

		assertThatThrownBy(() -> guard.assertAuthorizedForOrder(100L, 55L, 200L))
				.isInstanceOf(ForbiddenException.class)
				.hasMessageContaining("无权");
	}

	@Test
	void assertAuthorizedForOrder_whenStaffMatchesOrderDistributor_passes() {
		when(memberAccountService.getMemberInfo(55L, 100L)).thenReturn(Map.of("mobile", "13800000000"));
		when(selfDeliveryStaffListPort.listByPlainMobile(100L, "13800000000"))
				.thenReturn(List.of(Map.of("operator_id", 77L)));
		Operators staff = new Operators();
		staff.setOperatorId(77L);
		staff.setDistributorIds("[{\"distributor_id\":9}]");
		when(operatorsMapper.selectList(any())).thenReturn(List.of(staff));
		when(normalOrdersMapper.selectOne(any())).thenReturn(merchantOrder(200L, 9L));

		assertThatCode(() -> guard.assertAuthorizedForOrder(100L, 55L, 200L)).doesNotThrowAnyException();
	}

	@Test
	void assertAuthorizedForOrder_whenReceiptTypeNotMerchant_throwsForbidden() {
		NormalOrders order = merchantOrder(200L, 9L);
		order.setReceiptType("logistics");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		assertThatThrownBy(() -> guard.assertAuthorizedForOrder(100L, 55L, 200L))
				.isInstanceOf(ForbiddenException.class)
				.hasMessageContaining("无权");
		verify(memberAccountService, never()).getMemberInfo(eq(55L), eq(100L));
	}

	@Test
	void assertAuthorizedForOrder_whenOrderMissing_throwsResource() {
		when(normalOrdersMapper.selectOne(any())).thenReturn(null);

		assertThatThrownBy(() -> guard.assertAuthorizedForOrder(100L, 55L, 200L))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("不存在");
	}

	@Test
	void assertAuthorizedForDeliveryId_resolvesOrderThenAppliesSameRules() {
		OrdersDelivery delivery = new OrdersDelivery();
		delivery.setOrdersDeliveryId(501L);
		delivery.setCompanyId(100L);
		delivery.setOrderId(200L);
		when(ordersDeliveryMapper.selectOne(any())).thenReturn(delivery);

		when(memberAccountService.getMemberInfo(55L, 100L)).thenReturn(Map.of("mobile", "13800000000"));
		when(selfDeliveryStaffListPort.listByPlainMobile(100L, "13800000000"))
				.thenReturn(List.of(Map.of("operator_id", 77L)));
		Operators staff = new Operators();
		staff.setOperatorId(77L);
		staff.setDistributorIds("[{\"distributor_id\":9}]");
		when(operatorsMapper.selectList(any())).thenReturn(List.of(staff));
		when(normalOrdersMapper.selectOne(any())).thenReturn(merchantOrder(200L, 9L));

		assertThatCode(() -> guard.assertAuthorizedForDeliveryId(100L, 55L, "501"))
				.doesNotThrowAnyException();
	}

	private static NormalOrders merchantOrder(long orderId, long distributorId) {
		NormalOrders order = new NormalOrders();
		order.setCompanyId(100L);
		order.setOrderId(orderId);
		order.setDistributorId(distributorId);
		order.setReceiptType("merchant");
		return order;
	}
}
