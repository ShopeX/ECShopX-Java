package cn.shopex.ecshopx.theme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.theme.domain.ThemePcTemplate;
import cn.shopex.ecshopx.theme.domain.ThemePcTemplateContent;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateContentMapper;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateMapper;
import cn.shopex.ecshopx.theme.support.PcTemplateDecorationDefaults;
import cn.shopex.ecshopx.theme.support.PcTemplateStorageUrlNormalizer;
import cn.shopex.ecshopx.theme.support.ThemePcTemplateContentRowMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PcTemplateGetDecorationContentServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(configuration, ""), ThemePcTemplate.class);
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(configuration, ""), ThemePcTemplateContent.class);
	}

	@Mock
	private ThemePcTemplateMapper themePcTemplateMapper;

	@Mock
	private ThemePcTemplateContentMapper themePcTemplateContentMapper;

	@Mock
	private ThemePcTemplateContentLangReadService themePcTemplateContentLangReadService;

	private PcTemplateGetDecorationContentService service;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper();
		service =
				new PcTemplateGetDecorationContentService(
						themePcTemplateMapper,
						themePcTemplateContentMapper,
						new ThemePcTemplateContentRowMapper(),
						themePcTemplateContentLangReadService,
						new PcTemplateStorageUrlNormalizer(objectMapper, "http://ecshopx.test"),
						new PcTemplateDecorationDefaults(objectMapper),
						objectMapper);
	}

	@Test
	@DisplayName("前台 page_type=home 按 PHP 映射为 index，且 GET 不写库")
	@SuppressWarnings("unchecked")
	void frontHomeMapsToIndexAndDoesNotSeed() {
		when(themePcTemplateMapper.selectList(any())).thenReturn(List.of());

		Map<String, Object> data =
				service.getDecorationContent(141L, "zh-CN", null, "home", null, 0L, false);

		assertThat(data).containsEntry("id", 0).containsEntry("name", "").containsEntry("config", "");
		ArgumentCaptor<LambdaQueryWrapper<ThemePcTemplate>> cap =
				ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(themePcTemplateMapper).selectList(cap.capture());
		LambdaQueryWrapper<ThemePcTemplate> wrapper = cap.getValue();
		wrapper.getSqlSegment();
		assertThat(wrapper.getParamNameValuePairs().values()).contains("index");
		assertThat(wrapper.getParamNameValuePairs().values()).doesNotContain("home");
		verify(themePcTemplateContentMapper, never()).insert(any(ThemePcTemplateContent.class));
	}

	@Test
	@DisplayName("前台无内容行时返回空 DSL 对象，不插入默认装修")
	void frontEmptyContentDoesNotInsert() {
		ThemePcTemplate tpl = new ThemePcTemplate();
		tpl.setThemePcTemplateId(9L);
		tpl.setPageType("index");
		when(themePcTemplateMapper.selectList(any())).thenReturn(List.of(tpl));
		when(themePcTemplateContentMapper.selectList(any())).thenReturn(List.of());

		Map<String, Object> data =
				service.getDecorationContent(141L, "zh-CN", null, "home", null, 0L, false);

		assertThat(data).containsEntry("id", 0).containsEntry("config", "");
		verify(themePcTemplateContentMapper, never()).insert(any(ThemePcTemplateContent.class));
	}

	@Test
	@DisplayName("优先返回带 ECX_SP_WEB_DECORATION_DSL_V1 标记的内容行")
	void prefersDslMarkerRow() {
		ThemePcTemplate tpl = new ThemePcTemplate();
		tpl.setThemePcTemplateId(9L);
		tpl.setPageType("index");
		when(themePcTemplateMapper.selectList(any())).thenReturn(List.of(tpl));

		ThemePcTemplateContent legacy = new ThemePcTemplateContent();
		legacy.setThemePcTemplateContentId(1L);
		legacy.setCompanyId(141L);
		legacy.setThemePcTemplateId(9L);
		legacy.setName("old");
		legacy.setParams("{\"type\":\"W0002\"}");

		ThemePcTemplateContent dsl = new ThemePcTemplateContent();
		dsl.setThemePcTemplateContentId(2L);
		dsl.setCompanyId(141L);
		dsl.setThemePcTemplateId(9L);
		dsl.setName("");
		dsl.setParams("{\"type\":\"ECX_SP_WEB_DECORATION_DSL_V1\",\"pageType\":\"home\"}");

		when(themePcTemplateContentMapper.selectList(any())).thenReturn(List.of(legacy, dsl));

		Map<String, Object> data =
				service.getDecorationContent(141L, "zh-CN", null, "home", null, 0L, false);

		assertThat(data.get("id")).isEqualTo("2");
		assertThat(String.valueOf(data.get("config"))).contains("ECX_SP_WEB_DECORATION_DSL_V1");
		verify(themePcTemplateContentMapper, never()).insert(any(ThemePcTemplateContent.class));
	}
}
