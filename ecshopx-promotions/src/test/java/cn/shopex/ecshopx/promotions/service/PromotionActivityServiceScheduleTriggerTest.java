package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.PromotionActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionActivityMapper;
import cn.shopex.ecshopx.promotions.schedule.ScheduleFirePromotionActivityMessage;
import cn.shopex.ecshopx.promotions.schedule.ScheduleFirePromotionActivityEnqueuePort;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionActivityMultiLangReadService;
import cn.shopex.ecshopx.promotions.service.schedule.MembershipSchedulePromotionActivitySupport;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromotionActivityServiceScheduleTriggerTest {

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

	private PromotionActivityService service;

	@BeforeEach
	void setUp() {
		service =
				new PromotionActivityService(
						promotionActivityMapper,
						promotionActivityCreateService,
						promotionActivityMultiLangReadService,
						scheduleFirePromotionActivityEnqueuePort,
						membershipSchedulePromotionActivitySupport);
	}

	@Test
	@DisplayName("§3 1+2-A：仅 Schedule 三类型产生 count 查询，upgrade 两类型跳过故共 3 次 selectCount")
	void scheduleTrigger_onlyThreeSelectCountForScheduleTypes() {
		when(promotionActivityMapper.selectCount(any())).thenReturn(0L);
		assertThat(service.scheduleTrigger()).isZero();
		verify(promotionActivityMapper, times(3)).selectCount(any());
	}

	@Test
	@DisplayName("§3 3-5：某类型 0 行不拉列表、不投递")
	void scheduleTrigger_zeroTotal_skipsPageAndEnqueue() {
		when(promotionActivityMapper.selectCount(any())).thenReturn(0L);
		service.scheduleTrigger();
		verify(promotionActivityMapper, never()).selectPage(any(), any());
		verify(scheduleFirePromotionActivityEnqueuePort, never()).enqueue(any());
	}

	@Test
	@DisplayName("§3 8+9+10(假)：经过 scheduleTriggerFireToJob 但 isTrigger 假不投递")
	void scheduleTrigger_isTriggerFalse_noEnqueue() {
		when(promotionActivityMapper.selectCount(any()))
				.thenReturn(1L)
				.thenReturn(0L)
				.thenReturn(0L);
		PromotionActivity e = new PromotionActivity();
		e.setActivityId(1L);
		Page<PromotionActivity> p = new Page<>(1, 50);
		p.setRecords(List.of(e));
		p.setTotal(1);
		when(promotionActivityMapper.selectPage(any(), any())).thenReturn(p);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", 10L);
		Map<String, Object> tc = new LinkedHashMap<>();
		tc.put("trigger_time", "birthday_month");
		row.put("trigger_condition", tc);
		row.put("activity_id", 1L);
		when(promotionActivityCreateService.toActivityListRow(e)).thenReturn(row);
		when(membershipSchedulePromotionActivitySupport.isTrigger(anyString(), any())).thenReturn(false);
		assertThat(service.scheduleTrigger()).isZero();
		verify(membershipSchedulePromotionActivitySupport, times(1))
				.isTrigger(eq("member_birthday"), any());
		verify(scheduleFirePromotionActivityEnqueuePort, never()).enqueue(any());
	}

	@Test
	@DisplayName("§3 8+11-14：isTrigger 真、250 人分 3 页投递、返回 3")
	void scheduleTrigger_enqueuesThreePages() {
		when(promotionActivityMapper.selectCount(any()))
				.thenReturn(1L)
				.thenReturn(0L)
				.thenReturn(0L);
		PromotionActivity e = new PromotionActivity();
		e.setActivityId(9L);
		Page<PromotionActivity> p = new Page<>(1, 50);
		p.setRecords(List.of(e));
		when(promotionActivityMapper.selectPage(any(), any())).thenReturn(p);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", 10L);
		row.put("activity_id", 9L);
		Map<String, Object> tc = new LinkedHashMap<>();
		tc.put("trigger_time", "birthday_day");
		row.put("trigger_condition", tc);
		when(promotionActivityCreateService.toActivityListRow(e)).thenReturn(row);
		when(membershipSchedulePromotionActivitySupport.isTrigger(eq("member_birthday"), any()))
				.thenReturn(true);
		when(membershipSchedulePromotionActivitySupport.countMembers(eq("member_birthday"), any(), anyLong()))
				.thenReturn(250L);
		when(scheduleFirePromotionActivityEnqueuePort.enqueue(any())).thenReturn(1L);
		assertThat(service.scheduleTrigger()).isEqualTo(3L);
		verify(scheduleFirePromotionActivityEnqueuePort, times(3)).enqueue(any());
		ArgumentCaptor<ScheduleFirePromotionActivityMessage> cap =
				ArgumentCaptor.forClass(ScheduleFirePromotionActivityMessage.class);
		verify(scheduleFirePromotionActivityEnqueuePort, times(3)).enqueue(cap.capture());
		assertThat(cap.getAllValues())
				.anyMatch(m -> m.page() == 1)
				.anyMatch(m -> m.page() == 2)
				.anyMatch(m -> m.page() == 3);
	}

	@Test
	@DisplayName("§3 7+8：有行时 applyTitles 每页一次")
	void scheduleTrigger_applyTitlesWhenHasRows() {
		when(promotionActivityMapper.selectCount(any()))
				.thenReturn(1L)
				.thenReturn(0L)
				.thenReturn(0L);
		PromotionActivity e = new PromotionActivity();
		Page<PromotionActivity> p = new Page<>(1, 50);
		p.setRecords(List.of(e));
		when(promotionActivityMapper.selectPage(any(), any())).thenReturn(p);
		Map<String, Object> row = Map.of("company_id", 1L, "trigger_condition", Map.of("trigger_time", "birthday_day"));
		when(promotionActivityCreateService.toActivityListRow(e)).thenReturn(new LinkedHashMap<>(row));
		when(membershipSchedulePromotionActivitySupport.isTrigger(anyString(), any())).thenReturn(false);
		service.scheduleTrigger();
		verify(promotionActivityMultiLangReadService, times(1)).applyTitles(anyList(), eq("zh-CN"));
	}

	@Test
	@DisplayName("§3 10(真)+12-13：countMembers=0 不投递")
	void scheduleTrigger_zeroMembers_noEnqueue() {
		when(promotionActivityMapper.selectCount(any()))
				.thenReturn(1L)
				.thenReturn(0L)
				.thenReturn(0L);
		PromotionActivity e = new PromotionActivity();
		Page<PromotionActivity> p = new Page<>(1, 50);
		p.setRecords(List.of(e));
		when(promotionActivityMapper.selectPage(any(), any())).thenReturn(p);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", 10L);
		row.put("activity_id", 1L);
		row.put("trigger_condition", Map.of("trigger_time", "birthday_day"));
		when(promotionActivityCreateService.toActivityListRow(e)).thenReturn(row);
		when(membershipSchedulePromotionActivitySupport.isTrigger(anyString(), any())).thenReturn(true);
		when(membershipSchedulePromotionActivitySupport.countMembers(anyString(), any(), anyLong()))
				.thenReturn(0L);
		assertThat(service.scheduleTrigger()).isZero();
		verify(scheduleFirePromotionActivityEnqueuePort, never()).enqueue(any());
	}
}
