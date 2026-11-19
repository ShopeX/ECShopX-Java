package cn.shopex.ecshopx.distribution.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.domain.DistributorAftersalesAddress;
import cn.shopex.ecshopx.distribution.mapper.DistributorAftersalesAddressMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributorDefaultAftersalesAddressReadPortImplTest {

	@Mock
	private DistributorAftersalesAddressMapper mapper;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(configuration, ""), DistributorAftersalesAddress.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findDefaultLogisticsReturnAddressIdFiltersLogisticsAndDefault() {
		DistributorAftersalesAddress row = new DistributorAftersalesAddress();
		row.setAddressId(88L);
		when(mapper.selectOne(any())).thenReturn(row);
		DistributorDefaultAftersalesAddressReadPortImpl port =
				new DistributorDefaultAftersalesAddressReadPortImpl(mapper);

		Optional<Long> found = port.findDefaultLogisticsReturnAddressId(10L, 7L);

		assertEquals(Optional.of(88L), found);
		ArgumentCaptor<LambdaQueryWrapper<DistributorAftersalesAddress>> query =
				ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(mapper).selectOne(query.capture());
		String sql = query.getValue().getCustomSqlSegment();
		assertTrue(sql.contains("company_id"));
		assertTrue(sql.contains("distributor_id"));
		assertTrue(sql.contains("return_type"));
		assertTrue(sql.contains("is_default"));
		assertTrue(query.getValue().getParamNameValuePairs().containsValue("logistics"));
		assertTrue(query.getValue().getParamNameValuePairs().containsValue(1));
	}
}
