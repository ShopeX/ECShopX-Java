package cn.shopex.ecshopx.systemlink.service.wdterp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.wdterp.WdtErpInventoryPort;
import cn.shopex.ecshopx.common.port.wdterp.WdtErpLogisticsPort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.promotions.service.WdtErpSyncInventoryPromotionSupport;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDeliveryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WdtErpSyncServiceTest {

	private WdtErpSyncService service;

	@Mock
	private WdtErpLogisticsPort wdtErpLogisticsPort;

	@Mock
	private WdtErpInventoryPort wdtErpInventoryPort;

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	@SuppressWarnings("rawtypes")
	private ValueOperations valueOps;

	@Mock
	private DistributorMapper distributorMapper;

	@Mock
	private DistributorItemsMapper distributorItemsMapper;

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private AdminNormalOrderDetailService adminNormalOrderDetailService;

	@Mock
	private AdminOrderDeliveryService adminOrderDeliveryService;

	@Mock
	private WdtErpSyncInventoryPromotionSupport wdtErpSyncInventoryPromotionSupport;

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private ItemStoreService itemStoreService;

	@Mock
	private ShopMenuService shopMenuService;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOps);
		service = new WdtErpSyncService(
				wdtErpLogisticsPort,
				wdtErpInventoryPort,
				companysRedisTemplate,
				distributorMapper,
				distributorItemsMapper,
				orderAssociationsMapper,
				adminNormalOrderDetailService,
				adminOrderDeliveryService,
				objectMapper,
				wdtErpSyncInventoryPromotionSupport,
				itemsRepository,
				itemStoreService,
				shopMenuService,
				normalOrdersItemsMapper);
	}

	private void baseSetting() {
		when(companysRedisTemplate.keys(WdtErpSyncService.WDT_REDIS_PREFIX + "*"))
				.thenReturn(Set.of("WdtErpSetting:ab"));
		when(valueOps.get("WdtErpSetting:ab"))
				.thenReturn("{\"is_open\":true,\"company_id\":1,\"sid\":\"s\",\"app_key\":\"k\",\"app_secret\":\"a:b\",\"shop_no\":\"SN\"}");
		when(distributorMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
	}

	@Test
	void branch1_redisEmpty_noPort() {
		when(companysRedisTemplate.keys(WdtErpSyncService.WDT_REDIS_PREFIX + "*")).thenReturn(Set.of());
		service.scheduleSyncLogistics();
		verify(wdtErpLogisticsPort, never()).getWaitSyncPage(anyLong(), anyString(), anyInt(), any(), any(), any());
		verify(distributorMapper, never()).selectList(any(LambdaQueryWrapper.class));
	}

	@Test
	void branch2_isOpenFalse_skips() {
		when(companysRedisTemplate.keys(WdtErpSyncService.WDT_REDIS_PREFIX + "*"))
				.thenReturn(Set.of("WdtErpSetting:ab"));
		when(valueOps.get("WdtErpSetting:ab")).thenReturn("{\"is_open\":false,\"company_id\":1}");
		service.scheduleSyncLogistics();
		verify(wdtErpLogisticsPort, never()).getWaitSyncPage(anyLong(), anyString(), anyInt(), any(), any(), any());
	}

	@Test
	void branch5_noShopNos_zeroFetch() {
		baseSetting();
		when(valueOps.get("WdtErpSetting:ab"))
				.thenReturn("{\"is_open\":true,\"company_id\":1,\"sid\":\"s\",\"app_key\":\"k\",\"app_secret\":\"a:b\",\"shop_no\":\"\"}");
		service.scheduleSyncLogistics();
		verify(wdtErpLogisticsPort, never()).getWaitSyncPage(anyLong(), anyString(), anyInt(), any(), any(), any());
	}

	@Test
	void branch3_open_oneEmptyPage_fetchesOnce() {
		baseSetting();
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(List.of());
		service.scheduleSyncLogistics();
		verify(wdtErpLogisticsPort, times(1)).getWaitSyncPage(eq(1L), eq("SN"), eq(0), any(), any(), any());
	}

	@Test
	void sixA_emptyList_noAck() {
		baseSetting();
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(List.of());
		service.scheduleSyncLogistics();
		verify(wdtErpLogisticsPort, never()).acknowledgeSync(anyLong(), anyList(), any(), any(), any());
	}

	@Test
	@DisplayName("分支6-C：单页2条物流，acknowledgeSync 一次且 sync 列表长度2")
	void sixC_singlePageTwoLogisticsRows_ackOnce_listLength2() {
		baseSetting();
		Map<String, Object> logi1 = new LinkedHashMap<>();
		logi1.put("tid", 901L);
		logi1.put("sync_id", 91L);
		logi1.put("logistics_no", "N1");
		logi1.put("logistics_code", "C1");
		logi1.put("is_part_sync", 0);
		Map<String, Object> logi2 = new LinkedHashMap<>();
		logi2.put("tid", 902L);
		logi2.put("sync_id", 92L);
		logi2.put("logistics_no", "N2");
		logi2.put("logistics_code", "C2");
		logi2.put("is_part_sync", 0);
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(new ArrayList<>(List.of(logi1, logi2)), List.of());
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
		service.scheduleSyncLogistics();
		ArgumentCaptor<List<Map<String, Object>>> c = ArgumentCaptor.captor();
		verify(wdtErpLogisticsPort, times(1)).acknowledgeSync(eq(1L), c.capture(), any(), any(), any());
		assertThat(c.getValue()).hasSize(2);
	}

	@Test
	void sixB1a_orderMissing_noDelivery() {
		baseSetting();
		Map<String, Object> logi = new LinkedHashMap<>();
		logi.put("tid", 9999L);
		logi.put("sync_id", 1L);
		logi.put("logistics_no", "N");
		logi.put("logistics_code", "C");
		logi.put("is_part_sync", 0);
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(new ArrayList<>(List.of(logi)), List.of());
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
		service.scheduleSyncLogistics();
		verify(adminOrderDeliveryService, never()).delivery(anyLong(), anyString(), anyLong(), any());
		ArgumentCaptor<List<Map<String, Object>>> c = ArgumentCaptor.captor();
		verify(wdtErpLogisticsPort).acknowledgeSync(eq(1L), c.capture(), any(), any(), any());
		assertThat(c.getValue().get(0).get("status")).isEqualTo(2);
	}

	@Test
	void sixB1b_done_noDelivery_acked0() {
		baseSetting();
		Map<String, Object> logi = new LinkedHashMap<>();
		logi.put("tid", 200L);
		logi.put("sync_id", 2L);
		logi.put("logistics_no", "N");
		logi.put("logistics_code", "C");
		logi.put("is_part_sync", 0);
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(new ArrayList<>(List.of(logi)), List.of());
		OrderAssociations a = new OrderAssociations();
		a.setOrderId(200L);
		a.setCompanyId(1L);
		a.setDeliveryStatus("DONE");
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(a);
		service.scheduleSyncLogistics();
		verify(adminOrderDeliveryService, never()).delivery(anyLong(), anyString(), anyLong(), any());
		ArgumentCaptor<List<Map<String, Object>>> c = ArgumentCaptor.captor();
		verify(wdtErpLogisticsPort).acknowledgeSync(anyLong(), c.capture(), any(), any(), any());
		assertThat(c.getValue().get(0).get("status")).isEqualTo(0);
	}

	@Test
	void sixB1g_resourceException_status2() {
		baseSetting();
		Map<String, Object> logi = new LinkedHashMap<>();
		logi.put("tid", 300L);
		logi.put("sync_id", 3L);
		logi.put("logistics_no", "N");
		logi.put("logistics_code", "C");
		logi.put("is_part_sync", 0);
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(new ArrayList<>(List.of(logi)), List.of());
		OrderAssociations a = new OrderAssociations();
		a.setOrderId(300L);
		a.setCompanyId(1L);
		a.setDeliveryStatus("PENDING");
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(a);
		Map<String, Object> orderInfo = buildOrderInfoWithItem(300L, 10L, "PENDING");
		when(adminNormalOrderDetailService.buildOrderBundle(eq(1L), eq("300"), eq(false)))
				.thenReturn(Map.of("orderInfo", orderInfo));
		when(adminOrderDeliveryService.delivery(anyLong(), anyString(), anyLong(), any()))
				.thenThrow(new ResourceException("x"));
		service.scheduleSyncLogistics();
		ArgumentCaptor<List<Map<String, Object>>> c = ArgumentCaptor.captor();
		verify(wdtErpLogisticsPort).acknowledgeSync(anyLong(), c.capture(), any(), any(), any());
		assertThat(c.getValue().get(0).get("status")).isEqualTo(2);
	}

	@Test
	@DisplayName("分支6-B-1：delivery 抛 RuntimeException 吞并回写 WDT status=2，不向外冒泡")
	void sixB1_runtimeException_doOrderStatus2_doesNotPropagate() {
		baseSetting();
		Map<String, Object> logi = new LinkedHashMap<>();
		logi.put("tid", 350L);
		logi.put("sync_id", 35L);
		logi.put("logistics_no", "N");
		logi.put("logistics_code", "C");
		logi.put("is_part_sync", 0);
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(new ArrayList<>(List.of(logi)), List.of());
		OrderAssociations a = new OrderAssociations();
		a.setOrderId(350L);
		a.setCompanyId(1L);
		a.setDeliveryStatus("PENDING");
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(a);
		Map<String, Object> orderInfo = buildOrderInfoWithItem(350L, 10L, "PENDING");
		when(adminNormalOrderDetailService.buildOrderBundle(eq(1L), eq("350"), eq(false)))
				.thenReturn(Map.of("orderInfo", orderInfo));
		when(adminOrderDeliveryService.delivery(anyLong(), anyString(), anyLong(), any()))
				.thenThrow(new RuntimeException("runtime-from-delivery"));
		assertThatCode(() -> service.scheduleSyncLogistics()).doesNotThrowAnyException();
		ArgumentCaptor<List<Map<String, Object>>> c = ArgumentCaptor.captor();
		verify(wdtErpLogisticsPort).acknowledgeSync(anyLong(), c.capture(), any(), any(), any());
		assertThat(c.getValue().get(0).get("status")).isEqualTo(2);
	}

	@Test
	void sixB1_deliverySuccess_status0() {
		baseSetting();
		Map<String, Object> logi = new LinkedHashMap<>();
		logi.put("tid", 400L);
		logi.put("sync_id", 40L);
		logi.put("logistics_no", "N");
		logi.put("logistics_code", "C");
		logi.put("is_part_sync", 0);
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(new ArrayList<>(List.of(logi)), List.of());
		OrderAssociations a = new OrderAssociations();
		a.setOrderId(400L);
		a.setCompanyId(1L);
		a.setDeliveryStatus("PENDING");
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(a);
		Map<String, Object> orderInfo = buildOrderInfoWithItem(400L, 20L, "PENDING");
		when(adminNormalOrderDetailService.buildOrderBundle(eq(1L), eq("400"), eq(false)))
				.thenReturn(Map.of("orderInfo", orderInfo));
		Map<String, Object> okR = new LinkedHashMap<>();
		okR.put("ok", true);
		when(adminOrderDeliveryService.delivery(anyLong(), anyString(), anyLong(), any())).thenReturn(okR);
		service.scheduleSyncLogistics();
		ArgumentCaptor<List<Map<String, Object>>> c = ArgumentCaptor.captor();
		verify(wdtErpLogisticsPort).acknowledgeSync(anyLong(), c.capture(), any(), any(), any());
		assertThat(c.getValue().get(0).get("status")).isEqualTo(0);
	}

	@Test
	void sixB1f_sepInfoEmpty_stillDelivers() {
		baseSetting();
		Map<String, Object> logi = new LinkedHashMap<>();
		logi.put("tid", 500L);
		logi.put("sync_id", 50L);
		logi.put("logistics_no", "N");
		logi.put("logistics_code", "C");
		logi.put("is_part_sync", 0);
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(new ArrayList<>(List.of(logi)), List.of());
		OrderAssociations a = new OrderAssociations();
		a.setOrderId(500L);
		a.setCompanyId(1L);
		a.setDeliveryStatus("PENDING");
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(a);
		Map<String, Object> orderInfo = buildOrderInfoWithItem(500L, 21L, "DONE");
		Map<String, Object> it2 = new LinkedHashMap<>();
		it2.put("id", 22L);
		it2.put("delivery_status", "DONE");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) orderInfo.get("items");
		items.add(it2);
		when(adminNormalOrderDetailService.buildOrderBundle(eq(1L), eq("500"), eq(false)))
				.thenReturn(Map.of("orderInfo", orderInfo));
		when(adminOrderDeliveryService.delivery(anyLong(), anyString(), anyLong(), any()))
				.thenReturn(new LinkedHashMap<>());
		service.scheduleSyncLogistics();
		verify(adminOrderDeliveryService).delivery(eq(1L), eq("system"), eq(0L), any());
	}

	@Test
	void sixB1d_partialSync_oids() throws Exception {
		baseSetting();
		Map<String, Object> logi = new LinkedHashMap<>();
		logi.put("tid", 600L);
		logi.put("sync_id", 60L);
		logi.put("logistics_no", "N");
		logi.put("logistics_code", "C");
		logi.put("is_part_sync", 1);
		logi.put("oids", "10,20");
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(new ArrayList<>(List.of(logi)), List.of());
		OrderAssociations a = new OrderAssociations();
		a.setOrderId(600L);
		a.setCompanyId(1L);
		a.setDeliveryStatus("PENDING");
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(a);
		Map<String, Object> oi = new LinkedHashMap<>();
		oi.put("company_id", 1L);
		oi.put("order_id", 600L);
		List<Map<String, Object>> items = new ArrayList<>();
		Map<String, Object> i10 = new LinkedHashMap<>();
		i10.put("id", 10L);
		i10.put("delivery_status", "PENDING");
		i10.put("num", 1);
		Map<String, Object> i20 = new LinkedHashMap<>();
		i20.put("id", 20L);
		i20.put("delivery_status", "PENDING");
		i20.put("num", 1);
		items.add(i10);
		items.add(i20);
		oi.put("items", items);
		when(adminNormalOrderDetailService.buildOrderBundle(eq(1L), eq("600"), eq(false)))
				.thenReturn(Map.of("orderInfo", oi));
		Map<String, Object> okR = new LinkedHashMap<>();
		when(adminOrderDeliveryService.delivery(anyLong(), anyString(), anyLong(), any())).thenReturn(okR);
		service.scheduleSyncLogistics();
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.captor();
		verify(adminOrderDeliveryService).delivery(eq(1L), eq("system"), eq(0L), cap.capture());
		JsonNode sep = objectMapper.readTree(cap.getValue().get("sepInfo").toString());
		assertThat(sep).hasSize(2);
	}

	@Test
	void sixB1e_skipsNonPending() throws Exception {
		baseSetting();
		Map<String, Object> logi = new LinkedHashMap<>();
		logi.put("tid", 700L);
		logi.put("sync_id", 70L);
		logi.put("logistics_no", "N");
		logi.put("logistics_code", "C");
		logi.put("is_part_sync", 0);
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(new ArrayList<>(List.of(logi)), List.of());
		OrderAssociations a = new OrderAssociations();
		a.setOrderId(700L);
		a.setCompanyId(1L);
		a.setDeliveryStatus("PENDING");
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(a);
		Map<String, Object> oi = new LinkedHashMap<>();
		oi.put("company_id", 1L);
		oi.put("order_id", 700L);
		List<Map<String, Object>> items = new ArrayList<>();
		Map<String, Object> i1 = new LinkedHashMap<>();
		i1.put("id", 1L);
		i1.put("delivery_status", "PENDING");
		i1.put("num", 1);
		Map<String, Object> i2 = new LinkedHashMap<>();
		i2.put("id", 2L);
		i2.put("delivery_status", "OTHER");
		i2.put("num", 1);
		items.add(i1);
		items.add(i2);
		oi.put("items", items);
		when(adminNormalOrderDetailService.buildOrderBundle(eq(1L), eq("700"), eq(false)))
				.thenReturn(Map.of("orderInfo", oi));
		when(adminOrderDeliveryService.delivery(anyLong(), anyString(), anyLong(), any()))
				.thenReturn(new LinkedHashMap<>());
		service.scheduleSyncLogistics();
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.captor();
		verify(adminOrderDeliveryService).delivery(eq(1L), eq("system"), eq(0L), cap.capture());
		JsonNode sep = objectMapper.readTree(cap.getValue().get("sepInfo").toString());
		assertThat(sep).hasSize(1);
	}

	@Test
	void sixD_multiPage() {
		baseSetting();
		Map<String, Object> row1 = new LinkedHashMap<>();
		row1.put("tid", 800L);
		row1.put("sync_id", 81L);
		row1.put("logistics_no", "N");
		row1.put("logistics_code", "C");
		row1.put("is_part_sync", 0);
		OrderAssociations a = new OrderAssociations();
		a.setOrderId(800L);
		a.setCompanyId(1L);
		a.setDeliveryStatus("PENDING");
		Map<String, Object> orderInfo = buildOrderInfoWithItem(800L, 1L, "PENDING");
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), eq("SN"), anyInt(), any(), any(), any()))
				.thenReturn(new ArrayList<>(List.of(row1)), List.of());
		when(orderAssociationsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(a);
		when(adminNormalOrderDetailService.buildOrderBundle(eq(1L), eq("800"), eq(false)))
				.thenReturn(Map.of("orderInfo", orderInfo));
		when(adminOrderDeliveryService.delivery(anyLong(), anyString(), anyLong(), any()))
				.thenReturn(new LinkedHashMap<>());
		service.scheduleSyncLogistics();
		verify(wdtErpLogisticsPort, times(2)).getWaitSyncPage(eq(1L), eq("SN"), anyInt(), any(), any(), any());
	}

	@Test
	void distributorAddShopNo_plus_settingShopNos() {
		baseSetting();
		Distributor d = new Distributor();
		d.setWdtShopNo("W1");
		d.setWdtShopId(1L);
		when(distributorMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(d));
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), anyString(), anyInt(), any(), any(), any()))
				.thenReturn(List.of());
		service.scheduleSyncLogistics();
		verify(wdtErpLogisticsPort, times(1)).getWaitSyncPage(eq(1L), eq("W1"), anyInt(), any(), any(), any());
		verify(wdtErpLogisticsPort, times(1)).getWaitSyncPage(eq(1L), eq("SN"), anyInt(), any(), any(), any());
	}

	@Test
	@DisplayName("分支7：全企业+全店铺拉空页处理完毕，完整跑通不抛异常")
	void branch7_fullEnterpriseAllShops_completesWithoutThrowing() {
		baseSetting();
		Distributor d = new Distributor();
		d.setWdtShopNo("W1");
		d.setWdtShopId(1L);
		when(distributorMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(d));
		when(wdtErpLogisticsPort.getWaitSyncPage(anyLong(), anyString(), anyInt(), any(), any(), any()))
				.thenReturn(List.of());
		assertThatCode(() -> service.scheduleSyncLogistics()).doesNotThrowAnyException();
		verify(wdtErpLogisticsPort, times(1)).getWaitSyncPage(eq(1L), eq("W1"), anyInt(), any(), any(), any());
		verify(wdtErpLogisticsPort, times(1)).getWaitSyncPage(eq(1L), eq("SN"), anyInt(), any(), any(), any());
	}

	private static Map<String, Object> buildOrderInfoWithItem(long orderId, long itemId, String lineDelivery) {
		Map<String, Object> oi = new LinkedHashMap<>();
		oi.put("company_id", 1L);
		oi.put("order_id", orderId);
		List<Map<String, Object>> items = new ArrayList<>();
		Map<String, Object> it = new LinkedHashMap<>();
		it.put("id", itemId);
		it.put("delivery_status", lineDelivery);
		it.put("num", 1);
		items.add(it);
		oi.put("items", items);
		return oi;
	}
}
