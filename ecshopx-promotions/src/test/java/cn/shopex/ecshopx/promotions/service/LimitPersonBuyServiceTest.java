package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.LimitPersonPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitPersonPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LimitPersonBuyServiceTest {

	@Mock
	private LimitPersonPromotionsMapper limitPersonPromotionsMapper;

	@Mock
	private LimitPromotionsMapper limitPromotionsMapper;

	@InjectMocks
	private LimitPersonBuyService limitPersonBuyService;

	@Test
	void createLimitPerson_whenNoExistingRow_insertsWithActivityWindowWhenDayZero() {
		int now = (int) (System.currentTimeMillis() / 1000L);
		when(limitPersonPromotionsMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		LimitPromotions limit = new LimitPromotions();
		limit.setLimitId(10L);
		limit.setStartTime(now - 100);
		limit.setEndTime(now + 1000);
		when(limitPromotionsMapper.selectById(10L)).thenReturn(limit);

		limitPersonBuyService.createLimitPerson(
				Map.of(
						"limit_id",
						10L,
						"user_id",
						1172L,
						"item_id",
						7670L,
						"company_id",
						141L,
						"number",
						2L,
						"day",
						0,
						"distributor_id",
						0L));

		ArgumentCaptor<LimitPersonPromotions> captor = ArgumentCaptor.forClass(LimitPersonPromotions.class);
		verify(limitPersonPromotionsMapper).insert(captor.capture());
		LimitPersonPromotions inserted = captor.getValue();
		assertThat(inserted.getLimitId()).isEqualTo(10L);
		assertThat(inserted.getUserId()).isEqualTo(1172L);
		assertThat(inserted.getItemId()).isEqualTo(7670L);
		assertThat(inserted.getCompanyId()).isEqualTo(141L);
		assertThat(inserted.getNumber()).isEqualTo(2L);
		assertThat(inserted.getStartTime()).isEqualTo(now - 100);
		assertThat(inserted.getEndTime()).isEqualTo(now + 1000);
		verify(limitPersonPromotionsMapper, never()).update(any(), any());
	}

	@Test
	void createLimitPerson_whenExistingRow_accumulatesNumber() {
		LimitPersonPromotions existing = new LimitPersonPromotions();
		existing.setId(99L);
		existing.setNumber(3L);
		when(limitPersonPromotionsMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

		limitPersonBuyService.createLimitPerson(
				Map.of(
						"limit_id",
						10L,
						"user_id",
						1172L,
						"item_id",
						7670L,
						"company_id",
						141L,
						"number",
						2L,
						"day",
						0,
						"distributor_id",
						0L));

		verify(limitPersonPromotionsMapper, never()).insert(any(LimitPersonPromotions.class));
		ArgumentCaptor<LimitPersonPromotions> updateCaptor = ArgumentCaptor.forClass(LimitPersonPromotions.class);
		verify(limitPersonPromotionsMapper).updateById(updateCaptor.capture());
		assertThat(updateCaptor.getValue().getNumber()).isEqualTo(5L);
		assertThat(updateCaptor.getValue().getId()).isEqualTo(99L);
	}

	@Test
	void getLimitPersonBuyNumber_sumsActiveActivityRows() {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LimitPersonPromotions row = new LimitPersonPromotions();
		row.setLimitId(10L);
		row.setNumber(4L);
		when(limitPersonPromotionsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row));
		LimitPromotions limit = new LimitPromotions();
		limit.setStartTime(now - 10);
		limit.setEndTime(now + 1000);
		when(limitPromotionsMapper.selectById(10L)).thenReturn(limit);

		long bought = limitPersonBuyService.getLimitPersonBuyNumber(141L, 1172L, 7670L);

		assertThat(bought).isEqualTo(4L);
	}

	@Test
	void getLimitPersonBuyNumber_returnsZeroWhenNoRows() {
		when(limitPersonPromotionsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		assertThat(limitPersonBuyService.getLimitPersonBuyNumber(141L, 1172L, 7670L)).isZero();
	}
}
