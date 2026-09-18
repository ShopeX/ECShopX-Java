package cn.shopex.ecshopx.goods.service.items;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.goods.support.ItemLangShardJdbc;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsMultiLangWriteServiceTest {

	@Mock
	private ItemLangShardJdbc itemLangShardJdbc;

	@Mock
	private LangueProperties langueProperties;

	@InjectMocks
	private ItemsMultiLangWriteService service;

	@Test
	void resolveLang_prefersBodyCountryCode() {
		org.mockito.Mockito.when(langueProperties.resolveToSupportedTag("en-CN")).thenReturn("en-CN");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("country_code", "en-CN");
		assertThat(service.resolveLang(params)).isEqualTo("en-CN");
	}

	@Test
	void afterItemUpdate_writesPresentFieldsToShard() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("item_name", "Hard shell jacket");
		params.put("brief", "b");
		service.afterItemUpdate(1L, 7L, params, "en-CN");
		verify(itemLangShardJdbc).upsert(1L, 7L, "en-CN", "items", "items", "item_name", "Hard shell jacket");
		verify(itemLangShardJdbc).upsert(1L, 7L, "en-CN", "items", "items", "brief", "b");
		verifyNoMoreInteractions(itemLangShardJdbc);
	}

	@Test
	void afterItemCreate_writesAllThreeFields() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("item_name", "硬壳外套");
		service.afterItemCreate(1L, 7L, params, "zh-CN");
		ArgumentCaptor<String> field = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
		verify(itemLangShardJdbc, org.mockito.Mockito.times(3))
				.upsert(eq(1L), eq(7L), eq("zh-CN"), eq("items"), eq("items"), field.capture(), value.capture());
		assertThat(field.getAllValues()).containsExactly("item_name", "brief", "intro");
		assertThat(value.getAllValues()).containsExactly("硬壳外套", "", "");
	}
}
