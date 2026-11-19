package cn.shopex.ecshopx.companys.service.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.domain.Article;
import cn.shopex.ecshopx.companys.mapper.ArticleMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
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

@ExtendWith(MockitoExtension.class)
class ArticleUpdateServiceDefaultLangTest {

	private static final String OLD_IMG = "https://example.com/old-slider.jpg";
	private static final String NEW_IMG = "https://example.com/new-slider.jpg";

	@Mock
	private ArticleMapper articleMapper;
	@Mock
	private CommonLangModWriteService commonLangModWriteService;

	private LangueProperties langueProperties;
	private ArticleUpdateService service;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Article.class);
	}

	@BeforeEach
	void setUp() {
		langueProperties = new LangueProperties();
		langueProperties.setList(List.of("zh-CN", "en-CN"));
		langueProperties.setDefaultLang("zh-CN");
		service =
				new ArticleUpdateService(
						articleMapper, langueProperties, commonLangModWriteService, new ObjectMapper());
	}

	@Test
	@DisplayName("默认语 zh-CN 更新软文 content 时同步写入多语言表")
	void updateDefaultLang_syncsContentToLangMod() {
		Article existing = articleEntity(26L, 1L, OLD_IMG);
		when(articleMapper.selectOne(any())).thenReturn(existing);
		when(articleMapper.updateById(any(Article.class))).thenReturn(1);

		Map<String, Object> body = bringArticleBody(NEW_IMG);
		service.updateDataArticle("26", body, 1L, 99L, "zh-CN");

		ArgumentCaptor<Map<String, String>> langCaptor = ArgumentCaptor.forClass(Map.class);
		verify(commonLangModWriteService)
				.updateLangData(eq(1), langCaptor.capture(), eq("companys_article"), eq(26), eq("companys_article"), eq("zh-CN"));
		assertThat(langCaptor.getValue()).containsKey("content");
		assertThat(langCaptor.getValue().get("content")).contains(NEW_IMG);
	}

	private static Article articleEntity(long articleId, long companyId, String sliderImg) {
		Article a = new Article();
		a.setArticleId(articleId);
		a.setCompanyId(companyId);
		a.setTitle("t");
		a.setSummary("s");
		a.setAuthor("a");
		a.setArticleType("bring");
		a.setCategoryId(1L);
		a.setContent(
				"[{\"name\":\"slider\",\"data\":[{\"imgUrl\":\""
						+ sliderImg
						+ "\",\"media_type\":\"img\"}]}]");
		return a;
	}

	private static Map<String, Object> bringArticleBody(String sliderImg) {
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("title", "t");
		body.put("summary", "s");
		body.put("author", "a");
		body.put("category_id", 1);
		body.put("article_type", "bring");
		body.put(
				"content",
				List.of(
						Map.of(
								"name",
								"slider",
								"data",
								List.of(
										Map.of(
												"imgUrl",
												sliderImg,
												"media_type",
												"img")))));
		return body;
	}
}