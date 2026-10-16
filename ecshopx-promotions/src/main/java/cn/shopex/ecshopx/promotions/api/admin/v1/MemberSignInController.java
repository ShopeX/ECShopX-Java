package cn.shopex.ecshopx.promotions.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.promotions.service.membersignin.MemberSignInActivityService;
import cn.shopex.ecshopx.promotions.service.membersignin.MemberSignInCouponCandidateService;
import cn.shopex.ecshopx.promotions.service.membersignin.MemberSignInDailyPointsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("promotionsAdminV1MemberSignIn")
@RequestMapping("/api/v1/promotions/member-signin")
public class MemberSignInController {

	private final MemberSignInDailyPointsService dailyPointsService;
	private final MemberSignInActivityService activityService;
	private final MemberSignInCouponCandidateService couponCandidateService;

	public MemberSignInController(
			MemberSignInDailyPointsService dailyPointsService,
			MemberSignInActivityService activityService,
			MemberSignInCouponCandidateService couponCandidateService) {
		this.dailyPointsService = dailyPointsService;
		this.activityService = activityService;
		this.couponCandidateService = couponCandidateService;
	}

	@Activated(routeAlias = "promotions.member_signin.daily_points.get")
	@GetMapping(value = "/daily-points", name = "读取会员签到日常积分配置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getDailyPoints(HttpServletRequest request) {
		long companyId =
				MemberSignInAdminSupport.readCompanyIdFromOperatorJwtMap(
						MemberSignInAdminSupport.readOperatorJwtMap(request));
		return ResponseEntity.ok(ApiResult.ok(dailyPointsService.getDailyPoints(companyId)));
	}

	@Activated(routeAlias = "promotions.member_signin.daily_points.save")
	@PutMapping(value = "/daily-points", name = "保存会员签到日常积分配置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> saveDailyPoints(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		long companyId =
				MemberSignInAdminSupport.readCompanyIdFromOperatorJwtMap(
						MemberSignInAdminSupport.readOperatorJwtMap(request));
		Map<String, Object> merged = MemberSignInAdminSupport.mergeInput(request, body);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		return ResponseEntity.ok(
				ApiResult.ok(dailyPointsService.saveDailyPoints(companyId, merged, locale)));
	}

	@Activated(routeAlias = "promotions.member_signin.activities.list")
	@GetMapping(value = "/activities", name = "会员签到活动列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> listActivities(
			HttpServletRequest request,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "page_size", required = false) String pageSize) {
		long companyId =
				MemberSignInAdminSupport.readCompanyIdFromOperatorJwtMap(
						MemberSignInAdminSupport.readOperatorJwtMap(request));
		return ResponseEntity.ok(
				ApiResult.ok(activityService.listActivities(companyId, name, page, pageSize)));
	}

	@Activated(routeAlias = "promotions.member_signin.activities.get")
	@GetMapping(value = "/activities/{id}", name = "会员签到活动详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivity(
			HttpServletRequest request, @PathVariable("id") long id) {
		long companyId =
				MemberSignInAdminSupport.readCompanyIdFromOperatorJwtMap(
						MemberSignInAdminSupport.readOperatorJwtMap(request));
		return ResponseEntity.ok(ApiResult.ok(activityService.getActivity(companyId, id)));
	}

	@Activated(routeAlias = "promotions.member_signin.activities.create")
	@PostMapping(value = "/activities", name = "新增会员签到活动", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createActivity(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		long companyId =
				MemberSignInAdminSupport.readCompanyIdFromOperatorJwtMap(
						MemberSignInAdminSupport.readOperatorJwtMap(request));
		Map<String, Object> merged = MemberSignInAdminSupport.mergeInput(request, body);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		return ResponseEntity.ok(
				ApiResult.ok(activityService.createActivity(companyId, merged, locale)));
	}

	@Activated(routeAlias = "promotions.member_signin.activities.update")
	@PutMapping(value = "/activities/{id}", name = "编辑会员签到活动", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateActivity(
			HttpServletRequest request, @PathVariable("id") long id, @FlexibleBody Map<String, Object> body) {
		long companyId =
				MemberSignInAdminSupport.readCompanyIdFromOperatorJwtMap(
						MemberSignInAdminSupport.readOperatorJwtMap(request));
		Map<String, Object> merged = MemberSignInAdminSupport.mergeInput(request, body);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		return ResponseEntity.ok(
				ApiResult.ok(activityService.updateActivity(companyId, id, merged, locale)));
	}

	@Activated(routeAlias = "promotions.member_signin.activities.stop")
	@PostMapping(value = "/activities/{id}/stop", name = "中止会员签到活动", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> stopActivity(
			HttpServletRequest request, @PathVariable("id") long id) {
		long companyId =
				MemberSignInAdminSupport.readCompanyIdFromOperatorJwtMap(
						MemberSignInAdminSupport.readOperatorJwtMap(request));
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		return ResponseEntity.ok(
				ApiResult.ok(activityService.stopActivity(companyId, id, locale)));
	}

	@Activated(routeAlias = "promotions.member_signin.coupons.candidates")
	@GetMapping(value = "/coupons/candidates", name = "会员签到阶梯券候选", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> listCouponCandidates(
			HttpServletRequest request,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "page_size", required = false) String pageSize) {
		long companyId =
				MemberSignInAdminSupport.readCompanyIdFromOperatorJwtMap(
						MemberSignInAdminSupport.readOperatorJwtMap(request));
		return ResponseEntity.ok(
				ApiResult.ok(couponCandidateService.listCandidates(companyId, keyword, page, pageSize)));
	}
}
