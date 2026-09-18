package cn.shopex.ecshopx.systemlink.service.wdterp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.wdterp.WdtErpInventoryPort;
import cn.shopex.ecshopx.common.port.wdterp.WdtErpLogisticsPort;
import cn.shopex.ecshopx.common.port.wdterp.dto.WdtInventoryWaitSyncPage;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDeliveryService;
import cn.shopex.ecshopx.promotions.service.WdtErpSyncInventoryPromotionSupport;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WdtErpSyncServiceScheduleSyncInventoryTest {

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

	private void openSetting() {
		when(companysRedisTemplate.keys(WdtErpSyncService.WDT_REDIS_PREFIX + "*"))
				.thenReturn(Set.of("WdtErpSetting:ab"));
		when(valueOps.get("WdtErpSetting:ab"))
				.thenReturn("{\"is_open\":true,\"company_id\":1,\"sid\":\"s\",\"app_key\":\"k\",\"app_secret\":\"a:b\",\"shop_id\":10}");
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("standard");
	}

	/** §5 1：Redis 早退，不调 WDT 库存。 */
	@Test
	void inv_branch1_keysEmpty() {
		when(companysRedisTemplate.keys(WdtErpSyncService.WDT_REDIS_PREFIX + "*")).thenReturn(Set.of());
		service.scheduleSyncInventory();
		verify(wdtErpInventoryPort, never()).fetchWaitSyncPage(anyLong(), anyInt(), any(), any(), any());
	}

	/** §5 2：未开启。 */
	@Test
	void inv_branch2_isOpenFalse() {
		when(companysRedisTemplate.keys(WdtErpSyncService.WDT_REDIS_PREFIX + "*"))
				.thenReturn(Set.of("WdtErpSetting:ab"));
		when(valueOps.get("WdtErpSetting:ab")).thenReturn("{\"is_open\":false,\"company_id\":1}");
		service.scheduleSyncInventory();
		verify(wdtErpInventoryPort, never()).fetchWaitSyncPage(anyLong(), anyInt(), any(), any(), any());
	}

	/** §5 3-F-2-c：活动内 BN 跳过。 */
	@Test
	void inv_branch_activityBn_skips() {
		openSetting();
		when(wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(1L)).thenReturn(Set.of("ACT_BN"));
		when(wdtErpInventoryPort.fetchWaitSyncPage(eq(1L), anyInt(), any(), any(), any()))
				.thenReturn(new WdtInventoryWaitSyncPage(List.of("R1"), 1), new WdtInventoryWaitSyncPage(List.of(), 1));
		Map<String, Object> st = new LinkedHashMap<>();
		st.put("match_code", "ACT_BN");
		st.put("shop_id", 10L);
		st.put("syn_stock", 5);
		when(wdtErpInventoryPort.queryStore(eq(1L), eq("R1"), any(), any(), any())).thenReturn(st);
		service.scheduleSyncInventory();
		verify(wdtErpInventoryPort, never()).acknowledgeSuccess(anyLong(), any(), any(), any(), any(), any());
		verify(wdtErpInventoryPort, never()).acknowledgeFail(anyLong(), any(), any(), any(), any(), any());
	}

	/** §5 3-F-2-d：无本地货品，ack 成功消队。 */
	@Test
	void inv_branch_itemMissing_ackSuccess() {
		openSetting();
		when(wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(1L)).thenReturn(Set.of());
		when(wdtErpInventoryPort.fetchWaitSyncPage(eq(1L), anyInt(), any(), any(), any()))
				.thenReturn(new WdtInventoryWaitSyncPage(List.of("R1"), 1), new WdtInventoryWaitSyncPage(List.of(), 1));
		Map<String, Object> st = new LinkedHashMap<>();
		st.put("match_code", "BN1");
		st.put("shop_id", 10L);
		st.put("syn_stock", 3);
		when(wdtErpInventoryPort.queryStore(eq(1L), eq("R1"), any(), any(), any())).thenReturn(st);
		when(itemsRepository.findByItemBnAndCompany("BN1", 1L)).thenReturn(null);
		service.scheduleSyncInventory();
		verify(wdtErpInventoryPort).acknowledgeSuccess(eq(1L), eq("R1"), eq(st), any(), any(), any());
	}

	/** §5 3-F-2-f 第一支：主仓更新。 */
	@Test
	void inv_branch_mainStore_updatesItems() {
		openSetting();
		when(wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(1L)).thenReturn(Set.of());
		when(wdtErpInventoryPort.fetchWaitSyncPage(eq(1L), anyInt(), any(), any(), any()))
				.thenReturn(new WdtInventoryWaitSyncPage(List.of("R1"), 1), new WdtInventoryWaitSyncPage(List.of(), 1));
		Map<String, Object> st = new LinkedHashMap<>();
		st.put("match_code", "BN1");
		st.put("shop_id", 10L);
		st.put("syn_stock", 8);
		when(wdtErpInventoryPort.queryStore(eq(1L), eq("R1"), any(), any(), any())).thenReturn(st);
		Items it = new Items();
		it.setItemId(100L);
		it.setItemBn("BN1");
		when(itemsRepository.findByItemBnAndCompany("BN1", 1L)).thenReturn(it);
		when(normalOrdersItemsMapper.sumUnpaidHoldingNumForItem(eq(1L), eq(100L), anyLong())).thenReturn(1L);
		when(itemStoreService.saveItemStoreForWdtSync(100L, 7, 0L)).thenReturn(true);
		service.scheduleSyncInventory();
		verify(itemsRepository).updateSingleItemStoreIfExists(100L, 7);
		verify(wdtErpInventoryPort).acknowledgeSuccess(eq(1L), eq("R1"), eq(st), any(), any(), any());
	}

	/** §5 3-F-2-f 第二支：分销库存。 */
	@Test
	void inv_branch_distributor_updatesDistItems() {
		openSetting();
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("standard");
		when(wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(1L)).thenReturn(Set.of());
		when(wdtErpInventoryPort.fetchWaitSyncPage(eq(1L), anyInt(), any(), any(), any()))
				.thenReturn(new WdtInventoryWaitSyncPage(List.of("R1"), 1), new WdtInventoryWaitSyncPage(List.of(), 1));
		Map<String, Object> st = new LinkedHashMap<>();
		st.put("match_code", "BN1");
		st.put("shop_id", 99L);
		st.put("syn_stock", 4);
		when(wdtErpInventoryPort.queryStore(eq(1L), eq("R1"), any(), any(), any())).thenReturn(st);
		Items it = new Items();
		it.setItemId(200L);
		when(itemsRepository.findByItemBnAndCompany("BN1", 1L)).thenReturn(it);
		Distributor d = new Distributor();
		d.setDistributorId(7L);
		d.setWdtShopId(99L);
		when(distributorMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(d);
		when(itemStoreService.saveItemStoreForWdtSync(200L, 4, 7L)).thenReturn(true);
		when(distributorItemsMapper.update(any(), any())).thenReturn(1);
		service.scheduleSyncInventory();
		verify(wdtErpInventoryPort).acknowledgeSuccess(eq(1L), eq("R1"), eq(st), any(), any(), any());
	}

	/** §5 3-F-2-f：分销商无匹配，ack 成功。 */
	@Test
	void inv_branch_distributorEmpty_ackSuccess() {
		openSetting();
		when(shopMenuService.resolveProductModelKeyForCompany(1L)).thenReturn("standard");
		when(wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(1L)).thenReturn(Set.of());
		when(wdtErpInventoryPort.fetchWaitSyncPage(eq(1L), anyInt(), any(), any(), any()))
				.thenReturn(new WdtInventoryWaitSyncPage(List.of("R1"), 1), new WdtInventoryWaitSyncPage(List.of(), 1));
		Map<String, Object> st = new LinkedHashMap<>();
		st.put("match_code", "BN1");
		st.put("shop_id", 99L);
		st.put("syn_stock", 1);
		when(wdtErpInventoryPort.queryStore(eq(1L), eq("R1"), any(), any(), any())).thenReturn(st);
		Items it = new Items();
		it.setItemId(1L);
		when(itemsRepository.findByItemBnAndCompany("BN1", 1L)).thenReturn(it);
		when(distributorMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
		service.scheduleSyncInventory();
		verify(wdtErpInventoryPort).acknowledgeSuccess(eq(1L), eq("R1"), eq(st), any(), any(), any());
	}

	/** §5 3-F-2-f：save 假不更新 DB。 */
	@Test
	void inv_saveFalse_failsAck() {
		openSetting();
		when(wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(1L)).thenReturn(Set.of());
		when(wdtErpInventoryPort.fetchWaitSyncPage(eq(1L), anyInt(), any(), any(), any()))
				.thenReturn(new WdtInventoryWaitSyncPage(List.of("R1"), 1), new WdtInventoryWaitSyncPage(List.of(), 1));
		Map<String, Object> st = new LinkedHashMap<>();
		st.put("match_code", "BN1");
		st.put("shop_id", 10L);
		st.put("syn_stock", 2);
		when(wdtErpInventoryPort.queryStore(eq(1L), eq("R1"), any(), any(), any())).thenReturn(st);
		Items it = new Items();
		it.setItemId(50L);
		when(itemsRepository.findByItemBnAndCompany("BN1", 1L)).thenReturn(it);
		when(normalOrdersItemsMapper.sumUnpaidHoldingNumForItem(eq(1L), eq(50L), anyLong())).thenReturn(0L);
		when(itemStoreService.saveItemStoreForWdtSync(50L, 2, 0L)).thenReturn(false);
		service.scheduleSyncInventory();
		verify(itemsRepository, never()).updateSingleItemStoreIfExists(anyLong(), anyInt());
		verify(wdtErpInventoryPort).acknowledgeFail(eq(1L), eq("R1"), eq(st), any(), any(), any());
	}

	/** §5 3-F-2-a：store_query 抛错。 */
	@Test
	void inv_queryStoreException_continue() {
		openSetting();
		when(wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(1L)).thenReturn(Set.of());
		when(wdtErpInventoryPort.fetchWaitSyncPage(eq(1L), anyInt(), any(), any(), any()))
				.thenReturn(new WdtInventoryWaitSyncPage(List.of("R1"), 1), new WdtInventoryWaitSyncPage(List.of(), 1));
		doThrow(new RuntimeException("wdt")).when(wdtErpInventoryPort).queryStore(eq(1L), eq("R1"), any(), any(), any());
		service.scheduleSyncInventory();
		verify(wdtErpInventoryPort, never()).acknowledgeSuccess(anyLong(), any(), any(), any(), any(), any());
	}

	/** §5 3-F-2-b：非异常但无有效 stock 行。 */
	@Test
	void inv_incompleteStockInfo_noAck() {
		openSetting();
		when(wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(1L)).thenReturn(Set.of());
		when(wdtErpInventoryPort.fetchWaitSyncPage(eq(1L), anyInt(), any(), any(), any()))
				.thenReturn(new WdtInventoryWaitSyncPage(List.of("R1"), 1), new WdtInventoryWaitSyncPage(List.of(), 1));
		Map<String, Object> st = new LinkedHashMap<>();
		st.put("match_code", "x");
		st.put("shop_id", 9L);
		when(wdtErpInventoryPort.queryStore(eq(1L), eq("R1"), any(), any(), any())).thenReturn(st);
		service.scheduleSyncInventory();
		verify(wdtErpInventoryPort, never()).acknowledgeSuccess(anyLong(), any(), any(), any(), any(), any());
		verify(wdtErpInventoryPort, never()).acknowledgeFail(anyLong(), any(), any(), any(), any(), any());
	}

	/** §5 3-F-2-e：未支付占量扣减。 */
	@Test
	void inv_unpaidHolding_reducesQuantity() {
		openSetting();
		when(wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(1L)).thenReturn(Set.of());
		when(wdtErpInventoryPort.fetchWaitSyncPage(eq(1L), anyInt(), any(), any(), any()))
				.thenReturn(new WdtInventoryWaitSyncPage(List.of("R1"), 1), new WdtInventoryWaitSyncPage(List.of(), 1));
		Map<String, Object> st = new LinkedHashMap<>();
		st.put("match_code", "BN1");
		st.put("shop_id", 10L);
		st.put("syn_stock", 20);
		when(wdtErpInventoryPort.queryStore(eq(1L), eq("R1"), any(), any(), any())).thenReturn(st);
		Items it = new Items();
		it.setItemId(11L);
		when(itemsRepository.findByItemBnAndCompany("BN1", 1L)).thenReturn(it);
		when(normalOrdersItemsMapper.sumUnpaidHoldingNumForItem(eq(1L), eq(11L), anyLong())).thenReturn(3L);
		when(itemStoreService.saveItemStoreForWdtSync(11L, 17, 0L)).thenReturn(true);
		service.scheduleSyncInventory();
		verify(itemStoreService).saveItemStoreForWdtSync(11L, 17, 0L);
	}
}
