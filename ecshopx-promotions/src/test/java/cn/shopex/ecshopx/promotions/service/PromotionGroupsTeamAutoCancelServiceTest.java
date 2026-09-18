package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromotionGroupsTeamAutoCancelServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(new MybatisConfiguration(), ""), PromotionGroupsTeam.class);
	}

	@Mock
	private PromotionGroupsTeamMapper promotionGroupsTeamMapper;

	@Mock
	private PromotionGroupsTeamFailService promotionGroupsTeamFailService;

	@InjectMocks
	private PromotionGroupsTeamAutoCancelService service;

	@Test
	void step1_step2_noTeams_earlyExit() {
		when(promotionGroupsTeamMapper.selectCount(any())).thenReturn(0L);
		assertThat(service.scheduleAutoCancelGroupOrders()).isZero();
		verify(promotionGroupsTeamMapper, never()).selectPage(any(), any());
		verify(promotionGroupsTeamFailService, never()).teamFail(any());
	}

	@Test
	void step1_selectCountUsesStatusAndEndTimeInSql() {
		when(promotionGroupsTeamMapper.selectCount(any())).thenReturn(0L);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaQueryWrapper<PromotionGroupsTeam>> cap =
				ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		assertThat(service.scheduleAutoCancelGroupOrders()).isZero();
		verify(promotionGroupsTeamMapper).selectCount(cap.capture());
		assertThat(cap.getValue().getSqlSegment()).contains("team_status");
		assertThat(cap.getValue().getSqlSegment()).contains("end_time");
	}

	@Test
	void step2_step3_onePage_delegatesTeamFailPerRow() {
		when(promotionGroupsTeamMapper.selectCount(any())).thenReturn(3L);
		Page<PromotionGroupsTeam> page = new Page<>(1, 100);
		List<PromotionGroupsTeam> rows = new ArrayList<>();
		rows.add(team("a"));
		rows.add(team("b"));
		rows.add(team("c"));
		page.setRecords(rows);
		when(promotionGroupsTeamMapper.selectPage(any(), any())).thenReturn(page);
		assertThat(service.scheduleAutoCancelGroupOrders()).isEqualTo(3);
		verify(promotionGroupsTeamMapper, times(1)).selectPage(any(), any());
		verify(promotionGroupsTeamFailService, times(1)).teamFail("a");
		verify(promotionGroupsTeamFailService, times(1)).teamFail("b");
		verify(promotionGroupsTeamFailService, times(1)).teamFail("c");
	}

	@Test
	void step3_fixedPageOne_repeatedFetchesForTotalPage_gt100() {
		when(promotionGroupsTeamMapper.selectCount(any())).thenReturn(150L);
		Page<PromotionGroupsTeam> page = new Page<>(1, 100);
		List<PromotionGroupsTeam> rows = new ArrayList<>();
		IntStream.range(0, 100).forEach(i -> rows.add(team("t" + i)));
		page.setRecords(rows);
		when(promotionGroupsTeamMapper.selectPage(any(), any())).thenReturn(page);
		assertThat(service.scheduleAutoCancelGroupOrders()).isEqualTo(200);
		verify(promotionGroupsTeamMapper, times(2)).selectPage(any(), any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Page<PromotionGroupsTeam>> pageCap = ArgumentCaptor.forClass(Page.class);
		verify(promotionGroupsTeamMapper, times(2)).selectPage(pageCap.capture(), any());
		assertThat(pageCap.getAllValues()).allMatch(p -> p.getCurrent() == 1L && p.getSize() == 100L);
		verify(promotionGroupsTeamFailService, times(200)).teamFail(any());
	}

	@Test
	void step3_totalPage2_when101Teams_twoIterations() {
		when(promotionGroupsTeamMapper.selectCount(any())).thenReturn(101L);
		Page<PromotionGroupsTeam> p100 = pageOfTeams(100, 0);
		Page<PromotionGroupsTeam> p1 = pageOfTeams(1, 100);
		when(promotionGroupsTeamMapper.selectPage(any(), any())).thenReturn(p100, p1);
		assertThat(service.scheduleAutoCancelGroupOrders()).isEqualTo(101);
		verify(promotionGroupsTeamMapper, times(2)).selectPage(any(), any());
		verify(promotionGroupsTeamFailService, times(101)).teamFail(any());
	}

	@Test
	void step3_delegationToTeamFail_includes4dPath() {
		when(promotionGroupsTeamMapper.selectCount(any())).thenReturn(1L);
		Page<PromotionGroupsTeam> page = new Page<>(1, 100);
		page.setRecords(List.of(team("sole")));
		when(promotionGroupsTeamMapper.selectPage(any(), any())).thenReturn(page);
		assertThat(service.scheduleAutoCancelGroupOrders()).isEqualTo(1);
		verify(promotionGroupsTeamFailService, times(1)).teamFail(eq("sole"));
	}

	@Test
	@DisplayName("analysis §3 步骤5: 积分退还未启用 — 本入口仅扫描团并委托 teamFail，不引入独立积分/退还 bean")
	void step5_integralRefundDisabled_delegationOnly() {
		when(promotionGroupsTeamMapper.selectCount(any())).thenReturn(1L);
		Page<PromotionGroupsTeam> page = new Page<>(1, 100);
		page.setRecords(List.of(team("sole")));
		when(promotionGroupsTeamMapper.selectPage(any(), any())).thenReturn(page);
		assertThat(service.scheduleAutoCancelGroupOrders()).isEqualTo(1);
		verify(promotionGroupsTeamFailService, times(1)).teamFail("sole");
	}

	private static PromotionGroupsTeam team(String teamId) {
		PromotionGroupsTeam t = new PromotionGroupsTeam();
		t.setTeamId(teamId);
		return t;
	}

	private static Page<PromotionGroupsTeam> pageOfTeams(int n, int idOffset) {
		Page<PromotionGroupsTeam> page = new Page<>(1, 100);
		List<PromotionGroupsTeam> rows = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			rows.add(team("id" + (idOffset + i)));
		}
		page.setRecords(rows);
		return page;
	}
}
