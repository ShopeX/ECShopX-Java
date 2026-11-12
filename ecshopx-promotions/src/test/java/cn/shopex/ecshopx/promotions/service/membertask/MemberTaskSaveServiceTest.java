package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ConflictException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class MemberTaskSaveServiceTest {

	@Mock
	private MemberTaskMapper memberTaskMapper;

	@Mock
	private MessageSource messageSource;

	private MemberTaskSaveService service;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		service = new MemberTaskSaveService(memberTaskMapper, messageSource, objectMapper);
	}

	@Test
	void create_rejectsEndNotAfterNow_en() {
		long now = 1_700_000_000L;
		when(messageSource.getMessage(
						eq("promotions.member_task.task_end_must_be_future"),
						eq(null),
						eq("结束时间必须晚于当前时间"),
						eq(Locale.US)))
				.thenReturn("End time must be later than now");

		Map<String, Object> body = validBody();
		body.put("begin_time", (int) (now - 100));
		body.put("end_time", (int) (now - 10));

		assertThatThrownBy(() -> service.create(1L, body, now, Locale.US))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("End time must be later than now");
	}

	@Test
	void create_insertsDraftAndReturnsVersion1() {
		long now = 1_700_000_000L;
		Map<String, Object> body = validBody();
		body.put("begin_time", (int) (now + 100));
		body.put("end_time", (int) (now + 1000));

		when(memberTaskMapper.insert(any(MemberTask.class)))
				.thenAnswer(
						inv -> {
							MemberTask t = inv.getArgument(0);
							t.setTaskId(42L);
							return 1;
						});

		Map<String, Object> out = service.create(9L, body, now, Locale.SIMPLIFIED_CHINESE);

		assertThat(out.get("task_id")).isEqualTo(42L);
		assertThat(out.get("config_version")).isEqualTo(1L);
		assertThat(out.get("display_status")).isEqualTo("draft");

		ArgumentCaptor<MemberTask> cap = ArgumentCaptor.forClass(MemberTask.class);
		verify(memberTaskMapper).insert(cap.capture());
		MemberTask saved = cap.getValue();
		assertThat(saved.getCompanyId()).isEqualTo(9L);
		assertThat(saved.getPublished()).isEqualTo(0);
		assertThat(saved.getOffShelf()).isEqualTo(0);
		assertThat(saved.getConfigVersion()).isEqualTo(1L);
		assertThat(saved.getTypeDetail()).isEqualTo("first_order");
		assertThat(saved.getRewardConfig()).contains("\"points\":100");
	}

	@Test
	void create_stripsRuleConfigDisplayListsBeforePersist() {
		long now = 1_700_000_000L;
		when(memberTaskMapper.insert(any(MemberTask.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTask.class).setTaskId(44L);
							return 1;
						});

		Map<String, Object> body = validBody();
		body.put("begin_time", (int) (now + 100));
		body.put("end_time", (int) (now + 1000));
		Map<String, Object> rule = new LinkedHashMap<>();
		rule.put("goods_scope", "unlimited");
		rule.put("amount_fen", 0);
		rule.put("need_receipt", false);
		rule.put("category_list", List.of(Map.of("category_id", 1L, "category_name", "展示")));
		rule.put("distributor_list", List.of(Map.of("distributor_id", 2L, "name", "展示")));
		body.put("rule_config", rule);

		service.create(9L, body, now, Locale.SIMPLIFIED_CHINESE);

		ArgumentCaptor<MemberTask> cap = ArgumentCaptor.forClass(MemberTask.class);
		verify(memberTaskMapper).insert(cap.capture());
		assertThat(cap.getValue().getRuleConfig()).doesNotContain("category_list");
		assertThat(cap.getValue().getRuleConfig()).doesNotContain("distributor_list");
	}

	@Test
	void create_purchaseItemsScope_requiresItemIds() {
		long now = 1_700_000_000L;
		when(messageSource.getMessage(
						eq("promotions.member_task.rule_config_invalid"),
						eq(null),
						eq("任务规则配置无效"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("任务规则配置无效");

		Map<String, Object> body = validBody();
		body.put("begin_time", (int) (now + 100));
		body.put("end_time", (int) (now + 1000));
		body.put(
				"rule_config",
				Map.of(
						"goods_scope",
						"items",
						"item_ids",
						List.of(),
						"amount_fen",
						0,
						"need_receipt",
						false));

		assertThatThrownBy(() -> service.create(1L, body, now, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("任务规则配置无效");
	}

	@Test
	void create_profile_rejectsNonEmptyRuleConfig() {
		long now = 1_700_000_000L;
		when(messageSource.getMessage(
						eq("promotions.member_task.rule_config_invalid"),
						eq(null),
						eq("任务规则配置无效"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("任务规则配置无效");

		Map<String, Object> body = validBody();
		body.put("type_category", "newcomer");
		body.put("type_detail", "profile");
		body.remove("rule_config");
		body.put("begin_time", (int) (now + 100));
		body.put("end_time", (int) (now + 1000));
		body.put("rule_config", Map.of("unexpected", 1));

		assertThatThrownBy(() -> service.create(1L, body, now, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("任务规则配置无效");
	}

	@Test
	void create_shareOrder_requiresShareCardFields() {
		long now = 1_700_000_000L;
		when(memberTaskMapper.insert(any(MemberTask.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTask.class).setTaskId(45L);
							return 1;
						});

		Map<String, Object> body = validBody();
		body.put("type_category", "share");
		body.put("type_detail", "share_order");
		body.put("name", "分享下单");
		body.put("begin_time", (int) (now + 100));
		body.put("end_time", (int) (now + 1000));
		body.put(
				"rule_config",
				Map.of(
						"goods_scope",
						"unlimited",
						"amount_fen",
						0,
						"need_receipt",
						true,
						"share_cover",
						"https://cdn/cover.png",
						"share_title",
						"标题",
						"share_jump_page",
						"/pages/index"));

		Map<String, Object> out = service.create(9L, body, now, Locale.SIMPLIFIED_CHINESE);

		assertThat(out.get("task_id")).isEqualTo(45L);
	}

	@Test
	void create_totalSpend_requiresPurchaseRuleConfig() {
		long now = 1_700_000_000L;
		when(memberTaskMapper.insert(any(MemberTask.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTask.class).setTaskId(44L);
							return 1;
						});

		Map<String, Object> body = validBody();
		body.put("type_category", "purchase");
		body.put("type_detail", "total_spend");
		body.put("name", "累计满500");
		body.put("begin_time", (int) (now + 100));
		body.put("end_time", (int) (now + 1000));
		body.put(
				"rule_config",
				Map.of("goods_scope", "unlimited", "amount_fen", 50000, "need_receipt", true));

		Map<String, Object> out = service.create(9L, body, now, Locale.SIMPLIFIED_CHINESE);

		assertThat(out.get("task_id")).isEqualTo(44L);
		ArgumentCaptor<MemberTask> cap = ArgumentCaptor.forClass(MemberTask.class);
		verify(memberTaskMapper).insert(cap.capture());
		assertThat(cap.getValue().getTypeDetail()).isEqualTo("total_spend");
	}

	@Test
	void create_singleSpend_requiresPurchaseRuleConfig() {
		long now = 1_700_000_000L;
		when(memberTaskMapper.insert(any(MemberTask.class)))
				.thenAnswer(
						inv -> {
							inv.getArgument(0, MemberTask.class).setTaskId(43L);
							return 1;
						});

		Map<String, Object> body = validBody();
		body.put("type_category", "purchase");
		body.put("type_detail", "single_spend");
		body.put("name", "单笔满100");
		body.put("begin_time", (int) (now + 100));
		body.put("end_time", (int) (now + 1000));
		body.put(
				"rule_config",
				Map.of("goods_scope", "unlimited", "amount_fen", 10000, "need_receipt", true));

		Map<String, Object> out = service.create(9L, body, now, Locale.SIMPLIFIED_CHINESE);

		assertThat(out.get("task_id")).isEqualTo(43L);
		ArgumentCaptor<MemberTask> cap = ArgumentCaptor.forClass(MemberTask.class);
		verify(memberTaskMapper).insert(cap.capture());
		assertThat(cap.getValue().getTypeDetail()).isEqualTo("single_spend");
		assertThat(cap.getValue().getRuleConfig()).contains("amount_fen");
	}

	@Test
	void update_rejectsNonDraft() {
		long now = 1_700_000_000L;
		MemberTask existing = draftEntity(now);
		existing.setPublished(1);
		when(memberTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
		when(messageSource.getMessage(
						eq("promotions.member_task.edit_not_draft"),
						eq(null),
						eq("仅待发布状态可编辑"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("仅待发布状态可编辑");

		Map<String, Object> body = validBody();
		body.put("config_version", 1);
		body.put("begin_time", (int) (now + 100));
		body.put("end_time", (int) (now + 1000));

		assertThatThrownBy(() -> service.update(1L, 10L, body, now, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("仅待发布状态可编辑");
	}

	@Test
	void update_conflictWhenVersionMismatch() {
		long now = 1_700_000_000L;
		MemberTask existing = draftEntity(now);
		existing.setConfigVersion(2L);
		when(memberTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
		when(messageSource.getMessage(
						eq("promotions.member_task.config_version_conflict"),
						eq(null),
						eq("任务配置已被他人修改，请刷新后重试"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("任务配置已被他人修改，请刷新后重试");

		Map<String, Object> body = validBody();
		body.put("config_version", 1);
		body.put("begin_time", (int) (now + 100));
		body.put("end_time", (int) (now + 1000));

		assertThatThrownBy(() -> service.update(1L, 10L, body, now, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(ConflictException.class)
				.hasMessage("任务配置已被他人修改，请刷新后重试");
	}

	@Test
	void update_rejectsTypeChange() {
		long now = 1_700_000_000L;
		MemberTask existing = draftEntity(now);
		when(memberTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
		when(messageSource.getMessage(
						eq("promotions.member_task.type_immutable"),
						eq(null),
						eq("任务类型不可修改"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("任务类型不可修改");

		Map<String, Object> body = validBody();
		body.put("config_version", 1);
		body.put("type_detail", "profile");
		body.put("begin_time", (int) (now + 100));
		body.put("end_time", (int) (now + 1000));

		assertThatThrownBy(() -> service.update(1L, 10L, body, now, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("任务类型不可修改");
	}

	@Test
	void update_notFound() {
		long now = 1_700_000_000L;
		when(memberTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
		when(messageSource.getMessage(
						eq("promotions.member_task.task_not_found"),
						eq(null),
						eq("任务不存在"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("任务不存在");

		Map<String, Object> body = validBody();
		body.put("config_version", 1);

		assertThatThrownBy(() -> service.update(1L, 99L, body, now, Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(ResourceException.class)
				.hasMessage("任务不存在");
	}

	private static MemberTask draftEntity(long now) {
		MemberTask t = new MemberTask();
		t.setTaskId(10L);
		t.setCompanyId(1L);
		t.setName("旧名");
		t.setTypeCategory("newcomer");
		t.setTypeDetail("first_order");
		t.setBeginTime((int) (now + 100));
		t.setEndTime((int) (now + 1000));
		t.setPublished(0);
		t.setOffShelf(0);
		t.setConfigVersion(1L);
		t.setFrequencyType("once");
		t.setAudienceMode("unlimited");
		t.setTaskRule("规则");
		t.setRewardConfig("{\"points\":10,\"coupon_id\":null}");
		return t;
	}

	private static Map<String, Object> validBody() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", "新人首单");
		body.put("type_category", "newcomer");
		body.put("type_detail", "first_order");
		body.put("frequency_type", "once");
		body.put("audience_mode", "unlimited");
		body.put("task_rule", "完成首单可得奖励");
		Map<String, Object> reward = new LinkedHashMap<>();
		reward.put("points", 100);
		reward.put("coupon_id", null);
		body.put("reward_config", reward);
		body.put(
				"rule_config",
				Map.of("goods_scope", "unlimited", "amount_fen", 0, "need_receipt", false));
		return body;
	}
}
