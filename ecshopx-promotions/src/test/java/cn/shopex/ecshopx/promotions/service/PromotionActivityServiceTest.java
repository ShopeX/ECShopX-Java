package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.PromotionActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionActivityMapper;
import cn.shopex.ecshopx.promotions.schedule.ScheduleFirePromotionActivityEnqueuePort;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionActivityMultiLangReadService;
import cn.shopex.ecshopx.promotions.service.schedule.MembershipSchedulePromotionActivitySupport;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromotionActivityServiceTest {

	@Mock
	private PromotionActivityMapper promotionActivityMapper;

	@Mock
	private PromotionActivityCreateService promotionActivityCreateService;

	@Mock
	private PromotionActivityMultiLangReadService promotionActivityMultiLangReadService;

	@Mock
	private ScheduleFirePromotionActivityEnqueuePort scheduleFirePromotionActivityEnqueuePort;

	@Mock
	private MembershipSchedulePromotionActivitySupport membershipSchedulePromotionActivitySupport;

	private PromotionActivityService promotionActivityService;

	@BeforeEach
	void setUp() {
		promotionActivityService =
				new PromotionActivityService(
						promotionActivityMapper,
						promotionActivityCreateService,
						promotionActivityMultiLangReadService,
						scheduleFirePromotionActivityEnqueuePort,
						membershipSchedulePromotionActivitySupport);
	}

	@Test
	@DisplayName(
			"analysis §3 1 / §3 2-A：构造 filter（end_time<now、activity_status=valid）；0 行与 PHP updateBy 一致：不抛错、不 update、返回 0")
	void activityInvalid_zeroCount_returnsZero_noUpdate() {
		when(promotionActivityMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
		assertThat(promotionActivityService.activityInvalid()).isZero();
		verify(promotionActivityMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName(
			"analysis §3 1 / §3 2-B / §3 5 / §3 6：构造 filter 与 count 一致；有行则 update；不触碰多语言写服务；set invalid + updated；返回影响行数")
	@SuppressWarnings("unchecked")
	void activityInvalid_hasRows_updatesWithMatchingFilter() {
		when(promotionActivityMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
		when(promotionActivityMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		assertThat(promotionActivityService.activityInvalid()).isEqualTo(1);
		ArgumentCaptor<QueryWrapper<PromotionActivity>> qCap = ArgumentCaptor.forClass(QueryWrapper.class);
		verify(promotionActivityMapper).selectCount(qCap.capture());
		String qSeg = qCap.getValue().getSqlSegment();
		assertThat(qSeg).contains("end_time").contains("activity_status");
		assertThat(qCap.getValue().getParamNameValuePairs().values()).contains("valid");
		ArgumentCaptor<UpdateWrapper<PromotionActivity>> uCap = ArgumentCaptor.forClass(UpdateWrapper.class);
		verify(promotionActivityMapper).update(isNull(), uCap.capture());
		assertThat(uCap.getValue().getSqlSet()).contains("activity_status").contains("updated");
		assertThat(uCap.getValue().getParamNameValuePairs().values()).contains("invalid");
	}

	@Test
	@DisplayName(
			"analysis §3 1 / §3 2-B / §3 5 / §3 6：多行一次 update；不触碰多语言写服务；返回影响行数")
	@SuppressWarnings("unchecked")
	void activityInvalid_multipleRows_returnsUpdateRows() {
		when(promotionActivityMapper.selectCount(any(QueryWrapper.class))).thenReturn(2L);
		when(promotionActivityMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(2);
		assertThat(promotionActivityService.activityInvalid()).isEqualTo(2);
	}
}
