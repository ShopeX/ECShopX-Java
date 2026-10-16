package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTaskRewardsDisplayServiceTest {

	@Mock
	private DiscountCardsMapper discountCardsMapper;

	private MemberTaskRewardsDisplayService service;

	@BeforeEach
	void setUp() {
		service = new MemberTaskRewardsDisplayService(discountCardsMapper, new ObjectMapper());
	}

	@Test
	void buildRewards_pointsAndCoupon() {
		DiscountCards card = new DiscountCards();
		card.setTitle("20立减券");
		when(discountCardsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(card);

		List<Map<String, Object>> rewards =
				service.buildRewards(1L, "{\"points\":50,\"coupon_id\":88}");

		assertThat(rewards).hasSize(2);
		assertThat(rewards.get(0).get("kind")).isEqualTo("points");
		assertThat(rewards.get(0).get("points")).isEqualTo(50L);
		assertThat(rewards.get(1).get("kind")).isEqualTo("coupon");
		assertThat(rewards.get(1).get("coupon_id")).isEqualTo(88L);
		assertThat(rewards.get(1).get("title")).isEqualTo("20立减券");
	}

	@Test
	void formatRewardsMultiline_joinsWithNewline() {
		List<Map<String, Object>> rewards =
				service.buildRewards(1L, "{\"points\":5,\"coupon_id\":0}");
		String text = service.formatRewardsMultiline(rewards);
		assertThat(text).isEqualTo("5积分");
	}
}
