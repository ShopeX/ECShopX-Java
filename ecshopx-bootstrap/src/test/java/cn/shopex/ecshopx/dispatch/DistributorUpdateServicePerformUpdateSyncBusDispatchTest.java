package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import cn.shopex.ecshopx.config.DistributorUpdateEventDispatchPublisherImpl;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.distribution.repository.ItemsAuditByDistributorJdbcRepository;
import cn.shopex.ecshopx.distribution.service.DistributorMultiLangWriteService;
import cn.shopex.ecshopx.distribution.service.DistributorUpdateService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Bus gate: distributor-update row snapshot is published through {@link DispatchFacade} to a registered listener.
 * {@link DistributorUpdateService} returns the row; the publisher call mirrors {@code DistributorUpdateOrchestrator}
 * after-commit wiring.
 */
@ExtendWith(MockitoExtension.class)
class DistributorUpdateServicePerformUpdateSyncBusDispatchTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Distributor.class);
	}

	@Mock
	private DistributorMapper distributorMapper;

	@Mock
	private DistributorWriteRepository distributorWriteRepository;

	@Mock
	private DistributorMultiLangWriteService distributorMultiLangWriteService;

	@Mock
	private ItemsAuditByDistributorJdbcRepository itemsAuditByDistributorJdbcRepository;

	@Test
	@DisplayName("performUpdateAndEvents: shop-batch merged — sync bus delivers entities to listener (entry-02-api-distributorshop-updateshops)")
	void performUpdateAndEvents_shopBatchMerged_publishEventSyncDeliversEntitiesToListener() {
		DispatchListener probeListener = mock(DispatchListener.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> asyncCaptured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, asyncCaptured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTOR_UPDATE,
				DistributionDispatchEventNames.LISTENER_SHOP_UPDATE_SEND_OME,
				ListenerDispatchOptions.syncDefaults(),
				probeListener);

		DistributorUpdateEventDispatchPublisherImpl publisher = new DistributorUpdateEventDispatchPublisherImpl(facade);
		ObjectMapper objectMapper = new ObjectMapper();
		DistributorUpdateService service = new DistributorUpdateService(
				distributorMapper,
				distributorWriteRepository,
				distributorMultiLangWriteService,
				objectMapper,
				itemsAuditByDistributorJdbcRepository);

		long companyId = 501L;
		long distributorId = 3003L;

		when(distributorWriteRepository.existsDefaultDistributor(companyId)).thenReturn(true);

		Distributor before = baseDistributor(companyId, distributorId);
		Distributor after = refreshedAfterShopBatchUpdate(companyId, distributorId);

		when(distributorWriteRepository.selectSimpleByCompanyAndId(companyId, distributorId))
				.thenReturn(Optional.of(before))
				.thenReturn(Optional.of(after));

		when(distributorMapper.update(isNull(), any())).thenReturn(1);

		Map<String, Object> merged = shopBatchMergedShape(companyId, distributorId);

		Map<String, Object> row = service.performUpdateAndEvents(merged, distributorId, null);
		publisher.publish(row);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> eventPayloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(probeListener).onEvent(eventPayloadCaptor.capture());

		Map<String, Object> listenerArg = eventPayloadCaptor.getValue();
		assertTrue(listenerArg.containsKey("entities"));
		@SuppressWarnings("unchecked")
		Map<String, Object> entities = (Map<String, Object>) listenerArg.get("entities");
		assertEquals(companyId, ((Number) entities.get("company_id")).longValue());
		assertEquals(distributorId, ((Number) entities.get("distributor_id")).longValue());
		assertEquals("门店展示名", entities.get("name"));
		assertEquals("文一西路998号", entities.get("address"));
		assertEquals("13800138000", entities.get("mobile"));
		assertEquals("true", entities.get("is_valid"));
		assertEquals("浙江省", entities.get("province"));
		assertEquals("杭州市", entities.get("city"));
		assertEquals("西湖区", entities.get("area"));
		assertEquals("09:00 - 18:00", entities.get("hour"));
		assertEquals("/logo.png", entities.get("logo"));
		assertEquals("/banner.png", entities.get("banner"));
		assertEquals(true, entities.get("auto_sync_goods"));
		assertEquals(true, entities.get("is_audit_goods"));
		assertEquals("张三", entities.get("contact"));
		assertEquals(true, entities.get("is_ziti"));
		assertEquals("120.153576", entities.get("lng"));
		assertEquals("30.287459", entities.get("lat"));
		assertEquals(false, entities.get("is_distributor"));

		verify(distributorMultiLangWriteService)
				.applyAfterUpdate(eq(distributorId), eq(companyId), eq(merged), nullable(String.class));

		List<DispatchMessage> published = facade.publishedMessages();
		assertEquals(2, published.size());
		assertTrue(asyncCaptured.isEmpty());
	}

	@Test
	@DisplayName("entry-03-api-shops-setwxshopssetting")
	void performUpdateAndEvents_wxShopsDisplayMerged_publishEventSyncDeliversEntitiesToListener() {
		DispatchListener probeListener = mock(DispatchListener.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> asyncCaptured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, asyncCaptured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTOR_UPDATE,
				DistributionDispatchEventNames.LISTENER_SHOP_UPDATE_SEND_OME,
				ListenerDispatchOptions.syncDefaults(),
				probeListener);

		DistributorUpdateEventDispatchPublisherImpl publisher = new DistributorUpdateEventDispatchPublisherImpl(facade);
		ObjectMapper objectMapper = new ObjectMapper();
		DistributorUpdateService service = new DistributorUpdateService(
				distributorMapper,
				distributorWriteRepository,
				distributorMultiLangWriteService,
				objectMapper,
				itemsAuditByDistributorJdbcRepository);

		long companyId = 504L;
		long distributorId = 3006L;

		when(distributorWriteRepository.existsDefaultDistributor(companyId)).thenReturn(true);

		Distributor before = baseDistributorWx(companyId, distributorId);
		Distributor after = refreshedAfterWxDisplayOnly(companyId, distributorId);

		when(distributorWriteRepository.selectSimpleByCompanyAndId(companyId, distributorId))
				.thenReturn(Optional.of(before))
				.thenReturn(Optional.of(after));

		when(distributorMapper.update(isNull(), any())).thenReturn(1);

		Map<String, Object> merged = wxShopsDisplayMergedShape(companyId, distributorId);

		Map<String, Object> row = service.performUpdateAndEvents(merged, distributorId, null);
		publisher.publish(row);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> eventPayloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(probeListener).onEvent(eventPayloadCaptor.capture());

		Map<String, Object> listenerArg = eventPayloadCaptor.getValue();
		assertTrue(listenerArg.containsKey("entities"));
		@SuppressWarnings("unchecked")
		Map<String, Object> entities = (Map<String, Object>) listenerArg.get("entities");
		assertEquals(companyId, ((Number) entities.get("company_id")).longValue());
		assertEquals(distributorId, ((Number) entities.get("distributor_id")).longValue());
		assertEquals("WxDisplayName", entities.get("name"));
		assertEquals("/wx-logo.svg", entities.get("logo"));

		verify(distributorMultiLangWriteService)
				.applyAfterUpdate(eq(distributorId), eq(companyId), eq(merged), nullable(String.class));

		List<DispatchMessage> published = facade.publishedMessages();
		assertEquals(2, published.size());
		assertTrue(asyncCaptured.isEmpty());
	}

	private static Distributor baseDistributor(long companyId, long distributorId) {
		Distributor d = new Distributor();
		d.setCompanyId(companyId);
		d.setDistributorId(distributorId);
		d.setShopId(0L);
		d.setIsDistributor(true);
		d.setName("旧名称");
		d.setMobile("13800138000");
		d.setIsAuditGoods(false);
		d.setIsValid("true");
		return d;
	}

	private static Distributor refreshedAfterShopBatchUpdate(long companyId, long distributorId) {
		Distributor d = new Distributor();
		d.setCompanyId(companyId);
		d.setDistributorId(distributorId);
		d.setShopId(88001L);
		d.setIsDistributor(false);
		d.setName("门店展示名");
		d.setAddress("文一西路998号");
		d.setMobile("13800138000");
		d.setIsValid("true");
		d.setProvince("浙江省");
		d.setCity("杭州市");
		d.setArea("西湖区");
		d.setHour("09:00 - 18:00");
		d.setLogo("/logo.png");
		d.setBanner("/banner.png");
		d.setAutoSyncGoods(true);
		d.setIsAuditGoods(true);
		d.setContact("张三");
		d.setIsZiti(true);
		d.setLng("120.153576");
		d.setLat("30.287459");
		d.setFirstLetter("M");
		d.setUpdated(1_700_000_000L);
		d.setCreated(1_600_000_000L);
		return d;
	}

	/**
	 * Field subset aligned with admin shop batch update input after region expansion into
	 * province/city/area and hour string normalization.
	 */
	private static Map<String, Object> shopBatchMergedShape(long companyId, long distributorId) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("distributor_id", distributorId);
		merged.put("name", "门店展示名");
		merged.put("address", "文一西路998号");
		merged.put("mobile", "13800138000");
		merged.put("is_valid", "true");
		merged.put("province", "浙江省");
		merged.put("city", "杭州市");
		merged.put("area", "西湖区");
		merged.put("hour", "09:00 - 18:00");
		merged.put("logo", "/logo.png");
		merged.put("banner", "/banner.png");
		merged.put("auto_sync_goods", true);
		merged.put("is_audit_goods", true);
		merged.put("contact", "张三");
		merged.put("is_ziti", true);
		merged.put("lng", "120.153576");
		merged.put("lat", "30.287459");
		return merged;
	}

	private static Distributor baseDistributorWx(long companyId, long distributorId) {
		Distributor d = new Distributor();
		d.setCompanyId(companyId);
		d.setDistributorId(distributorId);
		d.setShopId(0L);
		d.setIsDistributor(true);
		d.setName("OldWx");
		d.setLogo("/old.svg");
		d.setMobile("13800138000");
		d.setIsAuditGoods(false);
		d.setIsValid("true");
		return d;
	}

	private static Distributor refreshedAfterWxDisplayOnly(long companyId, long distributorId) {
		Distributor d = new Distributor();
		d.setCompanyId(companyId);
		d.setDistributorId(distributorId);
		d.setShopId(0L);
		d.setIsDistributor(true);
		d.setName("WxDisplayName");
		d.setLogo("/wx-logo.svg");
		d.setMobile("13800138000");
		d.setIsAuditGoods(false);
		d.setIsValid("true");
		d.setFirstLetter("W");
		return d;
	}

	private static Map<String, Object> wxShopsDisplayMergedShape(long companyId, long distributorId) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("distributor_id", distributorId);
		merged.put("name", "WxDisplayName");
		merged.put("logo", "/wx-logo.svg");
		return merged;
	}
}
