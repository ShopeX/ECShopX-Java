package cn.shopex.ecshopx.distribution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.domain.PickupLocation;
import cn.shopex.ecshopx.distribution.mapper.PickupLocationMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class PickupLocationListServiceDelScopeTest {

	@Mock
	private PickupLocationMapper pickupLocationMapper;
	@Mock
	private DistributorBatchApiRowQueryService distributorBatchApiRowQueryService;
	@Mock
	private DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	@Mock
	private MessageSource messageSource;

	private PickupLocationListService service;

	@BeforeAll
	static void initTableInfo() {
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(new MybatisConfiguration(), ""), PickupLocation.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new PickupLocationListService(
						pickupLocationMapper,
						distributorBatchApiRowQueryService,
						distributorListOutsideLangReadService,
						new ObjectMapper(),
						messageSource);
	}

	@Test
	void delPickupLocation_shopDistributor_usesDistributorIdScope() {
		when(pickupLocationMapper.delete(any())).thenReturn(1);

		service.delPickupLocation(38L, "distributor", "129", 10L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaQueryWrapper<PickupLocation>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(pickupLocationMapper).delete(cap.capture());
		LambdaQueryWrapper<PickupLocation> wrapper = cap.getValue();
		String sql = wrapper.getCustomSqlSegment();
		assertThat(sql).contains("company_id");
		assertThat(sql).contains("distributor_id =");
		assertThat(sql).doesNotContain("distributor_id IS NULL");
		assertThat(wrapper.getParamNameValuePairs().values()).contains(129L);
	}
}