package cn.shopex.ecshopx.kaquan.service.cardpackage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.kaquan.domain.CardPackage;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageMapper;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardPackageGradeLimitCheckServiceTest {

	@Mock
	private CardPackageMapper cardPackageMapper;
	@Mock
	private CardPackageDiscountCardsLoadService cardPackageDiscountCardsLoadService;
	@Mock
	private MemberCardGradeMapper memberCardGradeMapper;
	@Mock
	private VipGradeMapper vipGradeMapper;

	private CardPackageGradeLimitCheckService service;

	@BeforeEach
	void setUp() {
		service = new CardPackageGradeLimitCheckService(cardPackageMapper, cardPackageDiscountCardsLoadService,
				memberCardGradeMapper, vipGradeMapper);
	}

	@Test
	@DisplayName("仅有 vip_grade_ids 时 setType=grade 不报伪冲突")
	void gradeSetType_ignoresVipGradeIdsOnly() {
		stubPackage(1L, 10L, "礼包A");
		DiscountCards card = card(100L, "券甲", "", "9");
		when(cardPackageDiscountCardsLoadService.load(eq(1L), eq(List.of(10L))))
				.thenReturn(new CardPackageDiscountCardsLoadResult(List.of(card), Map.of()));
		stubMemberGrade(1L, 2L, "普通会员");

		List<Map<String, Object>> result = service.checkGradeLimit(1L, List.of(10L), "grade", 2L);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("仅有 grade_ids 时 setType=vip_grade 不报伪冲突")
	void vipGradeSetType_ignoresGradeIdsOnly() {
		stubPackage(1L, 10L, "礼包A");
		DiscountCards card = card(100L, "券甲", "3", "");
		when(cardPackageDiscountCardsLoadService.load(eq(1L), eq(List.of(10L))))
				.thenReturn(new CardPackageDiscountCardsLoadResult(List.of(card), Map.of()));
		stubVipGrade(1L, 8L, "黑金");

		List<Map<String, Object>> result = service.checkGradeLimit(1L, List.of(10L), "vip_grade", 8L);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("同层含其他等级 ID 仍返回冲突")
	void sameLayerMismatch_stillConflicts() {
		stubPackage(1L, 10L, "礼包A");
		DiscountCards card = card(100L, "券甲", "1,3", "");
		when(cardPackageDiscountCardsLoadService.load(eq(1L), eq(List.of(10L))))
				.thenReturn(new CardPackageDiscountCardsLoadResult(List.of(card), Map.of()));
		stubMemberGrade(1L, 2L, "普通会员");

		List<Map<String, Object>> result = service.checkGradeLimit(1L, List.of(10L), "grade", 2L);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).get("package_title")).isEqualTo("礼包A");
		assertThat(result.get(0).get("title")).isEqualTo("券甲");
		assertThat(result.get(0).get("grade_id")).isEqualTo("2");
		assertThat(result.get(0).get("grade_name")).isEqualTo("普通会员");
	}

	private void stubPackage(long companyId, long packageId, String title) {
		CardPackage pkg = new CardPackage();
		pkg.setCompanyId(companyId);
		pkg.setPackageId(packageId);
		pkg.setTitle(title);
		pkg.setRowStatus(1);
		when(cardPackageMapper.selectOne(any())).thenReturn(pkg);
	}

	private void stubMemberGrade(long companyId, long gradeId, String name) {
		MemberCardGrade g = new MemberCardGrade();
		g.setGradeId(gradeId);
		g.setGradeName(name);
		when(memberCardGradeMapper.selectList(any())).thenReturn(List.of(g));
	}

	private void stubVipGrade(long companyId, long vipGradeId, String name) {
		VipGrade v = new VipGrade();
		v.setVipGradeId(vipGradeId);
		v.setGradeName(name);
		when(vipGradeMapper.selectList(any())).thenReturn(List.of(v));
	}

	private static DiscountCards card(long cardId, String title, String gradeIds, String vipGradeIds) {
		DiscountCards c = new DiscountCards();
		c.setCardId(cardId);
		c.setTitle(title);
		c.setGradeIds(gradeIds);
		c.setVipGradeIds(vipGradeIds);
		return c;
	}
}
