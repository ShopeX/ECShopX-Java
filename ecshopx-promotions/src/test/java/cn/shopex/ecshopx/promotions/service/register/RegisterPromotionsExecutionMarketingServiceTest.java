package cn.shopex.ecshopx.promotions.service.register;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.RegisterPromotions;
import cn.shopex.ecshopx.promotions.mapper.DistributorPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.RegisterPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterPromotionsExecutionMarketingServiceTest {

	@Mock
	private RegisterPromotionsMapper registerPromotionsMapper;

	@Mock
	private DistributorPromotionsMapper distributorPromotionsMapper;

	@Mock
	private RegisterPromotionMembercardItemsApplyService registerPromotionMembercardItemsApplyService;

	@Mock
	private RegisterPromotionMembercardCouponsApplyService registerPromotionMembercardCouponsApplyService;

	@Mock
	private cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper memberRelTagsMapper;

	@Mock
	private cn.shopex.ecshopx.members.mapper.MemberTagsMapper memberTagsMapper;

	private RegisterPromotionsExecutionMarketingService service;

	@BeforeEach
	void setUp() {
		service =
				new RegisterPromotionsExecutionMarketingService(
						registerPromotionsMapper,
						distributorPromotionsMapper,
						new ObjectMapper(),
						registerPromotionMembercardItemsApplyService,
						registerPromotionMembercardCouponsApplyService,
						memberRelTagsMapper,
						memberTagsMapper);
	}

	@Test
	void executionMarketing_appliesGeneralRegisterCouponsWhenOpen() {
		RegisterPromotions row = new RegisterPromotions();
		row.setId(2L);
		row.setCompanyId(38L);
		row.setRegisterType("general");
		row.setIsOpen("true");
		row.setPromotionsValue(
				"{\"items\":[],\"coupons\":[{\"card_id\":\"413\",\"count\":1},{\"card_id\":\"408\",\"count\":1}]}");
		when(registerPromotionsMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<RegisterPromotions>>any()))
				.thenReturn(row);

		service.executionMarketing(38L, 0L, 1172L, "13333333333");

		verify(registerPromotionMembercardCouponsApplyService)
				.applyCoupons(eq(38L), eq(1172L), eq("13333333333"), org.mockito.ArgumentMatchers.any());
		verifyNoInteractions(registerPromotionMembercardItemsApplyService);
	}

	@Test
	void executionMarketing_whenPromotionClosed_doesNothing() {
		RegisterPromotions row = new RegisterPromotions();
		row.setIsOpen("false");
		when(registerPromotionsMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<RegisterPromotions>>any()))
				.thenReturn(row);

		service.executionMarketing(38L, 0L, 1172L, "13333333333");

		verifyNoInteractions(registerPromotionMembercardCouponsApplyService);
		verifyNoInteractions(registerPromotionMembercardItemsApplyService);
	}
}
