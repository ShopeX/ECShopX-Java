package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityPassphraseEnterpriseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityPassphraseServiceSupportsTest {

	@Mock
	private ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper;

	private ActivityPassphraseService service;

	@BeforeEach
	void setUp() {
		service = new ActivityPassphraseService(activityPassphraseEnterpriseMapper);
	}

	@Test
	@DisplayName("supportsPassphraseBypassWhitelist：开口令且企业在范围内且有口令码")
	void supportsWhenConfigValid() {
		Activities activity = new Activities();
		activity.setIsPassphraseEnabled(true);
		activity.setEnterpriseId("10,20");

		ActivityPassphraseEnterprise cfg = new ActivityPassphraseEnterprise();
		cfg.setPassphraseCode("Ab12Cd34");
		when(activityPassphraseEnterpriseMapper.selectOne(any())).thenReturn(cfg);

		assertTrue(service.supportsPassphraseBypassWhitelist(activity, 1L, 100L, 10L));
	}

	@Test
	@DisplayName("supportsPassphraseBypassWhitelist：企业不在活动范围")
	void notSupportsWhenEnterpriseNotInActivity() {
		Activities activity = new Activities();
		activity.setIsPassphraseEnabled(true);
		activity.setEnterpriseId("20");

		assertFalse(service.supportsPassphraseBypassWhitelist(activity, 1L, 100L, 10L));
	}

	@Test
	@DisplayName("supportsPassphraseBypassWhitelist：口令码为空")
	void notSupportsWhenPassphraseCodeBlank() {
		Activities activity = new Activities();
		activity.setIsPassphraseEnabled(true);
		activity.setEnterpriseId("10");

		ActivityPassphraseEnterprise cfg = new ActivityPassphraseEnterprise();
		cfg.setPassphraseCode("  ");
		when(activityPassphraseEnterpriseMapper.selectOne(any())).thenReturn(cfg);

		assertFalse(service.supportsPassphraseBypassWhitelist(activity, 1L, 100L, 10L));
	}
}
