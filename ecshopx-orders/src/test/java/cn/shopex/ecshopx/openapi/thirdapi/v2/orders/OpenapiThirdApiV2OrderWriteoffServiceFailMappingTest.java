package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.setting.PickupcodeSettingRedisService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderWriteoffV2FailException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import cn.shopex.ecshopx.orders.service.normal.OrderPickupSmsRedisVerifyService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenapiThirdApiV2OrderWriteoffServiceFailMappingTest {

	@Mock private OrderAssociationsMapper orderAssociationsMapper;
	@Mock private PickupcodeSettingRedisService pickupcodeSettingRedisService;
	@Mock private OrderPickupSmsRedisVerifyService orderPickupSmsRedisVerifyService;
	@Mock private NormalOrdersMapper normalOrdersMapper;
	@Mock private NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;

	@InjectMocks private OpenapiThirdApiV2OrderWriteoffService service;

	private static final long COMPANY_ID = 9L;
	private static final long ORDER_ID = 501L;

	@Test
	void emptyOrderIdRaw_throwsE5001() {
		OpenapiOrderWriteoffV2FailException ex =
				assertThrows(
						OpenapiOrderWriteoffV2FailException.class,
						() -> service.executeOpenapiWriteoff(COMPANY_ID, "  ", null));
		assertEquals(OpenapiErrorCode.SERVICE_MISSING_PARAMS, ex.getOpenapiCode());
		assertEquals("请填写订单编号", ex.getMessage());
	}

	@Test
	void associationNull_throwsE5301() {
		when(orderAssociationsMapper.selectOne(any())).thenReturn(null);

		OpenapiOrderWriteoffV2FailException ex =
				assertThrows(
						OpenapiOrderWriteoffV2FailException.class,
						() -> service.executeOpenapiWriteoff(COMPANY_ID, "501", null));
		assertEquals(OpenapiErrorCode.ORDER_NOT_FOUND, ex.getOpenapiCode());
		assertEquals("订单相应的明细不存在", ex.getMessage());
	}

	@Test
	void nonZitiReceiptType_throwsE5301WithZitiMessage() {
		stubAssociationAndPickupOff();
		NormalOrders normal = validNormal();
		normal.setReceiptType("express");
		when(normalOrdersMapper.selectOne(any())).thenReturn(normal);

		OpenapiOrderWriteoffV2FailException ex =
				assertThrows(
						OpenapiOrderWriteoffV2FailException.class,
						() -> service.executeOpenapiWriteoff(COMPANY_ID, "501", null));
		assertEquals(OpenapiErrorCode.ORDER_NOT_FOUND, ex.getOpenapiCode());
		assertEquals("自提订单相应的明细不存在", ex.getMessage());
	}

	@Test
	void orderStatusDone_throwsE5310StatusNotAllowed() {
		stubAssociationAndPickupOff();
		NormalOrders normal = validNormal();
		normal.setOrderStatus("DONE");
		when(normalOrdersMapper.selectOne(any())).thenReturn(normal);

		OpenapiOrderWriteoffV2FailException ex =
				assertThrows(
						OpenapiOrderWriteoffV2FailException.class,
						() -> service.executeOpenapiWriteoff(COMPANY_ID, "501", null));
		assertEquals(OpenapiErrorCode.ORDER_HANDLE_ERROR, ex.getOpenapiCode());
		assertEquals("自提订单状态不正确，不能进行操作", ex.getMessage());
	}

	@Test
	void pickupOnWithEmptyPickupcode_throwsE5001() {
		stubAssociation();
		when(pickupcodeSettingRedisService.handle(eq(COMPANY_ID), eq(null)))
				.thenReturn(Map.of("pickupcode_status", true));

		OpenapiOrderWriteoffV2FailException ex =
				assertThrows(
						OpenapiOrderWriteoffV2FailException.class,
						() -> service.executeOpenapiWriteoff(COMPANY_ID, "501", "  "));
		assertEquals(OpenapiErrorCode.SERVICE_MISSING_PARAMS, ex.getOpenapiCode());
		assertEquals("请填写提货码", ex.getMessage());
	}

	@Test
	void pickupOnWithPickupcodeZero_doesNotThrowMissingPickupcode() {
		stubAssociationAndPickupOn();
		NormalOrders normal = validNormal();
		when(normalOrdersMapper.selectOne(any())).thenReturn(normal);

		service.executeOpenapiWriteoff(COMPANY_ID, "501", "0");

		verify(normalOrderZitiWriteoffService)
				.orderZitiWriteoffForOpenapi(eq(COMPANY_ID), eq(ORDER_ID), eq(true), eq("0"));
	}

	@Test
	void pickupOnWithoutMobile_throwsE5310() {
		stubAssociationAndPickupOn();
		NormalOrders normal = validNormal();
		normal.setMobile("");
		when(normalOrdersMapper.selectOne(any())).thenReturn(normal);

		OpenapiOrderWriteoffV2FailException ex =
				assertThrows(
						OpenapiOrderWriteoffV2FailException.class,
						() -> service.executeOpenapiWriteoff(COMPANY_ID, "501", "1234"));
		assertEquals(OpenapiErrorCode.ORDER_HANDLE_ERROR, ex.getOpenapiCode());
		assertEquals("未查询到提货人联系手机！", ex.getMessage());
	}

	@Test
	void pickupVerifyFailure_throwsE5310WithPunctuation() {
		stubAssociationAndPickupOn();
		NormalOrders normal = validNormal();
		when(normalOrdersMapper.selectOne(any())).thenReturn(normal);
		doThrow(new ResourceException("提货码验证错误"))
				.when(orderPickupSmsRedisVerifyService)
				.verifyAndConsumePickupCode(eq(ORDER_ID), eq("13800000000"), eq("1234"));

		OpenapiOrderWriteoffV2FailException ex =
				assertThrows(
						OpenapiOrderWriteoffV2FailException.class,
						() -> service.executeOpenapiWriteoff(COMPANY_ID, "501", "1234"));
		assertEquals(OpenapiErrorCode.ORDER_HANDLE_ERROR, ex.getOpenapiCode());
		assertEquals("提货码验证错误！", ex.getMessage());
		verify(normalOrderZitiWriteoffService, never())
				.orderZitiWriteoffForOpenapi(anyLong(), anyLong(), anyBoolean(), anyString());
	}

	private void stubAssociation() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(COMPANY_ID);
		assoc.setOrderId(ORDER_ID);
		assoc.setOrderType("normal");
		assoc.setOrderClass("normal");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
	}

	private void stubAssociationAndPickupOff() {
		stubAssociation();
		when(pickupcodeSettingRedisService.handle(eq(COMPANY_ID), eq(null)))
				.thenReturn(Map.of("pickupcode_status", false));
	}

	private void stubAssociationAndPickupOn() {
		stubAssociation();
		when(pickupcodeSettingRedisService.handle(eq(COMPANY_ID), eq(null)))
				.thenReturn(Map.of("pickupcode_status", true));
	}

	private static NormalOrders validNormal() {
		NormalOrders normal = new NormalOrders();
		normal.setCompanyId(COMPANY_ID);
		normal.setOrderId(ORDER_ID);
		normal.setReceiptType("ziti");
		normal.setOrderStatus("PAYED");
		normal.setZitiStatus("PENDING");
		normal.setPayStatus("PAYED");
		normal.setCancelStatus("NO_APPLY_CANCEL");
		normal.setMobile("13800000000");
		return normal;
	}
}
