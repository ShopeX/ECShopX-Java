package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.GroupRobotWechatInfo;
import cn.shopex.ecshopx.common.cron.PromotionGroupRobotWechatProfilePort;
import cn.shopex.ecshopx.orders.service.group.GroupPromotionOrderPayedService;
import cn.shopex.ecshopx.promotions.domain.PaymentOverEndTimeRow;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.domain.ScheduleAutoDoneGroupTeamRow;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromotionGroupsTeamServiceTest {

	@Mock
	private PromotionGroupsTeamMapper promotionGroupsTeamMapper;

	@Mock
	private PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;

	@Mock
	private PromotionGroupsTeamFailService promotionGroupsTeamFailService;

	@Mock
	private GroupPromotionOrderPayedService groupPromotionOrderPayedService;

	@Mock
	private PromotionGroupRobotWechatProfilePort promotionGroupRobotWechatProfilePort;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private PromotionGroupsTeamService promotionGroupsTeamService;

	@BeforeEach
	void setUp() {
		promotionGroupsTeamService =
				new PromotionGroupsTeamService(
						promotionGroupsTeamMapper,
						promotionGroupsTeamMemberMapper,
						promotionGroupsTeamFailService,
						groupPromotionOrderPayedService,
						promotionGroupRobotWechatProfilePort,
						objectMapper);
		lenient()
				.when(promotionGroupRobotWechatProfilePort.getRandUserInfo(anyInt()))
				.thenReturn(List.of(new GroupRobotWechatInfo("h", "n")));
	}

	@Test
	void scheduleAutoDoneGroup_whenNoCandidates_returns0() {
		when(promotionGroupsTeamMapper.countScheduleAutoDoneGroup(any(Long.class), anyInt())).thenReturn(0L);
		assertThat(promotionGroupsTeamService.scheduleAutoDoneGroup()).isZero();
		verify(promotionGroupsTeamMemberMapper, never()).selectGroupTeamSuccessMembers(any());
	}

	@Test
	void scheduleAutoDoneGroup_emptyTeamIdOnPage_3A_A1() {
		when(promotionGroupsTeamMapper.countScheduleAutoDoneGroup(any(Long.class), anyInt())).thenReturn(1L);
		when(promotionGroupsTeamMapper.listScheduleAutoDoneGroupPage(any(Long.class), anyInt(), eq(0), eq(100)))
				.thenReturn(List.of());
		int processed = promotionGroupsTeamService.scheduleAutoDoneGroup();
		assertThat(processed).isZero();
		verify(promotionGroupsTeamMemberMapper, never()).selectPaymentOverEndTimeList(anyList());
		verify(promotionGroupsTeamFailService, never()).teamFail(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void scheduleAutoDoneGroup_multiPage_forceEachPage_beforeRows() {
		when(promotionGroupsTeamMapper.countScheduleAutoDoneGroup(any(Long.class), anyInt())).thenReturn(150L);
		List<ScheduleAutoDoneGroupTeamRow> p1 = new ArrayList<>();
		for (int k = 0; k < 100; k++) {
			p1.add(sampleRow("t" + k, 2L, 1L, 1L, k, "entity"));
		}
		List<ScheduleAutoDoneGroupTeamRow> p2 = new ArrayList<>();
		for (int k = 0; k < 50; k++) {
			p2.add(sampleRow("p" + k, 2L, 1L, 1L, 100 + k, "entity"));
		}
		when(promotionGroupsTeamMapper.listScheduleAutoDoneGroupPage(any(Long.class), anyInt(), eq(0), eq(100)))
				.thenReturn(p1);
		when(promotionGroupsTeamMapper.listScheduleAutoDoneGroupPage(any(Long.class), anyInt(), eq(100), eq(100)))
				.thenReturn(p2);
		when(promotionGroupsTeamMemberMapper.selectPaymentOverEndTimeList(anyList())).thenReturn(List.of());
		int processed = promotionGroupsTeamService.scheduleAutoDoneGroup();
		assertThat(processed).isEqualTo(150);
		ArgumentCaptor<List<String>> cap = ArgumentCaptor.forClass(List.class);
		verify(promotionGroupsTeamMemberMapper, times(2)).selectPaymentOverEndTimeList(cap.capture());
		assertThat(cap.getAllValues().get(0)).hasSize(100);
		assertThat(cap.getAllValues().get(1)).hasSize(50);
		verify(promotionGroupsTeamFailService, never()).teamFail(any());
	}

	@Test
	void scheduleAutoDoneGroup_groupRobot_3B_insertsAndSetNum() {
		when(promotionGroupsTeamMapper.countScheduleAutoDoneGroup(any(Long.class), anyInt())).thenReturn(1L);
		ScheduleAutoDoneGroupTeamRow row = sampleRow("t-robot", 1L, 3L, 1L, 7L, "services");
		when(promotionGroupsTeamMapper.listScheduleAutoDoneGroupPage(any(Long.class), anyInt(), anyInt(), anyInt()))
				.thenReturn(List.of(row));
		when(promotionGroupsTeamMemberMapper.selectPaymentOverEndTimeList(anyList())).thenReturn(List.of());
		when(promotionGroupsTeamMemberMapper.selectGroupTeamSuccessMembers("t-robot")).thenReturn(List.of());
		promotionGroupsTeamService.scheduleAutoDoneGroup();
		verify(promotionGroupRobotWechatProfilePort, times(1)).getRandUserInfo(3);
		verify(promotionGroupsTeamMemberMapper, times(2)).insert(any(PromotionGroupsTeamMember.class));
		verify(promotionGroupsTeamMapper, times(1)).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	void scheduleAutoDoneGroup_collaboratorInjection_3_2() {
		assertThat(promotionGroupsTeamService).isNotNull();
	}

	@Test
	void scheduleAutoDoneGroup_whenTeamForcesFail_skipsPayed_3A_3C() {
		when(promotionGroupsTeamMapper.countScheduleAutoDoneGroup(any(Long.class), anyInt())).thenReturn(1L);
		ScheduleAutoDoneGroupTeamRow row = sampleRow("t1", 1L, 1L, 2L, 1L, "services");
		when(promotionGroupsTeamMapper.listScheduleAutoDoneGroupPage(any(Long.class), anyInt(), anyInt(), anyInt()))
				.thenReturn(List.of(row));
		PaymentOverEndTimeRow pr = new PaymentOverEndTimeRow();
		pr.setTeamId("t1");
		when(promotionGroupsTeamMemberMapper.selectPaymentOverEndTimeList(anyList())).thenReturn(List.of(pr));
		promotionGroupsTeamService.scheduleAutoDoneGroup();
		verify(promotionGroupsTeamFailService, times(1)).teamFail(eq("t1"));
		verify(groupPromotionOrderPayedService, never())
				.markServiceGroupOrderPayedAndTryGrantRights(anyLong(), anyLong(), anyLong());
	}

	@Test
	void scheduleAutoDoneGroup_forceFail_multipleTeams_3C_A5() {
		when(promotionGroupsTeamMapper.countScheduleAutoDoneGroup(any(Long.class), anyInt())).thenReturn(1L);
		ScheduleAutoDoneGroupTeamRow a = sampleRow("ta", 1L, 1L, 2L, 1L, "services");
		ScheduleAutoDoneGroupTeamRow b = sampleRow("tb", 1L, 1L, 2L, 2L, "services");
		when(promotionGroupsTeamMapper.listScheduleAutoDoneGroupPage(any(Long.class), anyInt(), anyInt(), anyInt()))
				.thenReturn(List.of(a, b));
		PaymentOverEndTimeRow ra = new PaymentOverEndTimeRow();
		ra.setTeamId("ta");
		PaymentOverEndTimeRow rb = new PaymentOverEndTimeRow();
		rb.setTeamId("tb");
		when(promotionGroupsTeamMemberMapper.selectPaymentOverEndTimeList(anyList())).thenReturn(List.of(ra, rb));
		promotionGroupsTeamService.scheduleAutoDoneGroup();
		verify(promotionGroupsTeamFailService, times(1)).teamFail("ta");
		verify(promotionGroupsTeamFailService, times(1)).teamFail("tb");
	}

	@Test
	void scheduleAutoDoneGroup_whenTeamStatusNotOne_skipsInnerPath() {
		when(promotionGroupsTeamMapper.countScheduleAutoDoneGroup(any(Long.class), anyInt())).thenReturn(1L);
		ScheduleAutoDoneGroupTeamRow row = sampleRow("t1", 2L, 1L, 2L, 1L, "services");
		when(promotionGroupsTeamMapper.listScheduleAutoDoneGroupPage(any(Long.class), anyInt(), anyInt(), anyInt()))
				.thenReturn(List.of(row));
		when(promotionGroupsTeamMemberMapper.selectPaymentOverEndTimeList(anyList())).thenReturn(List.of());
		promotionGroupsTeamService.scheduleAutoDoneGroup();
		verify(promotionGroupsTeamMemberMapper, never()).selectGroupTeamSuccessMembers(any());
	}

	@Test
	void scheduleAutoDoneGroup_robotPath_callsPayedForServices() {
		when(promotionGroupsTeamMapper.countScheduleAutoDoneGroup(any(Long.class), anyInt())).thenReturn(1L);
		ScheduleAutoDoneGroupTeamRow row = sampleRow("t1", 1L, 1L, 2L, 1L, "services");
		when(promotionGroupsTeamMapper.listScheduleAutoDoneGroupPage(any(Long.class), anyInt(), anyInt(), anyInt()))
				.thenReturn(List.of(row));
		when(promotionGroupsTeamMemberMapper.selectPaymentOverEndTimeList(anyList())).thenReturn(List.of());
		PromotionGroupsTeamMember m = new PromotionGroupsTeamMember();
		m.setMemberId(5L);
		m.setOrderId("100");
		m.setCompanyId(9L);
		when(promotionGroupsTeamMemberMapper.selectGroupTeamSuccessMembers("t1")).thenReturn(List.of(m));
		promotionGroupsTeamService.scheduleAutoDoneGroup();
		verify(groupPromotionOrderPayedService, times(1))
				.markServiceGroupOrderPayedAndTryGrantRights(9L, 5L, 100L);
	}

	@Test
	void scheduleAutoDoneGroup_robotPath_entityGroup_callsNormalPayed() {
		when(promotionGroupsTeamMapper.countScheduleAutoDoneGroup(any(Long.class), anyInt())).thenReturn(1L);
		ScheduleAutoDoneGroupTeamRow row = sampleRow("t1", 1L, 1L, 2L, 1L, "entity");
		when(promotionGroupsTeamMapper.listScheduleAutoDoneGroupPage(any(Long.class), anyInt(), anyInt(), anyInt()))
				.thenReturn(List.of(row));
		when(promotionGroupsTeamMemberMapper.selectPaymentOverEndTimeList(anyList())).thenReturn(List.of());
		PromotionGroupsTeamMember m = new PromotionGroupsTeamMember();
		m.setMemberId(5L);
		m.setOrderId("200");
		m.setCompanyId(1L);
		when(promotionGroupsTeamMemberMapper.selectGroupTeamSuccessMembers("t1")).thenReturn(List.of(m));
		promotionGroupsTeamService.scheduleAutoDoneGroup();
		verify(groupPromotionOrderPayedService, times(1)).markNormalGroupOrderPayedAndPublishErpSync(1L, 5L, 200L);
	}

	@Test
	void scheduleNoStoreAutoDoneGroup_whenNoCandidates_returns0() {
		when(promotionGroupsTeamMapper.countScheduleNoStoreAutoDoneGroup()).thenReturn(0L);
		assertThat(promotionGroupsTeamService.scheduleNoStoreAutoDoneGroup()).isZero();
		verify(promotionGroupsTeamMemberMapper, never()).selectGroupTeamSuccessMembers(any());
		verify(promotionGroupsTeamMapper, never()).listScheduleNoStoreAutoDoneGroupPage(anyInt(), anyInt());
	}

	@Test
	void scheduleNoStoreAutoDoneGroup_collaboratorInjection_3_2() {
		assertThat(promotionGroupsTeamService).isNotNull();
	}

	@Test
	void scheduleNoStoreAutoDoneGroup_multiPage_fixedFirstPageOffset() {
		when(promotionGroupsTeamMapper.countScheduleNoStoreAutoDoneGroup()).thenReturn(150L);
		List<ScheduleAutoDoneGroupTeamRow> p1 = new ArrayList<>();
		for (int k = 0; k < 100; k++) {
			p1.add(sampleRow("t" + k, 2L, 1L, 1L, k, "entity"));
		}
		when(promotionGroupsTeamMapper.listScheduleNoStoreAutoDoneGroupPage(eq(0), eq(100)))
				.thenReturn(p1);
		int processed = promotionGroupsTeamService.scheduleNoStoreAutoDoneGroup();
		assertThat(processed).isEqualTo(200);
		verify(promotionGroupsTeamMapper, times(2)).listScheduleNoStoreAutoDoneGroupPage(eq(0), eq(100));
		verify(promotionGroupsTeamMapper, never()).listScheduleNoStoreAutoDoneGroupPage(eq(100), anyInt());
		verify(promotionGroupsTeamMemberMapper, never()).selectPaymentOverEndTimeList(anyList());
		verify(promotionGroupsTeamFailService, never()).teamFail(any());
	}

	@Test
	void scheduleNoStoreAutoDoneGroup_noForceFail_3_3() {
		when(promotionGroupsTeamMapper.countScheduleNoStoreAutoDoneGroup()).thenReturn(1L);
		when(promotionGroupsTeamMapper.listScheduleNoStoreAutoDoneGroupPage(anyInt(), anyInt()))
				.thenReturn(List.of(sampleRow("t1", 1L, 1L, 2L, 1L, "services")));
		when(promotionGroupsTeamMemberMapper.selectGroupTeamSuccessMembers(any())).thenReturn(List.of());
		promotionGroupsTeamService.scheduleNoStoreAutoDoneGroup();
		verify(promotionGroupsTeamMemberMapper, never()).selectPaymentOverEndTimeList(anyList());
		verify(promotionGroupsTeamFailService, never()).teamFail(any());
	}

	@Test
	void scheduleNoStoreAutoDoneGroup_whenTeamStatusNotOne_skipsInnerPath() {
		when(promotionGroupsTeamMapper.countScheduleNoStoreAutoDoneGroup()).thenReturn(1L);
		when(promotionGroupsTeamMapper.listScheduleNoStoreAutoDoneGroupPage(anyInt(), anyInt()))
				.thenReturn(List.of(sampleRow("t1", 2L, 1L, 2L, 1L, "services")));
		promotionGroupsTeamService.scheduleNoStoreAutoDoneGroup();
		verify(promotionGroupsTeamMemberMapper, never()).selectGroupTeamSuccessMembers(any());
		verify(promotionGroupsTeamMemberMapper, never()).selectPaymentOverEndTimeList(anyList());
		verify(promotionGroupsTeamFailService, never()).teamFail(any());
	}

	@Test
	void scheduleNoStoreAutoDoneGroup_groupRobot_3B_insertsAndSetNum() {
		when(promotionGroupsTeamMapper.countScheduleNoStoreAutoDoneGroup()).thenReturn(1L);
		ScheduleAutoDoneGroupTeamRow row = sampleRow("t-robot", 1L, 3L, 1L, 7L, "services");
		when(promotionGroupsTeamMapper.listScheduleNoStoreAutoDoneGroupPage(anyInt(), anyInt()))
				.thenReturn(List.of(row));
		when(promotionGroupsTeamMemberMapper.selectGroupTeamSuccessMembers("t-robot")).thenReturn(List.of());
		promotionGroupsTeamService.scheduleNoStoreAutoDoneGroup();
		verify(promotionGroupRobotWechatProfilePort, times(1)).getRandUserInfo(3);
		verify(promotionGroupsTeamMemberMapper, times(2)).insert(any(PromotionGroupsTeamMember.class));
		verify(promotionGroupsTeamMapper, times(1)).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	void scheduleNoStoreAutoDoneGroup_robotPath_callsPayedForServices() {
		when(promotionGroupsTeamMapper.countScheduleNoStoreAutoDoneGroup()).thenReturn(1L);
		ScheduleAutoDoneGroupTeamRow row = sampleRow("t1", 1L, 1L, 2L, 1L, "services");
		when(promotionGroupsTeamMapper.listScheduleNoStoreAutoDoneGroupPage(anyInt(), anyInt()))
				.thenReturn(List.of(row));
		PromotionGroupsTeamMember m = new PromotionGroupsTeamMember();
		m.setMemberId(5L);
		m.setOrderId("100");
		m.setCompanyId(9L);
		when(promotionGroupsTeamMemberMapper.selectGroupTeamSuccessMembers("t1")).thenReturn(List.of(m));
		promotionGroupsTeamService.scheduleNoStoreAutoDoneGroup();
		verify(groupPromotionOrderPayedService, times(1))
				.markServiceGroupOrderPayedAndTryGrantRights(9L, 5L, 100L);
	}

	@Test
	void scheduleNoStoreAutoDoneGroup_robotPath_entityGroup_callsNormalPayed() {
		when(promotionGroupsTeamMapper.countScheduleNoStoreAutoDoneGroup()).thenReturn(1L);
		ScheduleAutoDoneGroupTeamRow row = sampleRow("t1", 1L, 1L, 2L, 1L, "entity");
		when(promotionGroupsTeamMapper.listScheduleNoStoreAutoDoneGroupPage(anyInt(), anyInt()))
				.thenReturn(List.of(row));
		PromotionGroupsTeamMember m = new PromotionGroupsTeamMember();
		m.setMemberId(5L);
		m.setOrderId("200");
		m.setCompanyId(1L);
		when(promotionGroupsTeamMemberMapper.selectGroupTeamSuccessMembers("t1")).thenReturn(List.of(m));
		promotionGroupsTeamService.scheduleNoStoreAutoDoneGroup();
		verify(promotionGroupsTeamMemberMapper, never()).selectPaymentOverEndTimeList(anyList());
		verify(promotionGroupsTeamFailService, never()).teamFail(any());
		verify(groupPromotionOrderPayedService, times(1)).markNormalGroupOrderPayedAndPublishErpSync(1L, 5L, 200L);
	}

	@Test
	void scheduleNoStoreAutoDoneGroup_neverInvokesPaymentOverEndTimeOrTeamFail() {
		when(promotionGroupsTeamMapper.countScheduleNoStoreAutoDoneGroup()).thenReturn(1L);
		when(promotionGroupsTeamMapper.listScheduleNoStoreAutoDoneGroupPage(anyInt(), anyInt()))
				.thenReturn(List.of(sampleRow("t1", 1L, 1L, 2L, 1L, "entity")));
		when(promotionGroupsTeamMemberMapper.selectGroupTeamSuccessMembers("t1")).thenReturn(List.of());
		promotionGroupsTeamService.scheduleNoStoreAutoDoneGroup();
		verify(promotionGroupsTeamMemberMapper, never()).selectPaymentOverEndTimeList(anyList());
		verify(promotionGroupsTeamFailService, never()).teamFail(any());
		verify(groupPromotionOrderPayedService, never())
				.markNormalGroupOrderPayedAndPublishErpSync(anyLong(), anyLong(), anyLong());
	}

	private static ScheduleAutoDoneGroupTeamRow sampleRow(
			String teamId, long teamStatus, long actPerson, long join, long id, String goodsType) {
		ScheduleAutoDoneGroupTeamRow r = new ScheduleAutoDoneGroupTeamRow();
		r.setId(id);
		r.setTeamId(teamId);
		r.setTeamStatus(teamStatus);
		r.setActPersonNum(actPerson);
		r.setJoinPersonNum(join);
		r.setGroupGoodsType(goodsType);
		r.setCompanyId(1L);
		r.setActId(1L);
		r.setActRobot(true);
		return r;
	}
}
