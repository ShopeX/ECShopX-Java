package cn.shopex.ecshopx.promotions.service.membersignin;

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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 会员签到漏签提醒推送（延迟队列模型）。
 *
 * <p>不再依赖 xxl-job 周期扫描：用户开启提醒 / 重新获得订阅授权时，投递一条一次性 Redis 延迟消息，
 * delay 直接算到下一次推送时刻（{@code member-signin.reminder.push-time}，默认 12:00）。
 * 到点后 {@link #handleScheduledPush} 校验并发送；当天已签到或发送配置缺失时自动重新武装下一次，
 * 保持提醒链不断；发送成功消耗一次性订阅额度（subscribe_accepted 置回 false），等待用户重新订阅事件再武装。
 */
@Service
public class MemberSignInReminderPushService {

	static final String SCENES_NAME = "member_signin_reminder";

	private static final String DEFAULT_ACTIVITY_NAME = "会员签到";
	private static final String DEFAULT_NOTICE_TEXT = "记得每天坚持打卡哦~";

	private static final Logger log = LoggerFactory.getLogger(MemberSignInReminderPushService.class);

	private final MemberSigninReminderMapper reminderMapper;
	private final MemberSigninRecordMapper recordMapper;
	private final WxaNoticeTemplateMapper wxaNoticeTemplateMapper;
	private final WeappMapper weappMapper;
	private final WxaMemberOpenIdLookupPort wxaMemberOpenIdLookupPort;
	private final MemberSignInActivitySupport activitySupport;
	private final MemberSigninZoneResolver zoneResolver;
	private final WxaTemplateMsgActivityRemindSendService wxaTemplateMsgActivityRemindSendService;
	private final MemberSigninReminderDispatchPublisher reminderDispatchPublisher;

	/** 每日推送时刻（HH:mm），用于计算下一次推送的延迟；与部署环境一致的业务日时区配合使用。 */
	@Value("${member-signin.reminder.push-time:12:00}")
	private String pushTime;

	public MemberSignInReminderPushService(
			MemberSigninReminderMapper reminderMapper,
			MemberSigninRecordMapper recordMapper,
			WxaNoticeTemplateMapper wxaNoticeTemplateMapper,
			WeappMapper weappMapper,
			WxaMemberOpenIdLookupPort wxaMemberOpenIdLookupPort,
			MemberSignInActivitySupport activitySupport,
			MemberSigninZoneResolver zoneResolver,
			WxaTemplateMsgActivityRemindSendService wxaTemplateMsgActivityRemindSendService,
			MemberSigninReminderDispatchPublisher reminderDispatchPublisher) {
		this.reminderMapper = reminderMapper;
		this.recordMapper = recordMapper;
		this.wxaNoticeTemplateMapper = wxaNoticeTemplateMapper;
		this.weappMapper = weappMapper;
		this.wxaMemberOpenIdLookupPort = wxaMemberOpenIdLookupPort;
		this.activitySupport = activitySupport;
		this.zoneResolver = zoneResolver;
		this.wxaTemplateMsgActivityRemindSendService = wxaTemplateMsgActivityRemindSendService;
		this.reminderDispatchPublisher = reminderDispatchPublisher;
	}

	/**
	 * 为用户武装下一次推送（供开启提醒 / 重新订阅等事件调用）。
	 *
	 * @return 本次实际排定的目标时刻（epoch 秒）；null = 无需排定（未开启/未订阅/已排定/配置或投递异常）
	 */
	public Long scheduleNextPush(long companyId, long userId) {
		MemberSigninReminder row = loadRow(companyId, userId);
		if (row == null) {
			return null;
		}
		return scheduleNextPush(row);
	}

	Long scheduleNextPush(MemberSigninReminder row) {
		if (!Boolean.TRUE.equals(row.getEnabled()) || !Boolean.TRUE.equals(row.getSubscribeAccepted())) {
			return null;
		}
		long now = Instant.now().getEpochSecond();
		if (row.getNextPushTime() != null && row.getNextPushTime() > now) {
			return null; // 已排定，避免重复投递
		}
		long target;
		try {
			target = computeNextPushEpoch(now);
		} catch (RuntimeException ex) {
			log.warn(
					"[member-signin-reminder] compute next push failed, check member-signin.reminder.push-time config",
					ex);
			return null;
		}
		row.setNextPushTime((int) target);
		row.setUpdated((int) now);
		reminderMapper.updateById(row);
		try {
			reminderDispatchPublisher.publish(
					row.getCompanyId(), row.getUserId(), target, Duration.ofSeconds(Math.max(1, target - now)));
		} catch (RuntimeException ex) {
			log.warn(
					"[member-signin-reminder] publish delayed job failed, rollback schedule marker companyId={} userId={}",
					row.getCompanyId(),
					row.getUserId(),
					ex);
			row.setNextPushTime(null);
			reminderMapper.updateById(row);
			return null;
		}
		return target;
	}

	/**
	 * 消费到点的延迟消息：幂等校验 → 发送/跳过 → 必要时重新武装下一次。
	 *
	 * <p>幂等规则：payload 携带排程时的 target，仅当 DB 中 next_push_time 与之相等才处理；
	 * 用户重新开关产生的更新排程会接管时间线，过期消息直接丢弃。
	 */
	public void handleScheduledPush(long companyId, long userId, long targetEpochSeconds) {
		MemberSigninReminder row = loadRow(companyId, userId);
		if (row == null || row.getNextPushTime() == null || row.getNextPushTime() != targetEpochSeconds) {
			return; // 记录不存在 / 排程已被清空 / 已被更新的排程接管
		}
		long now = Instant.now().getEpochSecond();
		LocalDate today = LocalDate.now(zoneResolver.resolveZone(companyId));

		if (!Boolean.TRUE.equals(row.getEnabled()) || !Boolean.TRUE.equals(row.getSubscribeAccepted())) {
			persistCleared(row, now); // 用户已关闭或额度已消耗：停止链路，等待重新开启/订阅事件
			return;
		}
		if (row.getLastPushDate() != null && row.getLastPushDate().equals(today)) {
			scheduleNextPush(row); // 重复排程兜底：今天已推过，排到下一次
			return;
		}
		if (hasSignedToday(companyId, userId, today)) {
			scheduleNextPush(row); // 当天已签到：跳过但保持下一次提醒
			return;
		}
		boolean sent = dispatchPush(row);
		if (sent) {
			row.setNextPushTime(null);
			row.setLastPushDate(today);
			row.setSubscribeAccepted(false); // 一次性订阅额度已消耗
			row.setUpdated((int) now);
			reminderMapper.updateById(row);
			return; // 等待用户重新订阅事件再武装
		}
		scheduleNextPush(row); // 模板未开/appid/openid 缺失等配置问题：明日再试
	}

	/**
	 * 惰性兜底：签到页打开时调用，修复 Redis 延迟消息丢失导致的断链
	 * （enabled 且有额度但 next_push_time 为空或已过期时重新武装）。
	 */
	public void ensureArmed(long companyId, long userId) {
		try {
			scheduleNextPush(companyId, userId);
		} catch (RuntimeException ex) {
			log.warn("[member-signin-reminder] lazy re-arm failed companyId={} userId={}", companyId, userId, ex);
		}
	}

	private void persistCleared(MemberSigninReminder row, long now) {
		row.setNextPushTime(null);
		row.setUpdated((int) now);
		reminderMapper.updateById(row);
	}

	private boolean hasSignedToday(long companyId, long userId, LocalDate today) {
		Long signed =
				recordMapper.selectCount(
						new LambdaQueryWrapper<MemberSigninRecord>()
								.eq(MemberSigninRecord::getCompanyId, companyId)
								.eq(MemberSigninRecord::getUserId, userId)
								.eq(MemberSigninRecord::getSignDate, today));
		return signed != null && signed > 0;
	}

	/** 计算下一次推送时刻：今天的 push-time，已过则明天。 */
	long computeNextPushEpoch(long nowEpochSeconds) {
		ZoneId zone = zoneResolver.resolveZone(0L);
		LocalTime pushAt = LocalTime.parse(pushTime.trim());
		LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochSecond(nowEpochSeconds), zone);
		LocalDateTime next = now.toLocalDate().atTime(pushAt);
		if (!next.isAfter(now)) {
			next = next.plusDays(1);
		}
		return next.atZone(zone).toEpochSecond();
	}

	private MemberSigninReminder loadRow(long companyId, long userId) {
		return reminderMapper.selectOne(
				new LambdaQueryWrapper<MemberSigninReminder>()
						.eq(MemberSigninReminder::getCompanyId, companyId)
						.eq(MemberSigninReminder::getUserId, userId)
						.last("LIMIT 1"));
	}

	private boolean dispatchPush(MemberSigninReminder reminder) {
		WxaNoticeTemplate notice = resolveOpenNotice(reminder);
		if (notice == null) {
			return false;
		}
		String templateName = notice.getTemplateName();
		if (!StringUtils.hasText(templateName)) {
			return false;
		}
		Weapp weapp =
				weappMapper.selectOne(
						new LambdaQueryWrapper<Weapp>()
								.eq(Weapp::getCompanyId, reminder.getCompanyId())
								.eq(Weapp::getTemplateName, templateName.trim())
								.last("LIMIT 1"));
		if (weapp == null || !StringUtils.hasText(weapp.getAuthorizerAppid())) {
			return false;
		}
		String appid = weapp.getAuthorizerAppid().trim();
		String openid = wxaMemberOpenIdLookupPort.resolveOpenId(reminder.getUserId(), appid);
		if (!StringUtils.hasText(openid)) {
			return false;
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", reminder.getCompanyId());
		payload.put("user_id", reminder.getUserId());
		payload.put("scenes_name", SCENES_NAME);
		payload.put("appid", appid);
		payload.put("openid", openid);
		Map<String, Object> data = new HashMap<>();
		data.put("activity_name", resolveActivityName(reminder.getCompanyId()));
		data.put("notice", DEFAULT_NOTICE_TEXT);
		payload.put("data", data);
		try {
			return wxaTemplateMsgActivityRemindSendService.send(payload, true);
		} catch (Exception ex) {
			log.warn(
					"[member-signin-reminder] push failed companyId={} userId={}",
					reminder.getCompanyId(),
					reminder.getUserId(),
					ex);
			return false;
		}
	}

	private WxaNoticeTemplate resolveOpenNotice(MemberSigninReminder reminder) {
		long companyId = reminder.getCompanyId();
		String subscribedId =
				reminder.getSubscribeTemplateId() == null ? "" : reminder.getSubscribeTemplateId().trim();
		if (StringUtils.hasText(subscribedId)) {
			WxaNoticeTemplate bySubscribed =
					wxaNoticeTemplateMapper.selectOne(
							new LambdaQueryWrapper<WxaNoticeTemplate>()
									.eq(WxaNoticeTemplate::getCompanyId, companyId)
									.eq(WxaNoticeTemplate::getScenesName, SCENES_NAME)
									.eq(WxaNoticeTemplate::getIsOpen, true)
									.eq(WxaNoticeTemplate::getTemplateId, subscribedId)
									.last("LIMIT 1"));
			if (bySubscribed != null) {
				return bySubscribed;
			}
		}
		WxaNoticeTemplate anyOpen =
				wxaNoticeTemplateMapper.selectOne(
						new LambdaQueryWrapper<WxaNoticeTemplate>()
								.eq(WxaNoticeTemplate::getCompanyId, companyId)
								.eq(WxaNoticeTemplate::getScenesName, SCENES_NAME)
								.eq(WxaNoticeTemplate::getIsOpen, true)
								.last("LIMIT 1"));
		if (anyOpen == null || !StringUtils.hasText(anyOpen.getTemplateId())) {
			return null;
		}
		return anyOpen;
	}

	private String resolveActivityName(long companyId) {
		MemberSigninActivity activity = activitySupport.findInProgressActivity(companyId);
		if (activity == null || !StringUtils.hasText(activity.getName())) {
			return DEFAULT_ACTIVITY_NAME;
		}
		return activity.getName().trim();
	}
}
