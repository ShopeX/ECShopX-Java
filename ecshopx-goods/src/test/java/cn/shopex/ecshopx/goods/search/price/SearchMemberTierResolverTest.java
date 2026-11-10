/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.goods.search.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchMemberTierResolverTest {

	private static final long COMPANY_ID = 88L;

	@Mock
	private VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;

	@Mock
	private MemberCardGradeQueryService memberCardGradeQueryService;

	@Mock
	private MemberAccountService memberAccountService;

	private SearchMemberTierResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new SearchMemberTierResolver(
				vipGradeUserVipGradeGetService,
				memberCardGradeQueryService,
				memberAccountService,
				new ObjectMapper());
	}

	@Test
	void guestUsesDefaultGrade() {
		when(memberCardGradeQueryService.getDefaultGradeData(eq(COMPANY_ID)))
				.thenReturn(Map.of(
						"grade_id", 3L,
						"grade_name", "普通会员",
						"privileges", Map.of("discount", 95)));
		SearchMemberTier tier = resolver.resolve(COMPANY_ID, 0L);
		assertThat(tier).isNotNull();
		assertThat(tier.lvType()).isEqualTo("normal");
		assertThat(tier.gradeId()).isEqualTo(3L);
		assertThat(tier.discount()).isEqualTo(95);
		assertThat(tier.gradeName()).isEqualTo("普通会员");
	}

	@Test
	void guestNoDefaultGradeReturnsNull() {
		when(memberCardGradeQueryService.getDefaultGradeData(eq(COMPANY_ID))).thenReturn(null);
		assertThat(resolver.resolve(COMPANY_ID, -1L)).isNull();
	}

	@Test
	void vipUserPrefersVipGradeBucket() {
		long userId = 42L;
		when(vipGradeUserVipGradeGetService.userVipGradeGet(eq(COMPANY_ID), eq(userId), eq(false)))
				.thenReturn(Map.of(
						"is_vip", true,
						"vip_grade_id", 7L,
						"grade_name", "超级会员",
						"lv_type", "svip",
						"discount", 88));
		SearchMemberTier tier = resolver.resolve(COMPANY_ID, userId);
		assertThat(tier).isNotNull();
		assertThat(tier.lvType()).isEqualTo("svip");
		assertThat(tier.gradeId()).isEqualTo(7L);
		assertThat(tier.discount()).isEqualTo(88);
		assertThat(tier.gradeName()).isEqualTo("超级会员");
	}
}
