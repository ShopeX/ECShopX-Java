package cn.shopex.ecshopx.distribution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.distribution.repository.ItemsAuditByDistributorJdbcRepository;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributorUpdateServiceApplyEntryPublishProbeTest {

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
	void performUpdateAndEvents_returnsApiRowAndAppliesMultiLang() {
		ObjectMapper objectMapper = new ObjectMapper();
		DistributorUpdateService service = new DistributorUpdateService(
				distributorMapper,
				distributorWriteRepository,
				distributorMultiLangWriteService,
				objectMapper,
				itemsAuditByDistributorJdbcRepository);

		Distributor info = new Distributor();
		info.setCompanyId(7L);
		info.setDistributorId(99L);
		info.setName("N");
		info.setIsAuditGoods(false);

		when(distributorWriteRepository.existsDefaultDistributor(7L)).thenReturn(true);
		when(distributorWriteRepository.selectSimpleByCompanyAndId(7L, 99L)).thenReturn(Optional.of(info));

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", 7L);
		merged.put("distributor_id", 99L);

		Map<String, Object> row = service.performUpdateAndEvents(merged, 99L, null);

		assertEquals(7L, ((Number) row.get("company_id")).longValue());
		assertEquals(99L, ((Number) row.get("distributor_id")).longValue());

		verify(distributorMultiLangWriteService)
				.applyAfterUpdate(eq(99L), eq(7L), eq(merged), eq((String) null));
	}

	@Test
	@DisplayName("performUpdateAndEvents: shop-batch merged alias (entry-02-api-distributorshop-updateshops)")
	void performUpdateAndEvents_whenMergedMatchesShopBatchUpdateAlias_thenReturnsRowAndMultiLang() {
		ObjectMapper objectMapper = new ObjectMapper();
		DistributorUpdateService service = new DistributorUpdateService(
				distributorMapper,
				distributorWriteRepository,
				distributorMultiLangWriteService,
				objectMapper,
				itemsAuditByDistributorJdbcRepository);

		long companyId = 601L;
		long distributorId = 5005L;

		Distributor before = new Distributor();
		before.setCompanyId(companyId);
		before.setDistributorId(distributorId);
		before.setShopId(0L);
		before.setIsDistributor(true);
		before.setName("旧店名");
		before.setMobile("13912345678");
		before.setIsAuditGoods(false);

		Distributor after = new Distributor();
		after.setCompanyId(companyId);
		after.setDistributorId(distributorId);
		after.setShopId(77007L);
		after.setIsDistributor(false);
		after.setName("新门店名");
		after.setAddress("中关村大街1号");
		after.setMobile("13912345678");
		after.setIsValid("true");
		after.setProvince("北京市");
		after.setCity("北京市");
		after.setArea("海淀区");
		after.setHour("08:30 - 21:30");
		after.setLogo("/x/logo.jpg");
		after.setBanner("/x/bn.jpg");
		after.setAutoSyncGoods(true);
		after.setIsAuditGoods(true);
		after.setContact("王五");
		after.setIsZiti(true);
		after.setLng("116.310003");
		after.setLat("39.992806");
		after.setFirstLetter("X");
		after.setCreated(1_600_000_000L);
		after.setUpdated(1_700_000_001L);

		when(distributorWriteRepository.existsDefaultDistributor(companyId)).thenReturn(true);
		when(distributorWriteRepository.selectSimpleByCompanyAndId(companyId, distributorId))
				.thenReturn(Optional.of(before))
				.thenReturn(Optional.of(after));
		when(distributorMapper.update(isNull(), any())).thenReturn(1);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("distributor_id", distributorId);
		merged.put("name", "新门店名");
		merged.put("address", "中关村大街1号");
		merged.put("mobile", "13912345678");
		merged.put("is_valid", "true");
		merged.put("province", "北京市");
		merged.put("city", "北京市");
		merged.put("area", "海淀区");
		merged.put("hour", "08:30 - 21:30");
		merged.put("logo", "/x/logo.jpg");
		merged.put("banner", "/x/bn.jpg");
		merged.put("auto_sync_goods", true);
		merged.put("is_audit_goods", true);
		merged.put("contact", "王五");
		merged.put("is_ziti", true);
		merged.put("lng", "116.310003");
		merged.put("lat", "39.992806");

		Map<String, Object> row = service.performUpdateAndEvents(merged, distributorId, null);

		assertEquals(companyId, ((Number) row.get("company_id")).longValue());
		assertEquals(distributorId, ((Number) row.get("distributor_id")).longValue());
		assertEquals("新门店名", row.get("name"));
		assertEquals("中关村大街1号", row.get("address"));
		assertEquals("13912345678", row.get("mobile"));
		assertEquals("08:30 - 21:30", row.get("hour"));
		assertEquals(false, row.get("is_distributor"));

		verify(distributorMultiLangWriteService)
				.applyAfterUpdate(eq(distributorId), eq(companyId), eq(merged), nullable(String.class));
	}

	@Test
	@DisplayName("entry-03-api-shops-setwxshopssetting")
	void performUpdateAndEvents_whenMergedMatchesWxShopsDisplayColumns_thenReturnsRowAndMultiLang() {
		ObjectMapper objectMapper = new ObjectMapper();
		DistributorUpdateService service = new DistributorUpdateService(
				distributorMapper,
				distributorWriteRepository,
				distributorMultiLangWriteService,
				objectMapper,
				itemsAuditByDistributorJdbcRepository);

		long companyId = 702L;
		long distributorId = 9002L;

		Distributor before = new Distributor();
		before.setCompanyId(companyId);
		before.setDistributorId(distributorId);
		before.setName("OldWx");
		before.setLogo("/old.svg");
		before.setIsAuditGoods(false);

		Distributor after = new Distributor();
		after.setCompanyId(companyId);
		after.setDistributorId(distributorId);
		after.setName("WxDisplayName");
		after.setLogo("/wx-logo.svg");
		after.setIsAuditGoods(false);

		when(distributorWriteRepository.existsDefaultDistributor(companyId)).thenReturn(true);
		when(distributorWriteRepository.selectSimpleByCompanyAndId(companyId, distributorId))
				.thenReturn(Optional.of(before))
				.thenReturn(Optional.of(after));
		when(distributorMapper.update(isNull(), any())).thenReturn(1);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("distributor_id", distributorId);
		merged.put("name", "WxDisplayName");
		merged.put("logo", "/wx-logo.svg");

		Map<String, Object> row = service.performUpdateAndEvents(merged, distributorId, null);

		assertEquals(companyId, ((Number) row.get("company_id")).longValue());
		assertEquals(distributorId, ((Number) row.get("distributor_id")).longValue());
		assertEquals("WxDisplayName", row.get("name"));
		assertEquals("/wx-logo.svg", row.get("logo"));

		verify(distributorMultiLangWriteService)
				.applyAfterUpdate(eq(distributorId), eq(companyId), eq(merged), nullable(String.class));
	}
}
