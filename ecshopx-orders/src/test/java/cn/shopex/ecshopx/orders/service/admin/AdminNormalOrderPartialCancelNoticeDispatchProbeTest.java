package cn.shopex.ecshopx.orders.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminNormalOrderPartialCancelNoticeDispatchProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), SupplierOrder.class);
	}

	@Mock
	NormalOrdersMapper normalOrdersMapper;

	@Mock
	NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	SupplierOrderMapper supplierOrderMapper;

	@Mock
	AftersalesShopPartialCancelCreateService aftersalesShopPartialCancelCreateService;

	@Mock
	OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Mock
	SendAfterSaleWaitDealNoticeJobDispatchPublisher noticePublisher;

	@InjectMocks
	AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService;

	@Test
	void partialCancel_afterCreateCommitted_invokesNoticePublisherOnceWithCompanyAndBn() {
		long companyId = 77L;
		long orderId = 88001L;
		long userId = 990L;
		long expectedAftersalesBn = 202605067778889L;

		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setUserId(userId);
		order.setOrderStatus("PAYED");
		order.setDeliveryStatus("PARTAIL");
		order.setType(0);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		NormalOrdersItems lineBefore = new NormalOrdersItems();
		lineBefore.setCompanyId(companyId);
		lineBefore.setOrderId(orderId);
		lineBefore.setId(501L);
		lineBefore.setNum(10);
		lineBefore.setDeliveryItemNum(5);
		lineBefore.setSupplierId(0);
		lineBefore.setCancelItemNum(0);

		NormalOrdersItems lineAfter = new NormalOrdersItems();
		lineAfter.setCompanyId(companyId);
		lineAfter.setOrderId(orderId);
		lineAfter.setId(501L);
		lineAfter.setNum(10);
		lineAfter.setDeliveryItemNum(5);
		lineAfter.setSupplierId(0);
		lineAfter.setCancelItemNum(5);

		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(lineBefore)).thenReturn(List.of(lineAfter));
		when(normalOrdersItemsMapper.selectOne(any())).thenReturn(lineBefore);
		when(normalOrdersItemsMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> createOut = new LinkedHashMap<>();
		createOut.put("aftersales_bn", expectedAftersalesBn);
		createOut.put("refund_fee", 50);
		createOut.put("refund_point", 0);
		when(aftersalesShopPartialCancelCreateService.createForShopPartialCancel(
						eq(companyId),
						eq(orderId),
						eq(userId),
						eq(0L),
						eq("admin"),
						eq(2L),
						eq("买家取消"),
						any()))
				.thenReturn(createOut);

		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("order_finish_time", 7);
		when(orderValiditySettingRedisReadService.readPlatformSetting(eq(companyId))).thenReturn(setting);

		adminNormalOrderPartialCancelService.execute(companyId, "admin", 2L, 0L, userId, orderId, "买家取消");

		verify(noticePublisher, times(1)).publish(eq(companyId), eq(expectedAftersalesBn));
	}

	@Test
	void partialCancel_whenAutoAftersalesEnabled_invokesAutoApproveOnlyRefundOnce() {
		long companyId = 77L;
		long orderId = 88001L;
		long userId = 990L;
		long expectedAftersalesBn = 202605067778889L;
		int refundFee = 50;
		int refundPoint = 0;

		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setUserId(userId);
		order.setOrderStatus("PAYED");
		order.setDeliveryStatus("PARTAIL");
		order.setType(0);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		NormalOrdersItems lineBefore = new NormalOrdersItems();
		lineBefore.setCompanyId(companyId);
		lineBefore.setOrderId(orderId);
		lineBefore.setId(501L);
		lineBefore.setNum(10);
		lineBefore.setDeliveryItemNum(5);
		lineBefore.setSupplierId(0);
		lineBefore.setCancelItemNum(0);

		NormalOrdersItems lineAfter = new NormalOrdersItems();
		lineAfter.setCompanyId(companyId);
		lineAfter.setOrderId(orderId);
		lineAfter.setId(501L);
		lineAfter.setNum(10);
		lineAfter.setDeliveryItemNum(5);
		lineAfter.setSupplierId(0);
		lineAfter.setCancelItemNum(5);

		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(lineBefore)).thenReturn(List.of(lineAfter));
		when(normalOrdersItemsMapper.selectOne(any())).thenReturn(lineBefore);
		when(normalOrdersItemsMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> createOut = new LinkedHashMap<>();
		createOut.put("aftersales_bn", expectedAftersalesBn);
		createOut.put("refund_fee", refundFee);
		createOut.put("refund_point", refundPoint);
		when(aftersalesShopPartialCancelCreateService.createForShopPartialCancel(
						eq(companyId),
						eq(orderId),
						eq(userId),
						eq(0L),
						eq("admin"),
						eq(2L),
						eq("买家取消"),
						any()))
				.thenReturn(createOut);

		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("order_finish_time", 7);
		setting.put("auto_aftersales", Boolean.TRUE);
		when(orderValiditySettingRedisReadService.readPlatformSetting(eq(companyId))).thenReturn(setting);

		adminNormalOrderPartialCancelService.execute(companyId, "admin", 2L, 0L, userId, orderId, "买家取消");

		verify(aftersalesShopPartialCancelCreateService, times(1))
				.createForShopPartialCancel(
						eq(companyId),
						eq(orderId),
						eq(userId),
						eq(0L),
						eq("admin"),
						eq(2L),
						eq("买家取消"),
						any());
		verify(aftersalesShopPartialCancelCreateService, times(1))
				.autoApproveOnlyRefund(
						eq(companyId),
						eq(expectedAftersalesBn),
						eq(refundFee),
						eq(refundPoint),
						eq(0),
						eq("admin"),
						eq(2L),
						eq(userId));
		verify(noticePublisher, times(1)).publish(eq(companyId), eq(expectedAftersalesBn));
		verifyNoMoreInteractions(aftersalesShopPartialCancelCreateService);
	}
}
