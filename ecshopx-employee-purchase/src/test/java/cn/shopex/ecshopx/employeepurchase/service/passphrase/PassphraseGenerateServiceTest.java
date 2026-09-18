package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterprises;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassphraseGenerateServiceTest {

	@Mock
	private ActivitiesMapper activitiesMapper;

	@Mock
	private ActivityEnterprisesMapper activityEnterprisesMapper;

	@Mock
	private EnterprisesMapper enterprisesMapper;

	@Mock
	private ActivityPassphraseService activityPassphraseService;

	private PassphraseGenerateService service;

	@BeforeEach
	void setUp() {
		service =
				new PassphraseGenerateService(
						activitiesMapper,
						activityEnterprisesMapper,
						enterprisesMapper,
						activityPassphraseService);
	}

	@Test
	@DisplayName("QA-07/B1: 生成 8 位字母数字码，条数符合 count")
	void generateReturnsEightCharCodes() {
		stubEnterprise(1L, 101L);
		when(activityPassphraseService.collectUsedCodesForCompany(1L, 0L)).thenReturn(java.util.Set.of());

		Map<String, Object> result = service.generate(1L, 0L, List.of(101L), 2, null);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		assertEquals(1, list.size());
		@SuppressWarnings("unchecked")
		List<String> codes = (List<String>) list.get(0).get("passphrase_codes");
		assertEquals(2, codes.size());
		for (String code : codes) {
			assertEquals(8, code.length());
			assertTrue(code.matches("[0-9a-zA-Z]{8}"));
		}
	}

	@Test
	@DisplayName("B1: 单次企业数超过 100 拒绝")
	void rejectMoreThan100Enterprises() {
		List<Long> ids = java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList();
		assertThrows(ResourceException.class, () -> service.generate(1L, 0L, ids, 1, null));
	}

	@Test
	@DisplayName("B1: 总数超过 500 拒绝")
	void rejectTotalOver500() {
		assertThrows(
				ResourceException.class,
				() -> service.generate(1L, 0L, List.of(101L), 51, null));
	}

	@Test
	@DisplayName("B2: 企业须为活动参与企业")
	void rejectEnterpriseNotInActivity() {
		Activities activity = new Activities();
		activity.setId(150L);
		when(activitiesMapper.selectOne(any())).thenReturn(activity);
		when(activityEnterprisesMapper.selectList(any())).thenReturn(List.of());
		assertThrows(
				ResourceException.class,
				() -> service.generate(1L, 0L, List.of(101L), 1, 150L));
	}

	private void stubEnterprise(long companyId, long enterpriseId) {
		Enterprises ent = new Enterprises();
		ent.setId(enterpriseId);
		ent.setCompanyId(companyId);
		ent.setDistributorId(0);
		when(enterprisesMapper.selectOne(any())).thenReturn(ent);
	}
}
