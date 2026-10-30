package cn.shopex.ecshopx.orders.service.shippingtemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import cn.shopex.ecshopx.orders.domain.ShippingTemplates;
import cn.shopex.ecshopx.orders.mapper.ShippingTemplatesMapper;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
class ShippingTemplateAdminCreateServiceLangTest {

	@Mock
	private ShippingTemplatesMapper shippingTemplatesMapper;
	@Mock
	private ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	@Mock
	private CommonLangModWriteService commonLangModWriteService;
	@Mock
	private CommonLangModReadService commonLangModReadService;

	private LangueProperties langueProperties;
	private ShippingTemplateAdminCreateService service;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), ShippingTemplates.class);
	}

	@BeforeEach
	void setUp() {
		langueProperties = new LangueProperties();
		langueProperties.setList(List.of("zh-CN", "en-CN", "ar-SA", "zh-TW", "ja-JP", "ru-RU"));
		langueProperties.setDefaultLang("zh-CN");
		service =
				new ShippingTemplateAdminCreateService(
						shippingTemplatesMapper,
						shippingTemplatesQueryRepository,
						new ObjectMapper(),
						langueProperties,
						commonLangModWriteService,
						commonLangModReadService);
	}

	@Test
	@DisplayName("非默认语更新：不改主表 name，只写多语言表")
	void updateNonDefaultLang_skipsMainName_writesLangMod() {
		ShippingTemplates existing = baseEntity("主表中文名");
		when(shippingTemplatesQueryRepository.findByTemplateBusinessKey("324", 1L, 0L, 0L))
				.thenReturn(Optional.of(existing));
		when(shippingTemplatesMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> body = freeShippingBody("包邮繁体");
		service.updateShippingTemplates("324", 1L, 0L, 0L, body, "zh-TW");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaUpdateWrapper<ShippingTemplates>> cap =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(shippingTemplatesMapper).update(any(), cap.capture());
		assertThat(cap.getValue().getSqlSet()).doesNotContain("name=");

		verify(commonLangModWriteService)
				.updateLangData(
						eq(1),
						eq(Map.of("name", "包邮繁体")),
						eq("shipping_templates"),
						eq(324),
						eq("shipping_templates"),
						eq("zh-TW"));
		verify(shippingTemplatesQueryRepository, never())
				.findTemplateIdByNameCompanySupplierAndDistributor(anyLong(), anyString(), anyLong(), anyLong());
	}

	@Test
	@DisplayName("默认语更新：改主表 name，并同步多语言表")
	void updateDefaultLang_setsMainName_andLangMod() {
		ShippingTemplates existing = baseEntity("旧中文名");
		when(shippingTemplatesQueryRepository.findByTemplateBusinessKey("324", 1L, 0L, 0L))
				.thenReturn(Optional.of(existing));
		when(shippingTemplatesMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> body = freeShippingBody("新中文名");
		service.updateShippingTemplates("324", 1L, 0L, 0L, body, "zh-CN");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaUpdateWrapper<ShippingTemplates>> cap =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(shippingTemplatesMapper).update(any(), cap.capture());
		assertThat(cap.getValue().getSqlSet()).contains("name=");

		verify(commonLangModWriteService)
				.updateLangData(
						eq(1),
						eq(Map.of("name", "新中文名")),
						eq("shipping_templates"),
						eq(324),
						eq("shipping_templates"),
						eq("zh-CN"));
	}

	private static ShippingTemplates baseEntity(String name) {
		ShippingTemplates e = new ShippingTemplates();
		e.setTemplateId(324L);
		e.setCompanyId(1L);
		e.setDistributorId(0L);
		e.setSupplierId(0L);
		e.setName(name);
		e.setIsFree("1");
		e.setValuation("1");
		e.setStatus(true);
		e.setNopostConf("[]");
		return e;
	}

	private static Map<String, Object> freeShippingBody(String name) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", name);
		body.put("is_free", "1");
		body.put("valuation", "1");
		body.put("status", "1");
		body.put("nopost_conf", List.of("450000"));
		body.put("fee_conf", List.of(Map.of("start_standard", "", "start_fee", "", "add_standard", "", "add_fee", "")));
		body.put("free_conf", List.of(Map.of("area", "0", "freetype", "1", "inweight", "", "upmoney", "")));
		return body;
	}
}
