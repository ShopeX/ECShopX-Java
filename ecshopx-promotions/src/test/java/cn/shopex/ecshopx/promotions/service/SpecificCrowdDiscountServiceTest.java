package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.SpecificCrowdDiscount;
import cn.shopex.ecshopx.promotions.mapper.SpecificCrowdDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpecificCrowdDiscountServiceTest {

	@Mock
	private SpecificCrowdDiscountMapper specificCrowdDiscountMapper;

	private SpecificCrowdDiscountService specificCrowdDiscountService;

	@BeforeEach
	void setUp() {
		specificCrowdDiscountService = new SpecificCrowdDiscountService(specificCrowdDiscountMapper);
	}

	@Test
	@DisplayName("analysis §3 步骤 1～3：筛选条件含 cycle_type=2、status=2、end_time 上界为当日零点")
	@SuppressWarnings("unchecked")
	void scheduleExpiredPromotion_filterMatchesPlan() {
		when(specificCrowdDiscountMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
		specificCrowdDiscountService.scheduleExpiredPromotion();
		ArgumentCaptor<QueryWrapper<SpecificCrowdDiscount>> cap = ArgumentCaptor.forClass(QueryWrapper.class);
		verify(specificCrowdDiscountMapper).selectCount(cap.capture());
		long expectedStart =
				LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		String seg = cap.getValue().getSqlSegment();
		assertThat(seg).contains("cycle_type").contains("status").contains("end_time");
		Map<String, Object> params = cap.getValue().getParamNameValuePairs();
		assertThat(params.values()).contains(expectedStart);
		assertThat(params.values()).anyMatch(v -> v instanceof Integer && Integer.valueOf(2).equals(v));
		assertThat(params.values()).anyMatch(v -> v instanceof Long && Long.valueOf(2L).equals(v));
	}

	@Test
	@DisplayName("analysis §3 步骤 4～5 totalPage=0、步骤 7：无 selectPage、无 update、返回 0")
	void scheduleExpiredPromotion_zeroCount_earlyExit() {
		when(specificCrowdDiscountMapper.selectCount(any())).thenReturn(0L);
		assertThat(specificCrowdDiscountService.scheduleExpiredPromotion()).isZero();
		verify(specificCrowdDiscountMapper, never()).selectPage(any(), any());
		verify(specificCrowdDiscountMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("analysis §3 步骤 6-A～6-D：单页非空 ids → update 仅 set status=4")
	@SuppressWarnings("unchecked")
	void scheduleExpiredPromotion_singlePage_updatesStatus() {
		when(specificCrowdDiscountMapper.selectCount(any())).thenReturn(1L);
		when(specificCrowdDiscountMapper.selectPage(any(Page.class), any()))
				.thenAnswer(
						invocation -> {
							Page<SpecificCrowdDiscount> p = invocation.getArgument(0);
							SpecificCrowdDiscount row = new SpecificCrowdDiscount();
							row.setId(100L);
							p.setRecords(List.of(row));
							return p;
						});
		when(specificCrowdDiscountMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		assertThat(specificCrowdDiscountService.scheduleExpiredPromotion()).isEqualTo(1);
		ArgumentCaptor<UpdateWrapper<SpecificCrowdDiscount>> uwCap = ArgumentCaptor.forClass(UpdateWrapper.class);
		verify(specificCrowdDiscountMapper).update(isNull(), uwCap.capture());
		UpdateWrapper<SpecificCrowdDiscount> uw = uwCap.getValue();
		assertThat(uw.getSqlSet()).contains("status");
		assertThat(uw.getParamNameValuePairs().values()).contains(4L);
	}

	@Test
	@DisplayName("analysis §3 步骤 6：多页；第二页 current=2；update 次数等于非空批次数")
	void scheduleExpiredPromotion_multiPage_secondPageCurrentIs2() {
		when(specificCrowdDiscountMapper.selectCount(any())).thenReturn(25L);
		when(specificCrowdDiscountMapper.selectPage(any(Page.class), any()))
				.thenAnswer(
						invocation -> {
							Page<SpecificCrowdDiscount> p = invocation.getArgument(0);
							long cur = p.getCurrent();
							List<SpecificCrowdDiscount> rows = new ArrayList<>();
							if (cur == 1) {
								for (int i = 0; i < 20; i++) {
									SpecificCrowdDiscount e = new SpecificCrowdDiscount();
									e.setId(100L + i);
									rows.add(e);
								}
							} else if (cur == 2) {
								for (int i = 0; i < 5; i++) {
									SpecificCrowdDiscount e = new SpecificCrowdDiscount();
									e.setId(200L + i);
									rows.add(e);
								}
							}
							p.setRecords(rows);
							return p;
						});
		when(specificCrowdDiscountMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(2);
		assertThat(specificCrowdDiscountService.scheduleExpiredPromotion()).isEqualTo(4);
		ArgumentCaptor<Page<SpecificCrowdDiscount>> pageCap = ArgumentCaptor.forClass(Page.class);
		verify(specificCrowdDiscountMapper, times(2)).selectPage(pageCap.capture(), any());
		List<Page<SpecificCrowdDiscount>> pages = pageCap.getAllValues();
		assertThat(pages.get(0).getCurrent()).isEqualTo(1L);
		assertThat(pages.get(1).getCurrent()).isEqualTo(2L);
		verify(specificCrowdDiscountMapper, times(2)).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("analysis §3 竞态备注：本页 records 空 → 跳过 update")
	void scheduleExpiredPromotion_emptyRecordsOnPage_skipsUpdate() {
		when(specificCrowdDiscountMapper.selectCount(any())).thenReturn(1L);
		when(specificCrowdDiscountMapper.selectPage(any(Page.class), any()))
				.thenAnswer(
						invocation -> {
							Page<SpecificCrowdDiscount> p = invocation.getArgument(0);
							p.setRecords(List.of());
							return p;
						});
		assertThat(specificCrowdDiscountService.scheduleExpiredPromotion()).isZero();
		verify(specificCrowdDiscountMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("analysis §3 步骤 7：累计 update 影响行数（两页各 2 行）")
	void scheduleExpiredPromotion_returnsSumOfUpdateRows() {
		when(specificCrowdDiscountMapper.selectCount(any())).thenReturn(25L);
		when(specificCrowdDiscountMapper.selectPage(any(Page.class), any()))
				.thenAnswer(
						invocation -> {
							Page<SpecificCrowdDiscount> p = invocation.getArgument(0);
							long cur = p.getCurrent();
							List<SpecificCrowdDiscount> rows = new ArrayList<>();
							if (cur == 1) {
								for (int i = 0; i < 20; i++) {
									SpecificCrowdDiscount e = new SpecificCrowdDiscount();
									e.setId(10L + i);
									rows.add(e);
								}
							} else {
								for (int i = 0; i < 5; i++) {
									SpecificCrowdDiscount e = new SpecificCrowdDiscount();
									e.setId(50L + i);
									rows.add(e);
								}
							}
							p.setRecords(rows);
							return p;
						});
		when(specificCrowdDiscountMapper.update(isNull(), any(UpdateWrapper.class)))
				.thenReturn(2)
				.thenReturn(2);
		assertThat(specificCrowdDiscountService.scheduleExpiredPromotion()).isEqualTo(4);
	}

	@Test
	@DisplayName("plan §2 步骤 1～2、§3 步骤 3–4：cycle_type=1、status=2、end_time 上界为当日零点")
	@SuppressWarnings("unchecked")
	void scheduleExpiredPromotionMonth_filterMatchesPlan() {
		when(specificCrowdDiscountMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
		assertThat(specificCrowdDiscountService.scheduleExpiredPromotionMonth()).isZero();
		ArgumentCaptor<QueryWrapper<SpecificCrowdDiscount>> cap = ArgumentCaptor.forClass(QueryWrapper.class);
		verify(specificCrowdDiscountMapper).selectCount(cap.capture());
		ZoneId z = ZoneId.systemDefault();
		long dayStart = LocalDate.now(z).atStartOfDay(z).toEpochSecond();
		String seg = cap.getValue().getSqlSegment();
		assertThat(seg).contains("cycle_type").contains("status").contains("end_time");
		assertThat(cap.getValue().getParamNameValuePairs().values()).contains(dayStart);
		assertThat(cap.getValue().getParamNameValuePairs().values())
				.anyMatch(v -> v instanceof Integer && Integer.valueOf(1).equals(v));
		assertThat(cap.getValue().getParamNameValuePairs().values())
				.anyMatch(v -> v instanceof Long && Long.valueOf(2L).equals(v));
	}

	@Test
	@DisplayName("analysis §3 步骤 3–4：totalCount=0 → 不 selectPage、不 update、返回 0")
	void scheduleExpiredPromotionMonth_zeroCount_earlyExit() {
		when(specificCrowdDiscountMapper.selectCount(any())).thenReturn(0L);
		assertThat(specificCrowdDiscountService.scheduleExpiredPromotionMonth()).isZero();
		verify(specificCrowdDiscountMapper, never()).selectPage(any(), any());
		verify(specificCrowdDiscountMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("analysis §3 步骤 8-A：end_time 严格小于本月初 → 单条 set start_time/end_time")
	@SuppressWarnings("unchecked")
	void scheduleExpiredPromotionMonth_branch8A_updatesMonthWindow() {
		ZoneId z = ZoneId.systemDefault();
		YearMonth ym = YearMonth.now(z);
		long monthStart = ym.atDay(1).atStartOfDay(z).toEpochSecond();
		long monthEnd = ym.atEndOfMonth().atTime(23, 59, 59).atZone(z).toEpochSecond();
		long endBefore = monthStart - 1L;
		when(specificCrowdDiscountMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
		when(specificCrowdDiscountMapper.selectPage(any(Page.class), any()))
				.thenAnswer(
						invocation -> {
							Page<SpecificCrowdDiscount> p = invocation.getArgument(0);
							SpecificCrowdDiscount row = new SpecificCrowdDiscount();
							row.setId(100L);
							row.setEndTime(endBefore);
							p.setRecords(List.of(row));
							return p;
						});
		when(specificCrowdDiscountMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		assertThat(specificCrowdDiscountService.scheduleExpiredPromotionMonth()).isEqualTo(1);
		ArgumentCaptor<UpdateWrapper<SpecificCrowdDiscount>> uwCap = ArgumentCaptor.forClass(UpdateWrapper.class);
		verify(specificCrowdDiscountMapper).update(isNull(), uwCap.capture());
		UpdateWrapper<SpecificCrowdDiscount> uw = uwCap.getValue();
		assertThat(uw.getSqlSet()).contains("start_time", "end_time");
		assertThat(uw.getParamNameValuePairs().values()).contains(monthStart, monthEnd);
	}

	@Test
	@DisplayName("analysis §3 步骤 8-B：end_time 等于本月初 → 不 update；返回 0")
	void scheduleExpiredPromotionMonth_branch8B_equalMonthStart_noUpdate() {
		ZoneId z = ZoneId.systemDefault();
		YearMonth ym = YearMonth.now(z);
		long monthStart = ym.atDay(1).atStartOfDay(z).toEpochSecond();
		when(specificCrowdDiscountMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
		when(specificCrowdDiscountMapper.selectPage(any(Page.class), any()))
				.thenAnswer(
						invocation -> {
							Page<SpecificCrowdDiscount> p = invocation.getArgument(0);
							SpecificCrowdDiscount row = new SpecificCrowdDiscount();
							row.setId(100L);
							row.setEndTime(monthStart);
							p.setRecords(List.of(row));
							return p;
						});
		assertThat(specificCrowdDiscountService.scheduleExpiredPromotionMonth()).isZero();
		verify(specificCrowdDiscountMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("analysis §3 步骤 8-B：end_time 大于本月初且仍满足外层上界 → 不 update；返回 0")
	void scheduleExpiredPromotionMonth_branch8B_afterMonthStart_noUpdate() {
		ZoneId z = ZoneId.systemDefault();
		long dayStart = LocalDate.now(z).atStartOfDay(z).toEpochSecond();
		YearMonth ym = YearMonth.now(z);
		long monthStart = ym.atDay(1).atStartOfDay(z).toEpochSecond();
		Assumptions.assumeTrue(dayStart > monthStart);
		long endBetween = monthStart + Math.min(86400L, (dayStart - monthStart) / 2);
		Assumptions.assumeTrue(endBetween > monthStart && endBetween <= dayStart);
		when(specificCrowdDiscountMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
		when(specificCrowdDiscountMapper.selectPage(any(Page.class), any()))
				.thenAnswer(
						invocation -> {
							Page<SpecificCrowdDiscount> p = invocation.getArgument(0);
							SpecificCrowdDiscount row = new SpecificCrowdDiscount();
							row.setId(100L);
							row.setEndTime(endBetween);
							p.setRecords(List.of(row));
							return p;
						});
		assertThat(specificCrowdDiscountService.scheduleExpiredPromotionMonth()).isZero();
		verify(specificCrowdDiscountMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("analysis §3 步骤 6–7：21 条 8-A、第二页、累计 21 次 update")
	@SuppressWarnings("unchecked")
	void scheduleExpiredPromotionMonth_paging21_records_secondPage() {
		ZoneId z = ZoneId.systemDefault();
		YearMonth ym = YearMonth.now(z);
		long monthStart = ym.atDay(1).atStartOfDay(z).toEpochSecond();
		long endBefore = monthStart - 1L;
		when(specificCrowdDiscountMapper.selectCount(any(QueryWrapper.class))).thenReturn(21L);
		when(specificCrowdDiscountMapper.selectPage(any(Page.class), any()))
				.thenAnswer(
						invocation -> {
							Page<SpecificCrowdDiscount> p = invocation.getArgument(0);
							long cur = p.getCurrent();
							List<SpecificCrowdDiscount> rows = new ArrayList<>();
							if (cur == 1) {
								for (int i = 0; i < 20; i++) {
									SpecificCrowdDiscount e = new SpecificCrowdDiscount();
									e.setId(100L + i);
									e.setEndTime(endBefore);
									rows.add(e);
								}
							} else if (cur == 2) {
								SpecificCrowdDiscount e = new SpecificCrowdDiscount();
								e.setId(200L);
								e.setEndTime(endBefore);
								rows.add(e);
							}
							p.setRecords(rows);
							return p;
						});
		when(specificCrowdDiscountMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		assertThat(specificCrowdDiscountService.scheduleExpiredPromotionMonth()).isEqualTo(21);
		ArgumentCaptor<Page<SpecificCrowdDiscount>> pageCap = ArgumentCaptor.forClass(Page.class);
		verify(specificCrowdDiscountMapper, times(2)).selectPage(pageCap.capture(), any());
		assertThat(pageCap.getAllValues().get(0).getCurrent()).isEqualTo(1L);
		assertThat(pageCap.getAllValues().get(1).getCurrent()).isEqualTo(2L);
		verify(specificCrowdDiscountMapper, times(21)).update(isNull(), any(UpdateWrapper.class));
	}
}
