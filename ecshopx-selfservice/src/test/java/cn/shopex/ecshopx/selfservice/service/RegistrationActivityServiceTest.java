package cn.shopex.ecshopx.selfservice.service;

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

import cn.shopex.ecshopx.common.dispatch.SendWxRemindJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.WxaNoticeTemplate;
import cn.shopex.ecshopx.promotions.mapper.WxaNoticeTemplateMapper;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationActivityServiceTest {

	@Mock
	private WxaNoticeTemplateMapper wxaNoticeTemplateMapper;

	@Mock
	private RegistrationActivityMapper registrationActivityMapper;

	@Mock
	private RegistrationActivityOutsideMultiLangReadService multiLang;

	@Mock
	private SendWxRemindJobDispatchPublisher sendWxRemindJobDispatchPublisher;

	private ObjectMapper objectMapper;
	private RegistrationActivityService service;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		service = new RegistrationActivityService(
				wxaNoticeTemplateMapper,
				registrationActivityMapper,
				multiLang,
				sendWxRemindJobDispatchPublisher,
				objectMapper);
	}

	@Test
	@DisplayName("§3 步骤3：无模板，dispatched=0，不入队、不查活动")
	void noTemplate_returnsZero() {
		when(wxaNoticeTemplateMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		assertThat(service.scheduleSendWxRemindMsg()).isZero();
		verify(registrationActivityMapper, never()).selectList(any(Wrapper.class));
		verify(sendWxRemindJobDispatchPublisher, never()).publish(anyLong());
	}

	@Test
	@DisplayName("§3 步骤4.1：templateId 或 sendTimeDesc 空则跳过，不查活动")
	void templateMissingId_skips() {
		WxaNoticeTemplate t = new WxaNoticeTemplate();
		t.setTemplateId(null);
		t.setSendTimeDesc("{\"value\":1}");
		when(wxaNoticeTemplateMapper.selectList(any(Wrapper.class))).thenReturn(List.of(t));
		assertThat(service.scheduleSendWxRemindMsg()).isZero();
		verify(registrationActivityMapper, never()).selectList(any(Wrapper.class));
		verify(sendWxRemindJobDispatchPublisher, never()).publish(anyLong());
	}

	@Test
	@DisplayName("§3 步骤4.2-4.3：JSON 非法，跳过")
	void badJson_skips() {
		WxaNoticeTemplate t = templateRow("1", "not-json{");
		when(wxaNoticeTemplateMapper.selectList(any(Wrapper.class))).thenReturn(List.of(t));
		assertThat(service.scheduleSendWxRemindMsg()).isZero();
		verify(registrationActivityMapper, never()).selectList(any(Wrapper.class));
	}

	@Test
	@DisplayName("§3 步骤4.2-4.3：空对象，跳过")
	void emptyObjectJson_skips() {
		WxaNoticeTemplate t = templateRow("1", "{}");
		when(wxaNoticeTemplateMapper.selectList(any(Wrapper.class))).thenReturn(List.of(t));
		assertThat(service.scheduleSendWxRemindMsg()).isZero();
		verify(registrationActivityMapper, never()).selectList(any(Wrapper.class));
	}

	@Test
	@DisplayName("§3 步骤4.4-4.5：value 为 0，跳过")
	void zeroRemind_skips() {
		WxaNoticeTemplate t = templateRow("1", "{\"value\":0}");
		when(wxaNoticeTemplateMapper.selectList(any(Wrapper.class))).thenReturn(List.of(t));
		assertThat(service.scheduleSendWxRemindMsg()).isZero();
		verify(registrationActivityMapper, never()).selectList(any(Wrapper.class));
	}

	@Test
	@DisplayName("§3 步骤4.9：时间窗内无活动")
	void noActivityInWindow_returnsZero() {
		WxaNoticeTemplate t = templateRow("1", "{\"value\":1}");
		t.setCompanyId(9L);
		when(wxaNoticeTemplateMapper.selectList(any(Wrapper.class))).thenReturn(List.of(t));
		when(registrationActivityMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
		assertThat(service.scheduleSendWxRemindMsg()).isZero();
		verify(sendWxRemindJobDispatchPublisher, never()).publish(anyLong());
		verify(multiLang, never()).applyActivityNameOverrides(anyLong(), anyList(), anyString());
	}

	@Test
	@DisplayName("§3 步骤4.10：主路径 1 模板 1 活动，入队 1 次、多语言 zh-CN、dispatched=1")
	void mainPath_enqueues() {
		long now = Instant.now().getEpochSecond();
		int remindStart = toIntSec(now + 2 * 3600L);
		int inWindow = remindStart + 10;
		WxaNoticeTemplate t = templateRow("tid", "{\"value\":1}");
		t.setCompanyId(200L);
		RegistrationActivity a = new RegistrationActivity();
		a.setActivityId(40L);
		a.setCompanyId(200L);
		a.setStartTime(inWindow);
		a.setIsWxappNotice(true);
		when(wxaNoticeTemplateMapper.selectList(any(Wrapper.class))).thenReturn(List.of(t));
		when(registrationActivityMapper.selectList(any(Wrapper.class))).thenReturn(List.of(a));
		assertThat(service.scheduleSendWxRemindMsg()).isEqualTo(1);
		verify(sendWxRemindJobDispatchPublisher, times(1)).publish(eq(40L));
		verify(multiLang, times(1)).applyActivityNameOverrides(eq(200L), anyList(), eq("zh-CN"));
	}

	@Test
	@DisplayName("§3 多活动：一模板 2 活动，dispatched=2")
	void twoActivities_twoEnqueues() {
		long now = Instant.now().getEpochSecond();
		int inWindow = toIntSec(now + 2 * 3600L) + 5;
		WxaNoticeTemplate t = templateRow("t", "{\"value\":1}");
		t.setCompanyId(1L);
		RegistrationActivity a1 = act(1L, 1L, inWindow);
		RegistrationActivity a2 = act(2L, 1L, inWindow);
		when(wxaNoticeTemplateMapper.selectList(any(Wrapper.class))).thenReturn(List.of(t));
		when(registrationActivityMapper.selectList(any(Wrapper.class))).thenReturn(List.of(a1, a2));
		assertThat(service.scheduleSendWxRemindMsg()).isEqualTo(2);
		ArgumentCaptor<Long> cap = ArgumentCaptor.forClass(Long.class);
		verify(sendWxRemindJobDispatchPublisher, times(2)).publish(cap.capture());
		assertThat(cap.getAllValues()).containsExactlyInAnyOrder(1L, 2L);
	}

	@Test
	@DisplayName("§3 多模板：2 个模板各命中 1 活动，累计 dispatched=2")
	void twoTemplates_accumulate() {
		long now = Instant.now().getEpochSecond();
		int inWindow = toIntSec(now + 2 * 3600L) + 3;
		WxaNoticeTemplate t1 = templateRow("a", "{\"value\":1}");
		t1.setCompanyId(10L);
		WxaNoticeTemplate t2 = templateRow("b", "{\"value\":1}");
		t2.setCompanyId(20L);
		RegistrationActivity r1 = act(11L, 10L, inWindow);
		RegistrationActivity r2 = act(22L, 20L, inWindow);
		when(wxaNoticeTemplateMapper.selectList(any(Wrapper.class))).thenReturn(List.of(t1, t2));
		when(registrationActivityMapper.selectList(any(Wrapper.class)))
				.thenReturn(List.of(r1))
				.thenReturn(List.of(r2));
		assertThat(service.scheduleSendWxRemindMsg()).isEqualTo(2);
		verify(sendWxRemindJobDispatchPublisher, times(2)).publish(anyLong());
	}

	private static WxaNoticeTemplate templateRow(String templateId, String sendTimeDesc) {
		WxaNoticeTemplate t = new WxaNoticeTemplate();
		t.setTemplateId(templateId);
		t.setSendTimeDesc(sendTimeDesc);
		return t;
	}

	private static RegistrationActivity act(long id, long company, int start) {
		RegistrationActivity a = new RegistrationActivity();
		a.setActivityId(id);
		a.setCompanyId(company);
		a.setStartTime(start);
		a.setIsWxappNotice(true);
		return a;
	}

	private static int toIntSec(long sec) {
		if (sec > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (sec < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) sec;
	}
}
