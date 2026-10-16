package cn.shopex.ecshopx.promotions.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.promotions.service.membersignin.MemberSignInCalendarService;
import cn.shopex.ecshopx.promotions.service.membersignin.MemberSignInQueryService;
import cn.shopex.ecshopx.promotions.service.membersignin.MemberSignInReminderService;
import cn.shopex.ecshopx.promotions.service.membersignin.MemberSignInSignService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("promotionsFrontV1MemberSignIn")
@RequestMapping("/api/v1/h5app/wxapp/promotion/member-signin")
public class MemberSignInController {

	private final MemberSignInCalendarService calendarService;
	private final MemberSignInSignService signService;
	private final MemberSignInQueryService queryService;
	private final MemberSignInReminderService reminderService;

	public MemberSignInController(
			MemberSignInCalendarService calendarService,
			MemberSignInSignService signService,
		 MemberSignInQueryService queryService,
			MemberSignInReminderService reminderService) {
		this.calendarService = calendarService;
		this.signService = signService;
		this.queryService = queryService;
		this.reminderService = reminderService;
	}

	@GetMapping(value = "/calendar", name = "会员签到自然周日历", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getCalendar(
			HttpServletRequest request,
			@RequestParam(name = "week_start", required = false) String weekStart) {
		long companyId = MemberSignInFrontSupport.readCompanyId(request);
		long userId = MemberSignInFrontSupport.readUserId(request);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		return ResponseEntity.ok(
				ApiResult.ok(calendarService.getCalendar(companyId, userId, weekStart, locale)));
	}

	@PostMapping(value = "/sign", name = "会员签到提交", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> sign(HttpServletRequest request) {
		long companyId = MemberSignInFrontSupport.readCompanyId(request);
		long userId = MemberSignInFrontSupport.readUserId(request);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		return ResponseEntity.ok(ApiResult.ok(signService.sign(companyId, userId, locale)));
	}

	@GetMapping(value = "/activity/progress", name = "会员签到活动进度", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityProgress(HttpServletRequest request) {
		long companyId = MemberSignInFrontSupport.readCompanyId(request);
		long userId = MemberSignInFrontSupport.readUserId(request);
		return ResponseEntity.ok(ApiResult.ok(queryService.getActivityProgress(companyId, userId)));
	}

	@GetMapping(value = "/records", name = "会员签到月记录", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getRecords(
			HttpServletRequest request, @RequestParam(name = "month") String month) {
		long companyId = MemberSignInFrontSupport.readCompanyId(request);
		long userId = MemberSignInFrontSupport.readUserId(request);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		return ResponseEntity.ok(
				ApiResult.ok(queryService.getMonthlyRecords(companyId, userId, month, locale)));
	}

	@GetMapping(value = "/rewards", name = "会员签到奖励记录", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getRewards(
			HttpServletRequest request,
			@RequestParam(name = "activity_id", required = false) Long activityId,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "page_size", required = false) String pageSize) {
		long companyId = MemberSignInFrontSupport.readCompanyId(request);
		long userId = MemberSignInFrontSupport.readUserId(request);
		return ResponseEntity.ok(
				ApiResult.ok(queryService.getRewards(companyId, userId, activityId, page, pageSize)));
	}

	@GetMapping(value = "/reminder", name = "会员签到提醒状态", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getReminder(HttpServletRequest request) {
		long companyId = MemberSignInFrontSupport.readCompanyId(request);
		long userId = MemberSignInFrontSupport.readUserId(request);
		return ResponseEntity.ok(ApiResult.ok(reminderService.getReminder(companyId, userId)));
	}

	@PutMapping(value = "/reminder", name = "会员签到提醒开关", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> saveReminder(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		long companyId = MemberSignInFrontSupport.readCompanyId(request);
		long userId = MemberSignInFrontSupport.readUserId(request);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		return ResponseEntity.ok(
				ApiResult.ok(reminderService.saveReminder(companyId, userId, body, locale)));
	}

	@PostMapping(
			value = "/reminder/subscribe-callback",
			name = "会员签到订阅回写",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> subscribeCallback(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		long companyId = MemberSignInFrontSupport.readCompanyId(request);
		long userId = MemberSignInFrontSupport.readUserId(request);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		return ResponseEntity.ok(
				ApiResult.ok(reminderService.subscribeCallback(companyId, userId, body, locale)));
	}
}
