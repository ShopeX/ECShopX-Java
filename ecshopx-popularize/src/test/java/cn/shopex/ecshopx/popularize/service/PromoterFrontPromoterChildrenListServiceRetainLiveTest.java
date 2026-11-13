package cn.shopex.ecshopx.popularize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.mapper.PromoterIdentityMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PromoterFrontPromoterChildrenListServiceRetainLiveTest {

	@Test
	void retainLiveTeamMemberRows_excludesDisabledAndLoggedOutMembers() {
		PromoterMapper promoterMapper = mock(PromoterMapper.class);
		PromoterGradeService promoterGradeService = mock(PromoterGradeService.class);
		MemberAccountService memberAccountService = mock(MemberAccountService.class);
		PromoterIdentityMapper promoterIdentityMapper = mock(PromoterIdentityMapper.class);

		PromoterFrontPromoterChildrenListService service =
				new PromoterFrontPromoterChildrenListService(
						promoterMapper,
						promoterGradeService,
						memberAccountService,
						promoterIdentityMapper,
						false);

		Map<String, Object> active = new LinkedHashMap<>();
		active.put("user_id", 101L);
		active.put("disabled", 0);

		Map<String, Object> disabled = new LinkedHashMap<>();
		disabled.put("user_id", 102L);
		disabled.put("disabled", 1);

		Map<String, Object> loggedOut = new LinkedHashMap<>();
		loggedOut.put("user_id", 103L);
		loggedOut.put("disabled", 0);

		when(memberAccountService.filterActiveMemberUserIds(eq(1L), any()))
				.thenReturn(Set.of(101L));

		List<Map<String, Object>> kept =
				service.retainLiveTeamMemberRows(
						1L, List.of(active, disabled, loggedOut));

		assertThat(kept).hasSize(1);
		assertThat(kept.get(0).get("user_id")).isEqualTo(101L);
	}
}
