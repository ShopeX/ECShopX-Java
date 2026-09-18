package cn.shopex.ecshopx.kaquan.service.discount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.salesperson.service.SalespersonTaskCouponCompleteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDiscountCardPackageGrantServiceGradeIdsTest {

	@Mock private UserDiscountMapper userDiscountMapper;
	@Mock private DiscountCardsMapper discountCardsMapper;
	@Mock private RelItemsMapper relItemsMapper;
	@Mock private MemberAccountService memberAccountService;
	@Mock private VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	@Mock private SalespersonTaskCouponCompleteService salespersonTaskCouponCompleteService;
	@Mock private DiscountCardsRowMapperService discountCardsRowMapperService;

	@Test
	@DisplayName("空列表 / [] 不算等级限制")
	void emptyGradeIdsIsUnrestricted() {
		assertThat(UserDiscountCardPackageGrantService.hasIdRestriction(null)).isFalse();
		assertThat(UserDiscountCardPackageGrantService.hasIdRestriction("")).isFalse();
		assertThat(UserDiscountCardPackageGrantService.hasIdRestriction("[]")).isFalse();
		assertThat(UserDiscountCardPackageGrantService.hasIdRestriction(List.of())).isFalse();
		assertThat(UserDiscountCardPackageGrantService.splitIds(List.of())).isEmpty();
		assertThat(UserDiscountCardPackageGrantService.splitIds("[]")).isEmpty();
	}

	@Test
	@DisplayName("List 等级 ID 按元素比较，而不是 String.valueOf 成 [1]")
	void listGradeIdsSplitByElement() {
		assertThat(UserDiscountCardPackageGrantService.splitIds(List.of("1"))).containsExactly("1");
		assertThat(UserDiscountCardPackageGrantService.splitIds(List.of(1))).containsExactly("1");
		assertThat(UserDiscountCardPackageGrantService.hasIdRestriction(List.of("1"))).isTrue();
		assertThat(UserDiscountCardPackageGrantService.splitIds("1,2")).containsExactly("1", "2");
	}

	@Test
	@DisplayName("checkCardList：详情里 grade_ids 为 List[1] 时匹配会员等级 1")
	void checkCardList_allowsWhenListGradeMatchesMember() {
		UserDiscountCardPackageGrantService service =
				new UserDiscountCardPackageGrantService(
						userDiscountMapper,
						discountCardsMapper,
						relItemsMapper,
						memberAccountService,
						vipGradeUserVipGradeGetService,
						salespersonTaskCouponCompleteService,
						discountCardsRowMapperService,
						new ObjectMapper());
		when(userDiscountMapper.countIssuedGroupByCardId(eq(1L), anyList())).thenReturn(List.of());
		when(userDiscountMapper.countIssuedGroupByCardIdForUser(eq(1L), eq(2L), anyList())).thenReturn(List.of());
		when(memberAccountService.getMemberInfo(2L, 1L)).thenReturn(Map.of("grade_id", "1"));
		when(vipGradeUserVipGradeGetService.userVipGradeGet(eq(1L), eq(2L), anyBoolean()))
				.thenReturn(Map.of("is_open", false));

		Map<String, Object> card = new LinkedHashMap<>();
		card.put("card_id", 10L);
		card.put("give_num", 1);
		card.put("quantity", 1000);
		card.put("kq_status", 0);
		card.put("get_limit", 10);
		card.put("end_date", System.currentTimeMillis() / 1000L + 86400);
		card.put("grade_ids", List.of("1"));
		card.put("vip_grade_ids", List.of());

		List<Map<String, Object>> result = service.checkCardList(1L, 2L, List.of(card), "大转盘中奖领取");
		assertThat(result).hasSize(1);
		assertThat(result.get(0).get("success")).isEqualTo(true);
	}

	@Test
	@DisplayName("checkCardList：空 List 等级不拦截")
	void checkCardList_emptyListDoesNotBlock() {
		UserDiscountCardPackageGrantService service =
				new UserDiscountCardPackageGrantService(
						userDiscountMapper,
						discountCardsMapper,
						relItemsMapper,
						memberAccountService,
						vipGradeUserVipGradeGetService,
						salespersonTaskCouponCompleteService,
						discountCardsRowMapperService,
						new ObjectMapper());
		when(userDiscountMapper.countIssuedGroupByCardId(eq(1L), anyList())).thenReturn(List.of());
		when(userDiscountMapper.countIssuedGroupByCardIdForUser(eq(1L), eq(2L), anyList())).thenReturn(List.of());
		when(memberAccountService.getMemberInfo(2L, 1L)).thenReturn(Map.of("grade_id", "1"));
		when(vipGradeUserVipGradeGetService.userVipGradeGet(eq(1L), eq(2L), anyBoolean()))
				.thenReturn(Map.of("is_open", false));

		Map<String, Object> card = new LinkedHashMap<>();
		card.put("card_id", 10L);
		card.put("give_num", 1);
		card.put("quantity", 1000);
		card.put("kq_status", "0");
		card.put("get_limit", 10);
		card.put("end_date", "2099-01-01 00:00:00");
		card.put("grade_ids", List.of());
		card.put("vip_grade_ids", "");

		List<Map<String, Object>> result = service.checkCardList(1L, 2L, List.of(card), "大转盘中奖领取");
		assertThat(result).hasSize(1);
		assertThat(result.get(0).get("success")).isEqualTo(true);
	}
}
