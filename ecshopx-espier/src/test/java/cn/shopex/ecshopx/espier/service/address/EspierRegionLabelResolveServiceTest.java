package cn.shopex.ecshopx.espier.service.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.espier.domain.Address;
import cn.shopex.ecshopx.espier.mapper.AddressMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EspierRegionLabelResolveServiceTest {

	private static final long BEIJING_PROVINCE_ID = 110_000L;
	private static final long BEIJING_CITY_ID = 110_100L;
	private static final long DONGCHENG_DISTRICT_ID = 110_101L;
	private static final long HAINAN_PROVINCE_ID = 460_000L;
	private static final long DIRECT_COUNTY_ID = 469_000L;
	private static final long WUZHISHAN_DISTRICT_ID = 469_001L;

	@Mock
	private AddressMapper addressMapper;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, Address.class);
	}

	@Test
	@DisplayName("TC-R1: 北京/东城区等精确名解析为正确三级 id")
	void tcR1_beijingDongchengResolvesExactLabelsToIds() {
		// #given DB 存在北京市三级精确 label
		when(addressMapper.selectOne(any()))
				.thenReturn(address(BEIJING_PROVINCE_ID, "北京市", 0L))
				.thenReturn(address(BEIJING_CITY_ID, "北京市", BEIJING_PROVINCE_ID))
				.thenReturn(address(DONGCHENG_DISTRICT_ID, "东城区", BEIJING_CITY_ID));

		EspierRegionLabelResolveService service = new EspierRegionLabelResolveService(addressMapper);

		// #when
		int[] ids = service.resolveIds("北京市", "北京市", "东城区");

		// #then
		assertArrayEquals(
				new int[] {
					(int) BEIJING_PROVINCE_ID, (int) BEIJING_CITY_ID, (int) DONGCHENG_DISTRICT_ID
				},
				ids);
	}

	@Test
	@DisplayName("TC-R2: city==region 时走省直辖县级行政区划回退")
	void tcR2_cityEqualsRegionUsesDirectCountyFallback() {
		// #given 海南省 / 五指山市 / 五指山市（city 与 region 同名）
		when(addressMapper.selectOne(any()))
				.thenReturn(address(HAINAN_PROVINCE_ID, "海南省", 0L))
				.thenReturn(null)
				.thenReturn(null)
				.thenReturn(address(DIRECT_COUNTY_ID, "省直辖县级行政区划", HAINAN_PROVINCE_ID))
				.thenReturn(address(WUZHISHAN_DISTRICT_ID, "五指山市", DIRECT_COUNTY_ID));

		EspierRegionLabelResolveService service = new EspierRegionLabelResolveService(addressMapper);

		// #when
		int[] ids = service.resolveIds("海南省", "五指山市", "五指山市");

		// #then 市 id 为虚拟节点 469000，区 id 为五指山县级 469001
		assertArrayEquals(
				new int[] {
					(int) HAINAN_PROVINCE_ID, (int) DIRECT_COUNTY_ID, (int) WUZHISHAN_DISTRICT_ID
				},
				ids);
	}

	@Test
	@DisplayName("TC-R3: 区名去「区/市/县」后缀后命中")
	void tcR3_districtSuffixStrippedBeforeLookup() {
		// #given 区 DB 存「新乐」，输入「新乐市」需去「市」后缀（PHP 规则含 区/市/县）
		long hebeiId = 130_000L;
		long shijiazhuangId = 130_100L;
		long xinleId = 130_184L;
		when(addressMapper.selectOne(any()))
				.thenReturn(
						address(hebeiId, "河北省", 0L),
						address(shijiazhuangId, "石家庄市", hebeiId),
						null)
				.thenAnswer(
						invocation -> {
							@SuppressWarnings("unchecked")
							LambdaQueryWrapper<Address> wrapper = invocation.getArgument(0);
							if (matches(wrapper, shijiazhuangId, "新乐")) {
								return address(xinleId, "新乐", shijiazhuangId);
							}
							return null;
						});

		EspierRegionLabelResolveService service = new EspierRegionLabelResolveService(addressMapper);

		// #when
		int[] ids = service.resolveIds("河北省", "石家庄市", "新乐市");

		// #then 去「市」后缀后命中新乐 id；未实现时 district 仍为默认 1
		assertArrayEquals(
				new int[] {(int) hebeiId, (int) shijiazhuangId, (int) xinleId}, ids);
	}

	@Test
	@DisplayName("TC-R4: 不存在地名抛异常且消息含「地区不存在」")
	void tcR4_unknownRegionThrowsWithMessage() {
		// #given 省级 label 无法命中
		when(addressMapper.selectOne(any())).thenReturn(null);

		EspierRegionLabelResolveService service = new EspierRegionLabelResolveService(addressMapper);

		// #when / #then 禁止默认返回 1
		Exception ex =
				assertThrows(
						Exception.class,
						() -> service.resolveIds("不存在省", "不存在市", "不存在区"));
		assertTrue(
				ex.getMessage() != null && ex.getMessage().contains("地区不存在"),
				"异常消息应含「地区不存在」");
	}

	@Test
	@DisplayName("TC-R5: province/city/district 空或 null 按失败语义处理")
	void tcR5_blankOrNullInputsThrowWithMessage() {
		// #given 空输入不应静默返回默认 id
		EspierRegionLabelResolveService service = new EspierRegionLabelResolveService(addressMapper);

		// #when / #then null 输入
		Exception nullEx =
				assertThrows(Exception.class, () -> service.resolveIds(null, null, null));
		assertTrue(
				nullEx.getMessage() != null && nullEx.getMessage().contains("地区不存在"),
				"null 输入应抛错含「地区不存在」");

		// #when / #then 空字符串输入
		Exception blankEx =
				assertThrows(Exception.class, () -> service.resolveIds("", "", ""));
		assertTrue(
				blankEx.getMessage() != null && blankEx.getMessage().contains("地区不存在"),
				"空字符串输入应抛错含「地区不存在」");
	}

	private static boolean matches(LambdaQueryWrapper<Address> wrapper, long parentId, String label) {
		Map<String, Object> params = wrapper.getParamNameValuePairs();
		boolean parentMatched =
				params.values().stream()
						.anyMatch(v -> v instanceof Number n && n.longValue() == parentId);
		return parentMatched && params.values().contains(label);
	}

	private static Address address(long id, String label, long parentId) {
		Address row = new Address();
		row.setId(id);
		row.setLabel(label);
		row.setParentId(parentId);
		return row;
	}
}
