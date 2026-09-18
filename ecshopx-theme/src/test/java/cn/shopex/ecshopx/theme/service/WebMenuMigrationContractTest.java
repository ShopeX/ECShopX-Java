package cn.shopex.ecshopx.theme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.CreateWebMenuRequest;
import cn.shopex.ecshopx.theme.domain.WebMenu;
import cn.shopex.ecshopx.theme.domain.WebMenuItem;
import cn.shopex.ecshopx.theme.mapper.WebMenuItemMapper;
import cn.shopex.ecshopx.theme.mapper.WebMenuMapper;
import cn.shopex.ecshopx.theme.support.WebMenuItemResponseMapper;
import cn.shopex.ecshopx.theme.support.WebMenuResponseMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebMenuMigrationContractTest {

	@Mock
	private WebMenuMapper webMenuMapper;

	@Mock
	private WebMenuItemMapper webMenuItemMapper;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(new MybatisConfiguration(), ""), WebMenu.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(new MybatisConfiguration(), ""), WebMenuItem.class);
	}

	@Test
	@DisplayName("B2：name 或 key 任一为空即报 name 与 key 不能为空")
	void createMenu_blankNameOrKey_throwsResourceException() {
		WebMenuCreateService service =
				new WebMenuCreateService(webMenuMapper, new WebMenuResponseMapper());
		CreateWebMenuRequest request = new CreateWebMenuRequest();
		request.setName(" ");
		request.setKey("home_nav");

		ResourceException ex = assertThrows(ResourceException.class, () -> service.create(1L, request));
		assertEquals("name 与 key 不能为空", ex.getMessage());
	}

	@Test
	@DisplayName("B2：同店铺 key 重复报错")
	void createMenu_duplicateKey_throwsResourceException() {
		WebMenuCreateService service =
				new WebMenuCreateService(webMenuMapper, new WebMenuResponseMapper());
		when(webMenuMapper.selectCount(any())).thenReturn(1L);
		CreateWebMenuRequest request = new CreateWebMenuRequest();
		request.setName("首页导航");
		request.setKey("home_nav");

		ResourceException ex = assertThrows(ResourceException.class, () -> service.create(1L, request));
		assertEquals("同一店铺下 key 已存在", ex.getMessage());
	}

	@Test
	@DisplayName("B1：items_count 和 top_level_item_names 包含禁用项，且不额外按 status 过滤")
	void listMenus_aggregateIncludesDisabledItems() {
		WebMenuListService service = new WebMenuListService(
				webMenuMapper, webMenuItemMapper, new WebMenuResponseMapper());
		when(webMenuMapper.selectCount(any())).thenReturn(1L);

		WebMenu menu = new WebMenu();
		menu.setId(10L);
		menu.setCompanyId(1L);
		menu.setName("顶部导航");
		menu.setKey("top_nav");
		menu.setStatus(1);
		Page<WebMenu> page = new Page<>(1, 20);
		page.setRecords(List.of(menu));
		when(webMenuMapper.selectPage(any(), any())).thenReturn(page);

		when(webMenuItemMapper.selectList(any())).thenReturn(List.of(
				topLevelItem(1L, 10L, 0L, "新品", 2, 0),
				topLevelItem(2L, 10L, 0L, "首页", 1, 1),
				childItem(3L, 10L, 1L, "禁用子项", 0, 0)));

		Map<String, Object> result = service.listMenus(1L, 1, 20, null);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		Map<String, Object> row = list.get(0);
		assertEquals(3, row.get("items_count"));
		assertEquals("首页,新品", row.get("top_level_item_names"));
	}

	@Test
	@DisplayName("B9：菜单不存在先于入参解析/空数组判断抛出")
	void batchSort_menuMissing_throwsBeforeParsingSorts() {
		WebMenuItemBatchSortService service = new WebMenuItemBatchSortService(webMenuMapper, webMenuItemMapper);
		when(webMenuMapper.selectOne(any())).thenReturn(null);
		JsonNode emptyBody = objectMapper.createObjectNode();

		ResourceException ex = assertThrows(ResourceException.class, () -> service.batchSort(9L, 1L, emptyBody));
		assertEquals("菜单不存在", ex.getMessage());
	}

	@Test
	@DisplayName("C1：菜单不存在复用 ResourceException，而不是引入 NotFoundException")
	void frontRead_menuMissing_usesResourceException() {
		WebMenuFrontReadService service = new WebMenuFrontReadService(
				webMenuMapper,
				webMenuItemMapper,
				new WebMenuTreeBuilder(),
				new WebMenuItemResponseMapper(objectMapper));
		when(webMenuMapper.selectOne(any())).thenReturn(null);

		ResourceException ex = assertThrows(ResourceException.class, () -> service.byKey(1L, "missing"));
		assertEquals("菜单不存在", ex.getMessage());
	}

	@Test
	@DisplayName("C1：前台树不输出 parent_id/status，且保留 children")
	void frontRead_activeTree_omitsParentIdAndStatus() {
		WebMenuFrontReadService service = new WebMenuFrontReadService(
				webMenuMapper,
				webMenuItemMapper,
				new WebMenuTreeBuilder(),
				new WebMenuItemResponseMapper(objectMapper));
		WebMenu menu = new WebMenu();
		menu.setId(10L);
		menu.setCompanyId(1L);
		menu.setName("顶部导航");
		menu.setKey("top_nav");
		menu.setStatus(1);
		when(webMenuMapper.selectOne(any())).thenReturn(menu);
		when(webMenuItemMapper.selectList(any())).thenReturn(List.of(
				topLevelItem(1L, 10L, 0L, "首页", 1, 1),
				childItem(2L, 10L, 1L, "商品分类", 2, 1)));

		Map<String, Object> result = service.byKey(1L, "top_nav");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
		assertEquals(1, items.size());
		Map<String, Object> root = items.get(0);
		assertFalse(root.containsKey("parent_id"));
		assertFalse(root.containsKey("status"));
		assertInstanceOf(List.class, root.get("children"));
	}

	private static WebMenuItem topLevelItem(long id, long menuId, long parentId, String name, int sort, int status) {
		WebMenuItem item = new WebMenuItem();
		item.setId(id);
		item.setMenuId(menuId);
		item.setCompanyId(1L);
		item.setParentId(parentId);
		item.setName(name);
		item.setLinkType("url");
		item.setSort(sort);
		item.setStatus(status);
		return item;
	}

	private static WebMenuItem childItem(long id, long menuId, long parentId, String name, int sort, int status) {
		return topLevelItem(id, menuId, parentId, name, sort, status);
	}
}
