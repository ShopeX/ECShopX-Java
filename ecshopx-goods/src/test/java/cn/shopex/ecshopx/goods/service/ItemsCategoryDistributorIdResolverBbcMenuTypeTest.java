package cn.shopex.ecshopx.goods.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ItemsCategoryDistributorIdResolverBbcMenuTypeTest {

	@Mock
	private CompanysMapper companysMapper;

	private ItemsCategoryDistributorIdResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new ItemsCategoryDistributorIdResolver(companysMapper);
		ReflectionTestUtils.setField(resolver, "defaultProductModel", "platform");
	}

	@Test
	void isBbcPlatformMenuType_trueOnlyForMenuType3() {
		Companys bbc = new Companys();
		bbc.setMenuType(3);
		when(companysMapper.selectById(38L)).thenReturn(bbc);
		assertThat(resolver.isBbcPlatformMenuType(38L)).isTrue();
	}

	@Test
	void isBbcPlatformMenuType_falseForStandardAndB2c() {
		Companys standard = new Companys();
		standard.setMenuType(4);
		when(companysMapper.selectById(1L)).thenReturn(standard);
		assertThat(resolver.isBbcPlatformMenuType(1L)).isFalse();

		Companys b2c = new Companys();
		b2c.setMenuType(2);
		when(companysMapper.selectById(2L)).thenReturn(b2c);
		assertThat(resolver.isBbcPlatformMenuType(2L)).isFalse();
	}

	@Test
	void isBbcPlatformMenuType_falseWhenMenuTypeUnsetEvenIfDefaultProductModelPlatform() {
		Companys unset = new Companys();
		unset.setMenuType(0);
		when(companysMapper.selectById(9L)).thenReturn(unset);
		assertThat(resolver.resolveProductModel(9L)).isEqualTo("platform");
		assertThat(resolver.isBbcPlatformMenuType(9L)).isFalse();
	}
}