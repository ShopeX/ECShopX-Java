package cn.shopex.ecshopx.espier.service.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.espier.domain.Address;
import cn.shopex.ecshopx.espier.mapper.AddressMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class EspierAddressTreeApplicationServiceTest {

	private static final long HAINAN_ID = 460_000L;
	private static final long DIRECT_COUNTY_ID = 469_000L;
	private static final long WUZHISHAN_DISTRICT_ID = 469_001L;

	@Mock
	private AddressMapper addressMapper;

	@Mock
	private StringRedisTemplate espierStringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOps;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, Address.class);
	}

	@Test
	@DisplayName("TC-A2: 海南省直辖占位展开为真实县级二级节点")
	void tcA2_directCountyPlaceholderExpandedToCountyLabels() {
		// #given Redis miss，DB 含「省直辖县级行政区划」占位节点
		when(espierStringRedisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("address")).thenReturn(null);
		stubHainanDirectCountyDb();

		EspierAddressTreeApplicationService service =
				new EspierAddressTreeApplicationService(addressMapper, espierStringRedisTemplate, objectMapper);

		// #when
		List<Map<String, Object>> tree = service.get();

		// #then 二级不出现占位 label，出现「五指山市」且 value 仍为虚拟市码 469000
		Map<String, Object> hainan = findProvince(tree, "海南省");
		assertNotNull(hainan);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> cities = (List<Map<String, Object>>) hainan.get("children");

		assertFalse(
				cities.stream().anyMatch(c -> "省直辖县级行政区划".equals(c.get("label"))),
				"二级不应保留「省直辖县级行政区划」占位");

		Map<String, Object> wuzhishan =
				cities.stream()
						.filter(c -> "五指山市".equals(c.get("label")))
						.findFirst()
						.orElseThrow(() -> new AssertionError("二级应出现「五指山市」"));
		assertEquals(DIRECT_COUNTY_ID, wuzhishan.get("value"));
		assertEquals(DIRECT_COUNTY_ID, wuzhishan.get("id"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> districtChildren =
				(List<Map<String, Object>>) wuzhishan.get("children");
		assertEquals(1, districtChildren.size());
		assertEquals("五指山市", districtChildren.get(0).get("label"));
		assertEquals(WUZHISHAN_DISTRICT_ID, districtChildren.get(0).get("id"));
	}

	@Test
	@DisplayName("TC-A3: Redis miss 与 hit 返回一致的展开后地址树")
	void tcA3_redisHitAndMissReturnSameExpandedTree() throws Exception {
		// #given 同一海南省直辖 DB 数据，先 miss 再 hit
		when(espierStringRedisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("address")).thenReturn(null).thenAnswer(inv -> cachedJsonHolder[0]);
		stubHainanDirectCountyDb();

		EspierAddressTreeApplicationService service =
				new EspierAddressTreeApplicationService(addressMapper, espierStringRedisTemplate, objectMapper);

		// #when miss 路径
		List<Map<String, Object>> fromMiss = service.get();

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOps).set(eq("address"), jsonCaptor.capture());
		cachedJsonHolder[0] = jsonCaptor.getValue();

		// #when hit 路径
		List<Map<String, Object>> fromHit = service.get();

		// #then 两条路径均无占位且结果一致
		assertExpandedHainanDirectCounty(fromMiss);
		assertExpandedHainanDirectCounty(fromHit);
		assertEquals(fromMiss, fromHit);
	}

	private final String[] cachedJsonHolder = new String[1];

	private static void assertExpandedHainanDirectCounty(List<Map<String, Object>> tree) {
		Map<String, Object> hainan = findProvince(tree, "海南省");
		assertNotNull(hainan);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> cities = (List<Map<String, Object>>) hainan.get("children");
		assertFalse(
				cities.stream().anyMatch(c -> "省直辖县级行政区划".equals(c.get("label"))),
				"展开后二级不应保留「省直辖县级行政区划」占位");
		assertNotNull(
				cities.stream()
						.filter(c -> "五指山市".equals(c.get("label")))
						.findFirst()
						.orElse(null),
				"展开后二级应出现「五指山市」");
	}

	private void stubHainanDirectCountyDb() {
		Address hainan = address(HAINAN_ID, "海南省", 0L, "460000");
		Address directCounty =
				address(DIRECT_COUNTY_ID, "省直辖县级行政区划", HAINAN_ID, "460000,469000");
		Address wuzhishan =
				address(
						WUZHISHAN_DISTRICT_ID,
						"五指山市",
						DIRECT_COUNTY_ID,
						"460000,469000,469001");

		when(addressMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(hainan))
				.thenReturn(List.of(directCounty))
				.thenReturn(List.of(wuzhishan));
	}

	private static Address address(long id, String label, long parentId, String path) {
		Address row = new Address();
		row.setId(id);
		row.setLabel(label);
		row.setParentId(parentId);
		row.setPath(path);
		return row;
	}

	private static Map<String, Object> findProvince(List<Map<String, Object>> tree, String label) {
		return tree.stream()
				.filter(n -> label.equals(n.get("label")))
				.findFirst()
				.orElse(null);
	}
}
