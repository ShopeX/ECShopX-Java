package cn.shopex.ecshopx.promotions.service.membersignin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.promotions.domain.membersignin.MemberSigninReminder;
import cn.shopex.ecshopx.promotions.mapper.membersignin.MemberSigninReminderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MemberSignInReminderService {

	private final MemberSigninReminderMapper reminderMapper;
	private final MessageSource messageSource;
	private final MemberSignInReminderPushService reminderPushService;

	public MemberSignInReminderService(
			MemberSigninReminderMapper reminderMapper,
			MessageSource messageSource,
			MemberSignInReminderPushService reminderPushService) {
		this.reminderMapper = reminderMapper;
		this.messageSource = messageSource;
		this.reminderPushService = reminderPushService;
	}

	public Map<String, Object> getReminder(long companyId, long userId) {
		MemberSigninReminder row = loadOrDefault(companyId, userId);
		return toResponse(row);
	}

	@Transactional
	public Map<String, Object> saveReminder(
			long companyId, long userId, Map<String, Object> input, Locale locale) {
		boolean enabled = parseBoolean(input.get("enabled"));
		MemberSigninReminder row = loadOrDefault(companyId, userId);
		if (enabled && !Boolean.TRUE.equals(row.getSubscribeAccepted())) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.member_signin.reminder_subscribe_required",
							null,
							"请先授权订阅消息后再开启提醒",
							locale));
		}
		row.setEnabled(enabled);
		if (!enabled) {
			row.setNextPushTime(null); // 关闭提醒：撤销排程，到点的旧消息会被消费端丢弃
		}
		row.setUpdated((int) Instant.now().getEpochSecond());
		if (row.getId() == null) {
			reminderMapper.insert(row);
		} else {
			reminderMapper.updateById(row);
		}
		if (enabled) {
			reminderPushService.scheduleNextPush(companyId, userId);
		}
		return toResponse(row);
	}

	@Transactional
	public Map<String, Object> subscribeCallback(
			long companyId, long userId, Map<String, Object> input, Locale locale) {
		String templateId = input.get("template_id") == null ? null : input.get("template_id").toString().trim();
		boolean accepted = resolveSubscribeAccepted(input.get("accepted"), templateId);
		if (accepted && !StringUtils.hasText(templateId)) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.member_signin.reminder_template_id_required",
							null,
							"缺少订阅消息模板ID",
							locale));
		}
		MemberSigninReminder row = loadOrDefault(companyId, userId);
		row.setSubscribeAccepted(accepted);
		if (accepted && StringUtils.hasText(templateId)) {
			row.setSubscribeTemplateId(templateId);
		}
		if (accepted) {
			// 与前端「仅订阅、不可取消」一致：授权成功即视为开启提醒，无需再 PUT /reminder
			row.setEnabled(true);
		} else {
			row.setEnabled(false);
			row.setNextPushTime(null); // 拒绝订阅额度：撤销排程
		}
		row.setUpdated((int) Instant.now().getEpochSecond());
		if (row.getId() == null) {
			reminderMapper.insert(row);
		} else {
			reminderMapper.updateById(row);
		}
		if (accepted) {
			// 推送消耗额度后 subscribeAccepted=false，重新订阅时若开关仍开则重新武装下一次推送
			reminderPushService.scheduleNextPush(companyId, userId);
		}
		return toResponse(row);
	}

	private MemberSigninReminder loadOrDefault(long companyId, long userId) {
		MemberSigninReminder row =
				reminderMapper.selectOne(
						new LambdaQueryWrapper<MemberSigninReminder>()
								.eq(MemberSigninReminder::getCompanyId, companyId)
								.eq(MemberSigninReminder::getUserId, userId)
								.last("LIMIT 1"));
		if (row != null) {
			return row;
		}
		MemberSigninReminder created = new MemberSigninReminder();
		created.setCompanyId(companyId);
		created.setUserId(userId);
		created.setEnabled(false);
		created.setSubscribeAccepted(false);
		return created;
	}

	private Map<String, Object> toResponse(MemberSigninReminder row) {
		boolean enabled = Boolean.TRUE.equals(row.getEnabled());
		boolean subscribeAccepted = Boolean.TRUE.equals(row.getSubscribeAccepted());
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("enabled", enabled);
		out.put("subscribe_accepted", subscribeAccepted);
		out.put("subscribed", enabled && subscribeAccepted); // 前端按钮：true=已订阅且不可点
		out.put("next_push_time", row.getNextPushTime()); // 已排定的下一次推送时刻(epoch秒)，便于排查
		return out;
	}

	/** 仅传 template_id 时视为用户已在小程序侧 accept（简化前端 POST body）。 */
	private static boolean resolveSubscribeAccepted(Object acceptedRaw, String templateId) {
		if (acceptedRaw == null) {
			return StringUtils.hasText(templateId);
		}
		return parseBoolean(acceptedRaw);
	}

	private static boolean parseBoolean(Object raw) {
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw == null) {
			return false;
		}
		String text = raw.toString().trim();
		return "1".equals(text) || "true".equalsIgnoreCase(text);
	}
}
