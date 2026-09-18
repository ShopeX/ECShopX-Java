package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseDistributorItemsQueryMapper;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseAdminService;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseSyncService;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.ItemsRelCatsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ActivityCreateServiceAheadActivityTest {

	@Mock private ActivitiesMapper activitiesMapper;
	@Mock private ActivityEnterprisesMapper activityEnterprisesMapper;
	@Mock private EmployeePurchaseActivityItemWriteService employeePurchaseActivityItemWriteService;
	@Mock private ItemsMapper itemsMapper;
	@Mock private ItemsRelCatsMapper itemsRelCatsMapper;
	@Mock private ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	@Mock private ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	@Mock private ItemsCategoryRepository itemsCategoryRepository;
	@Mock private EmployeePurchaseDistributorItemsQueryMapper employeePurchaseDistributorItemsQueryMapper;
	@Mock private ActivityItemsMapper activityItemsMapper;
	@Mock private ActivityPassphraseSyncService activityPassphraseSyncService;
	@Mock private ActivityPassphraseAdminService activityPassphraseAdminService;
	@Mock private EnterpriseConfigService enterpriseConfigService;
	@Mock
	private EmployeePurchaseActivityItemsCategoryRedisService
			employeePurchaseActivityItemsCategoryRedisService;

	private ActivityCreateService service;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(new MybatisConfiguration(), ""), Activities.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new ActivityCreateService(
						activitiesMapper,
						activityEnterprisesMapper,
						new ObjectMapper(),
						employeePurchaseActivityItemWriteService,
						itemsMapper,
						itemsRelCatsMapper,
						itemsCategoryItemIdResolver,
						itemsCategoryDistributorIdResolver,
						itemsCategoryRepository,
						employeePurchaseDistributorItemsQueryMapper,
						activityItemsMapper,
						activityPassphraseSyncService,
						activityPassphraseAdminService,
						enterpriseConfigService,
						employeePurchaseActivityItemsCategoryRedisService);
	}

	@Test
	@DisplayName("提前开始只更新 employee_begin_time，不误写 is_passphrase_enabled")
	void aheadActivity_updatesOnlyEmployeeBeginTime() {
		int now = (int) (System.currentTimeMillis() / 1000);
		Activities row = new Activities();
		row.setId(170L);
		row.setCompanyId(141L);
		row.setStatus("active");
		row.setDisplayTime(now - 3600);
		row.setEmployeeBeginTime(now + 3600);
		row.setIsPassphraseEnabled(true);

		when(activitiesMapper.selectOne(any())).thenReturn(row);
		when(activitiesMapper.update(isNull(), any())).thenReturn(1);

		service.aheadActivity("170", Map.of("company_id", 141L));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Activities>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(activitiesMapper).update(isNull(), captor.capture());
		assertTrue(captor.getValue() instanceof LambdaUpdateWrapper);
		String sqlSet = ((LambdaUpdateWrapper<Activities>) captor.getValue()).getSqlSet();
		assertTrue(sqlSet != null && sqlSet.contains("employee_begin_time"));
		assertFalse(sqlSet.contains("is_passphrase_enabled"));
		assertFalse(sqlSet.contains("if_relative_join"));
		assertFalse(sqlSet.contains("status"));
	}
}
