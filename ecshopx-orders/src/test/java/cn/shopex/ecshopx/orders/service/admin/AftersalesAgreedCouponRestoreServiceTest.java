package cn.shopex.ecshopx.orders.service.admin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.common.port.order.OrderCancelUserDiscountRestorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AftersalesAgreedCouponRestoreServiceTest {

	private static final long COMPANY_ID = 1L;
	private static final long ORDER_ID = 10L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(assistant, AftersalesDetail.class);
	}

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private AftersalesDetailMapper aftersalesDetailMapper;

	@Mock
	private OrderCancelUserDiscountRestorePort orderCancelUserDiscountRestorePort;

	@Mock
	private DmCrmSettingReadPort dmCrmSettingReadPort;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private AftersalesAgreedCouponRestoreService service;

	@BeforeEach
	void setUp() {
		service =
				new AftersalesAgreedCouponRestoreService(
						normalOrdersItemsMapper,
						aftersalesDetailMapper,
						orderCancelUserDiscountRestorePort,
						dmCrmSettingReadPort,
						objectMapper);
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(false);
	}

	private static NormalOrdersItems line(long id, int num, String itemType, String discountInfo) {
		NormalOrdersItems row = new NormalOrdersItems();
		row.setId(id);
		row.setCompanyId(COMPANY_ID);
		row.setOrderId(ORDER_ID);
		row.setNum(num);
		row.setOrderItemType(itemType);
		row.setDiscountInfo(discountInfo);
		return row;
	}

	private static AftersalesDetail agreed(long subOrderId, String type, int status, int progress, int num) {
		AftersalesDetail row = new AftersalesDetail();
		row.setSubOrderId(subOrderId);
		row.setAftersalesType(type);
		row.setAftersalesStatus(status);
		row.setProgress(progress);
		row.setNum(num);
		return row;
	}

	private void stubOrderLines(List<NormalOrdersItems> lines) {
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(lines);
	}

	private void stubAftersales(List<AftersalesDetail> details) {
		when(aftersalesDetailMapper.selectList(any())).thenReturn(details);
	}

	@Test
	void restoreOnRefundAgreed_singleLineFullyCoveredByOnlyRefund_callsCallbackOnce() {
		stubOrderLines(List.of(line(5, 2, "normal", "[{\"coupon_code\":\"C1\"}]")));
		stubAftersales(List.of(agreed(5, "ONLY_REFUND", 1, 9, 2)));

		service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);

		verify(orderCancelUserDiscountRestorePort).callbackUserCard("C1", ORDER_ID);
	}

	@Test
	void restoreOnRefundAgreed_agreedQuantityInsufficient_neverRestores() {
		stubOrderLines(List.of(line(5, 3, "normal", "[{\"coupon_code\":\"C1\"}]")));
		stubAftersales(List.of(agreed(5, "ONLY_REFUND", 1, 9, 2)));

		service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);

		verify(orderCancelUserDiscountRestorePort, never()).callbackUserCard(any(), anyLong());
	}

	@Test
	void restoreOnRefundAgreed_sharedCouponSecondLineNotAgreed_neverRestores() {
		stubOrderLines(
				List.of(
						line(5, 1, "normal", "[{\"coupon_code\":\"C1\"}]"),
						line(6, 1, "normal", "[{\"coupon_code\":\"C1\"}]")));
		stubAftersales(List.of(agreed(5, "ONLY_REFUND", 1, 9, 1)));

		service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);

		verify(orderCancelUserDiscountRestorePort, never()).callbackUserCard(any(), anyLong());
	}

	@Test
	void restoreOnRefundAgreed_exchangingGoodsNotCounted_neverRestores() {
		stubOrderLines(List.of(line(5, 1, "normal", "[{\"coupon_code\":\"C1\"}]")));
		stubAftersales(List.of(agreed(5, "EXCHANGING_GOODS", 2, 4, 1)));

		service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);

		verify(orderCancelUserDiscountRestorePort, never()).callbackUserCard(any(), anyLong());
	}

	@Test
	void restoreOnRefundAgreed_giftLineIgnored_neverRestores() {
		stubOrderLines(
				List.of(
						line(5, 1, "gift", "[{\"coupon_code\":\"C1\"}]"),
						line(6, 1, "normal", null)));
		stubAftersales(List.of());

		service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);

		verify(orderCancelUserDiscountRestorePort, never()).callbackUserCard(any(), anyLong());
	}

	@Test
	void restoreOnRefundAgreed_twoCouponsOnlyOneFullyCovered_restoresCoveredOnly() {
		stubOrderLines(
				List.of(
						line(5, 1, "normal", "[{\"coupon_code\":\"C1\"}]"),
						line(6, 1, "normal", "[{\"coupon_code\":\"C2\"}]")));
		stubAftersales(List.of(agreed(5, "ONLY_REFUND", 1, 9, 1)));

		service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);

		verify(orderCancelUserDiscountRestorePort).callbackUserCard("C1", ORDER_ID);
		verify(orderCancelUserDiscountRestorePort, never()).callbackUserCard(eq("C2"), anyLong());
	}

	@Test
	void restoreOnRefundAgreed_refundGoodsStatus2Progress4_countsAsAgreed() {
		stubOrderLines(List.of(line(5, 1, "normal", "[{\"coupon_code\":\"C1\"}]")));
		stubAftersales(List.of(agreed(5, "REFUND_GOODS", 2, 4, 1)));

		service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);

		verify(orderCancelUserDiscountRestorePort).callbackUserCard("C1", ORDER_ID);
	}

	@Test
	void restoreOnRefundAgreed_dmIntegrationOpen_skipsRestore() {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(true);
		lenient()
				.when(normalOrdersItemsMapper.selectList(any()))
				.thenReturn(List.of(line(5, 1, "normal", "[{\"coupon_code\":\"C1\"}]")));
		lenient()
				.when(aftersalesDetailMapper.selectList(any()))
				.thenReturn(List.of(agreed(5, "ONLY_REFUND", 1, 9, 1)));

		service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);

		verify(orderCancelUserDiscountRestorePort, never()).callbackUserCard(any(), anyLong());
	}

	@Test
	void restoreOnRefundAgreed_dmReadThrows_doesNotPropagateOrRestore() {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID))
				.thenThrow(new RuntimeException("dm down"));
		lenient()
				.when(normalOrdersItemsMapper.selectList(any()))
				.thenReturn(List.of(line(5, 1, "normal", "[{\"coupon_code\":\"C1\"}]")));
		lenient()
				.when(aftersalesDetailMapper.selectList(any()))
				.thenReturn(List.of(agreed(5, "ONLY_REFUND", 1, 9, 1)));

		assertDoesNotThrow(() -> service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID));
		verify(orderCancelUserDiscountRestorePort, never()).callbackUserCard(any(), anyLong());
	}

	@Test
	void restoreOnRefundAgreed_zeroPurchaseQuantity_neverRestores() {
		stubOrderLines(List.of(line(5, 0, "normal", "[{\"coupon_code\":\"C1\"}]")));
		stubAftersales(List.of());

		service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);

		verify(orderCancelUserDiscountRestorePort, never()).callbackUserCard(any(), anyLong());
	}

	@Test
	void restoreOnRefundAgreed_callbackFailureOnFirstCoupon_stillRestoresSecond() {
		stubOrderLines(
				List.of(
						line(5, 1, "normal", "[{\"coupon_code\":\"C1\"}]"),
						line(6, 1, "normal", "[{\"coupon_code\":\"C2\"}]")));
		stubAftersales(
				List.of(
						agreed(5, "ONLY_REFUND", 1, 9, 1),
						agreed(6, "ONLY_REFUND", 1, 9, 1)));
		doThrow(new RuntimeException("kaquan"))
				.when(orderCancelUserDiscountRestorePort)
				.callbackUserCard("C1", ORDER_ID);

		assertDoesNotThrow(() -> service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID));

		verify(orderCancelUserDiscountRestorePort).callbackUserCard("C1", ORDER_ID);
		verify(orderCancelUserDiscountRestorePort).callbackUserCard("C2", ORDER_ID);
	}

	@Test
	void restoreOnRefundAgreed_couponCodeAsArray_expandsAndRestores() {
		stubOrderLines(List.of(line(5, 1, "normal", "[{\"coupon_code\":[\"C1\"]}]")));
		stubAftersales(List.of(agreed(5, "ONLY_REFUND", 1, 9, 1)));

		service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);

		verify(orderCancelUserDiscountRestorePort).callbackUserCard("C1", ORDER_ID);
	}

	@Test
	void restoreOnRefundAgreed_couponCodeAsNumber_expandsAndRestores() {
		stubOrderLines(List.of(line(5, 1, "normal", "[{\"coupon_code\":42}]")));
		stubAftersales(List.of(agreed(5, "ONLY_REFUND", 1, 9, 1)));

		service.restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);

		verify(orderCancelUserDiscountRestorePort).callbackUserCard("42", ORDER_ID);
	}
}
