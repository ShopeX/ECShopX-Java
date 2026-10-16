package cn.shopex.ecshopx.promotions.service.membersignin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.promotions.port.MemberSigninReminderDispatchPublisher;
import cn.shopex.ecshopx.common.wechat.WxaMemberOpenIdLookupPort;
import cn.shopex.ecshopx.promotions.domain.WxaNoticeTemplate;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninActivity;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninRecord;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninReminder;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninZoneResolver;
import cn.shopex.ecshopx.promotions.mapper.WxaNoticeTemplateMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninRecordMapper;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninReminderMapper;
import cn.shopex.ecshopx.promotions.service.WxaTemplateMsgActivityRemindSendService;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberSignInReminderPushServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
	private static final long COMPANY_ID = 38L;
	private static final long USER_ID = 261L;

	@Mock private MemberSigninReminderMapper reminderMapper;
	@Mock private MemberSigninRecordMapper recordMapper;
	@Mock private WxaNoticeTemplateMapper wxaNoticeTemplateMapper;
	@Mock private WeappMapper weappMapper;
	@Mock private WxaMemberOpenIdLookupPort wxaMemberOpenIdLookupPort;
	@Mock private MemberSignInActivitySupport activitySupport;
	@Mock private MemberSigninZoneResolver zoneResolver;
	@Mock private WxaTemplateMsgActivityRemindSendService sendService;
	@Mock private MemberSigninReminderDispatchPublisher publisher;

	private MemberSignInReminderPushService service;

	@BeforeEach
	void setUp() {
		when(zoneResolver.resolveZone(anyLong())).thenReturn(ZONE);
		service =
				new MemberSignInReminderPushService(
						reminderMapper,
						recordMapper,
						wxaNoticeTemplateMapper,
						weappMapper,
						wxaMemberOpenIdLookupPort,
						activitySupport,
						zoneResolver,
						sendService,
						publisher);
		ReflectionTestUtils.setField(service, "pushTime", "12:00");
	}

	@Test
	void computeNextPushEpoch_beforePushTime_returnsTodayPushTime() {
		long now = LocalDateTime.of(2026, 10, 10, 10, 0).atZone(ZONE).toEpochSecond();

		long target = service.computeNextPushEpoch(now);

		assertThat(target)
				.isEqualTo(LocalDateTime.of(2026, 10, 10, 12, 0).atZone(ZONE).toEpochSecond());
	}

	@Test
	void computeNextPushEpoch_afterPushTime_returnsTomorrowPushTime() {
		long now = LocalDateTime.of(2026, 10, 10, 13, 0).atZone(ZONE).toEpochSecond();

		long target = service.computeNextPushEpoch(now);

		assertThat(target)
				.isEqualTo(LocalDateTime.of(2026, 10, 11, 12, 0).atZone(ZONE).toEpochSecond());
	}

	@Test
	void scheduleNextPush_armsAndPublishesWhenNotScheduled() {
		MemberSigninReminder row = armedRow(null);
		when(reminderMapper.selectOne(any(Wrapper.class))).thenReturn(row);

		Long scheduled = service.scheduleNextPush(COMPANY_ID, USER_ID);

		assertThat(scheduled).isNotNull();
		ArgumentCaptor<Long> targetCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Duration> delayCaptor = ArgumentCaptor.forClass(Duration.class);
		verify(publisher).publish(eq(COMPANY_ID), eq(USER_ID), targetCaptor.capture(), delayCaptor.capture());
		long target = targetCaptor.getValue();
		LocalDateTime targetLdt = Instant.ofEpochSecond(target).atZone(ZONE).toLocalDateTime();
		assertThat(targetLdt.getHour()).isEqualTo(12);
		assertThat(targetLdt.getMinute()).isZero();
		assertThat(target).isGreaterThan(Instant.now().getEpochSecond());
		assertThat(delayCaptor.getValue().getSeconds()).isPositive();
		assertThat(row.getNextPushTime()).isEqualTo((int) target);
		verify(reminderMapper).updateById(row);
	}

	@Test
	void scheduleNextPush_skipsWhenAlreadyArmed() {
		long future = Instant.now().getEpochSecond() + 3600;
		MemberSigninReminder row = armedRow((int) future);
		when(reminderMapper.selectOne(any(Wrapper.class))).thenReturn(row);

		Long scheduled = service.scheduleNextPush(COMPANY_ID, USER_ID);

		assertThat(scheduled).isNull();
		verifyNoInteractions(publisher);
		verify(reminderMapper, never()).updateById(any(MemberSigninReminder.class));
	}

	@Test
	void scheduleNextPush_skipsWhenDisabled() {
		MemberSigninReminder row = armedRow(null);
		row.setEnabled(false);
		when(reminderMapper.selectOne(any(Wrapper.class))).thenReturn(row);

		assertThat(service.scheduleNextPush(COMPANY_ID, USER_ID)).isNull();
		verifyNoInteractions(publisher);
	}

	@Test
	void handleScheduledPush_dropsStaleTarget() {
		MemberSigninReminder row = armedRow((int) (Instant.now().getEpochSecond() + 9999));

		service.handleScheduledPush(COMPANY_ID, USER_ID, Instant.now().getEpochSecond());

		verifyNoInteractions(publisher);
		verify(reminderMapper, never()).updateById(any(MemberSigninReminder.class));
	}

	@Test
	void handleScheduledPush_clearsWhenDisabled() {
		long target = Instant.now().getEpochSecond() - 10;
		MemberSigninReminder row = armedRow((int) target);
		row.setEnabled(false);
		when(reminderMapper.selectOne(any(Wrapper.class))).thenReturn(row);

		service.handleScheduledPush(COMPANY_ID, USER_ID, target);

		ArgumentCaptor<MemberSigninReminder> captor = ArgumentCaptor.forClass(MemberSigninReminder.class);
		verify(reminderMapper).updateById(captor.capture());
		assertThat(captor.getValue().getNextPushTime()).isNull();
		verifyNoInteractions(publisher);
	}

	@Test
	void handleScheduledPush_reArmsWhenSignedToday() {
		long target = Instant.now().getEpochSecond() - 10;
		MemberSigninReminder row = armedRow((int) target);
		when(reminderMapper.selectOne(any(Wrapper.class))).thenReturn(row);
		when(recordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

		service.handleScheduledPush(COMPANY_ID, USER_ID, target);

		verify(publisher).publish(eq(COMPANY_ID), eq(USER_ID), anyLong(), any(Duration.class));
		assertThat(row.getNextPushTime()).isNotNull();
		assertThat(row.getNextPushTime()).isNotEqualTo((int) target);
		verify(reminderMapper).updateById(row);
		verifyNoInteractions(sendService);
	}

	@Test
	void handleScheduledPush_consumesQuotaOnSendSuccess() {
		long target = Instant.now().getEpochSecond() - 10;
		MemberSigninReminder row = armedRow((int) target);
		when(reminderMapper.selectOne(any(Wrapper.class))).thenReturn(row);
		when(recordMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
		when(wxaNoticeTemplateMapper.selectOne(any(Wrapper.class))).thenReturn(openNotice());
		when(weappMapper.selectOne(any(Wrapper.class))).thenReturn(weapp());
		when(wxaMemberOpenIdLookupPort.resolveOpenId(USER_ID, "wx-authorizer-appid")).thenReturn("openid-1");
		when(activitySupport.findInProgressActivity(COMPANY_ID)).thenReturn(null);
		when(sendService.send(any(), eq(true))).thenReturn(true);

		service.handleScheduledPush(COMPANY_ID, USER_ID, target);

		ArgumentCaptor<MemberSigninReminder> captor = ArgumentCaptor.forClass(MemberSigninReminder.class);
		verify(reminderMapper).updateById(captor.capture());
		MemberSigninReminder saved = captor.getValue();
		assertThat(saved.getLastPushDate()).isEqualTo(LocalDate.now(ZONE));
		assertThat(saved.getSubscribeAccepted()).isFalse();
		assertThat(saved.getNextPushTime()).isNull();
		verifyNoInteractions(publisher);
	}

	@Test
	void handleScheduledPush_reArmsWhenSendConfigMissing() {
		long target = Instant.now().getEpochSecond() - 10;
		MemberSigninReminder row = armedRow((int) target);
		when(reminderMapper.selectOne(any(Wrapper.class))).thenReturn(row);
		when(recordMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
		when(wxaNoticeTemplateMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		service.handleScheduledPush(COMPANY_ID, USER_ID, target);

		verify(publisher).publish(eq(COMPANY_ID), eq(USER_ID), anyLong(), any(Duration.class));
		assertThat(row.getNextPushTime()).isNotNull();
		verify(reminderMapper).updateById(row);
	}

	private MemberSigninReminder armedRow(Integer nextPushTime) {
		MemberSigninReminder row = new MemberSigninReminder();
		row.setId(3L);
		row.setCompanyId(COMPANY_ID);
		row.setUserId(USER_ID);
		row.setEnabled(true);
		row.setSubscribeAccepted(true);
		row.setNextPushTime(nextPushTime);
		return row;
	}

	private WxaNoticeTemplate openNotice() {
		WxaNoticeTemplate notice = new WxaNoticeTemplate();
		notice.setTemplateName("yykweishop");
		notice.setTemplateId("template-id-1");
		notice.setIsOpen(true);
		notice.setScenesName(MemberSignInReminderPushService.SCENES_NAME);
		return notice;
	}

	private Weapp weapp() {
		Weapp weapp = new Weapp();
		weapp.setAuthorizerAppid("wx-authorizer-appid");
		weapp.setTemplateName("yykweishop");
		return weapp;
	}
}
