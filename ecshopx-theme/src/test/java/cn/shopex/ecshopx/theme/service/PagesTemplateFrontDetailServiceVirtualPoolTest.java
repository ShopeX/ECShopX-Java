package cn.shopex.ecshopx.theme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.domain.PagesTemplateSet;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateSetMapper;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateFrontDetailQuery;
import cn.shopex.ecshopx.theme.support.PagesTemplateSetRowMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagesTemplateFrontDetailServiceVirtualPoolTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, PagesTemplate.class);
		TableInfoHelper.initTableInfo(assistant, PagesTemplateSet.class);
	}

	@Mock
	private PagesTemplateSetMapper pagesTemplateSetMapper;
	@Mock
	private PagesTemplateMapper pagesTemplateMapper;
	@Mock
	private PagesTemplateSetRowMapper pagesTemplateSetRowMapper;
	@Mock
	private PagesTemplateSetOutsideTabBarReadService pagesTemplateSetOutsideTabBarReadService;
	@Mock
	private PagesTemplateDecoratorContentService pagesTemplateDecoratorContentService;
	@Mock
	private PagesTemplateDecoratorMemberTagsFilterService pagesTemplateDecoratorMemberTagsFilterService;
	@Mock
	private PagesTemplateDecoratorHandlerService pagesTemplateDecoratorHandlerService;
	@Mock
	private PagesTemplateDecoratorFilterService pagesTemplateDecoratorFilterService;
	@Mock
	private LangueProperties langueProperties;
	@Mock
	private DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;

	private PagesTemplateFrontDetailService service;

	@BeforeEach
	void setUp() {
		service =
				new PagesTemplateFrontDetailService(
						pagesTemplateSetMapper,
						pagesTemplateMapper,
						pagesTemplateSetRowMapper,
						pagesTemplateSetOutsideTabBarReadService,
						pagesTemplateDecoratorContentService,
						pagesTemplateDecoratorMemberTagsFilterService,
						pagesTemplateDecoratorHandlerService,
						pagesTemplateDecoratorFilterService,
						langueProperties,
						distributorGetInfoSimpleByDistributorIdPort);
	}

	@Test
	@DisplayName("店铺首页 + 虚拟店 269：加载平台池启用模版 10392")
	void detail_shopIndexMode_virtualDistributorUsesPoolTemplate() {
		long companyId = 38L;
		int distributorId = 269;

		PagesTemplateSet globalSet = new PagesTemplateSet();
		globalSet.setCompanyId(companyId);
		globalSet.setIndexType(2);

		PagesTemplate poolTemplate = new PagesTemplate();
		poolTemplate.setCompanyId(companyId);
		poolTemplate.setDistributorId(0);
		poolTemplate.setPagesTemplateId(10_392L);
		poolTemplate.setWeappPages("index");
		poolTemplate.setStatus(1);

		when(pagesTemplateSetMapper.selectOne(any())).thenReturn(globalSet, (PagesTemplateSet) null);
		when(distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(companyId, distributorId))
				.thenReturn(Map.of("distributor_self", 1));
		when(pagesTemplateMapper.selectOne(any())).thenReturn(poolTemplate);
		when(pagesTemplateDecoratorContentService.buildDecoratedComponentRows(
						any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(Collections.emptyList());

		PagesTemplateFrontDetailQuery query =
				new PagesTemplateFrontDetailQuery(
						companyId,
						0L,
						0L,
						distributorId,
						"index",
						"yykweishop",
						"v1.0.2",
						1,
						50,
						null,
						null,
						0L,
						0L);

		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) service.detail(query, "zh-CN");
		assertEquals(List.of(), body.get("list"));
		verify(pagesTemplateDecoratorContentService)
				.buildDecoratedComponentRows(
						eq(companyId),
						eq(10_392L),
						eq("yykweishop"),
						eq("v1.0.2"),
						eq("zh-CN"),
						eq(0L),
						eq(distributorId),
						eq(0L),
						any(),
						any(),
						eq(1),
						eq(50));
	}
}