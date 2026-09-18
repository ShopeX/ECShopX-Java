/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.adapay.api.admin.v1;

import cn.shopex.ecshopx.adapay.domain.dto.AdapayAlipayIndustryCategoryTreeNodeDto;
import cn.shopex.ecshopx.adapay.domain.dto.AdapayBankCodesListRowDto;
import cn.shopex.ecshopx.adapay.domain.dto.AdapayRegionsListRowDto;
import cn.shopex.ecshopx.adapay.service.AdapayAlipayIndustryCategoryTreeService;
import cn.shopex.ecshopx.adapay.service.AdapayBankCodesQueryService;
import cn.shopex.ecshopx.adapay.service.AdapayRegionsQueryService;
import cn.shopex.ecshopx.adapay.service.AdapayRegionsThirdQueryService;
import cn.shopex.ecshopx.adapay.service.AdapayLicenseSubmitService;
import cn.shopex.ecshopx.adapay.service.AdapayMerchantEntryCreateService;
import cn.shopex.ecshopx.adapay.service.AdapayMerchantEntryInfoService;
import cn.shopex.ecshopx.adapay.service.AdapayMerchantResidentCreateService;
import cn.shopex.ecshopx.adapay.service.AdapayMerchantResidentInfoService;
import cn.shopex.ecshopx.adapay.service.AdapayOpenAccountGenerateKeyService;
import cn.shopex.ecshopx.adapay.service.AdapayOpenAccountIsOpenService;
import cn.shopex.ecshopx.adapay.service.AdapayOpenAccountOtherCatService;
import cn.shopex.ecshopx.adapay.service.AdapayOpenAccountStepService;
import cn.shopex.ecshopx.adapay.service.AdapaySubmitLicenseInfoService;
import cn.shopex.ecshopx.adapay.service.AdapayUploadLicenseFacadeService;
import cn.shopex.ecshopx.adapay.service.AdapayWxBusinessCategoryQueryService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("openAccountAdminV1")
@RequestMapping("/api/v1/adapay")
public class OpenAccountController {

	private final AdapayUploadLicenseFacadeService adapayUploadLicenseFacadeService;
	private final AdapayLicenseSubmitService adapayLicenseSubmitService;
	private final AdapayMerchantEntryCreateService adapayMerchantEntryCreateService;
	private final AdapayMerchantResidentCreateService adapayMerchantResidentCreateService;
	private final AdapayMerchantResidentInfoService adapayMerchantResidentInfoService;
	private final FileStorageService fileStorageService;
	private final AdapayAlipayIndustryCategoryTreeService adapayAlipayIndustryCategoryTreeService;
	private final AdapayBankCodesQueryService adapayBankCodesQueryService;
	private final AdapayRegionsQueryService adapayRegionsQueryService;
	private final AdapayRegionsThirdQueryService adapayRegionsThirdQueryService;
	private final AdapayOpenAccountGenerateKeyService adapayOpenAccountGenerateKeyService;
	private final AdapayOpenAccountIsOpenService adapayOpenAccountIsOpenService;
	private final AdapayOpenAccountStepService adapayOpenAccountStepService;
	private final AdapaySubmitLicenseInfoService adapaySubmitLicenseInfoService;
	private final AdapayMerchantEntryInfoService adapayMerchantEntryInfoService;
	private final AdapayOpenAccountOtherCatService adapayOpenAccountOtherCatService;
	private final AdapayWxBusinessCategoryQueryService adapayWxBusinessCategoryQueryService;

	public OpenAccountController(
			AdapayUploadLicenseFacadeService adapayUploadLicenseFacadeService,
			AdapayLicenseSubmitService adapayLicenseSubmitService,
			AdapayMerchantEntryCreateService adapayMerchantEntryCreateService,
			AdapayMerchantResidentCreateService adapayMerchantResidentCreateService,
			AdapayMerchantResidentInfoService adapayMerchantResidentInfoService,
			FileStorageService fileStorageService,
			AdapayAlipayIndustryCategoryTreeService adapayAlipayIndustryCategoryTreeService,
			AdapayBankCodesQueryService adapayBankCodesQueryService,
			AdapayRegionsQueryService adapayRegionsQueryService,
			AdapayRegionsThirdQueryService adapayRegionsThirdQueryService,
			AdapayOpenAccountGenerateKeyService adapayOpenAccountGenerateKeyService,
			AdapayOpenAccountIsOpenService adapayOpenAccountIsOpenService,
			AdapayOpenAccountStepService adapayOpenAccountStepService,
			AdapaySubmitLicenseInfoService adapaySubmitLicenseInfoService,
			AdapayMerchantEntryInfoService adapayMerchantEntryInfoService,
			AdapayOpenAccountOtherCatService adapayOpenAccountOtherCatService,
			AdapayWxBusinessCategoryQueryService adapayWxBusinessCategoryQueryService) {
		this.adapayUploadLicenseFacadeService = adapayUploadLicenseFacadeService;
		this.adapayLicenseSubmitService = adapayLicenseSubmitService;
		this.adapayMerchantEntryCreateService = adapayMerchantEntryCreateService;
		this.adapayMerchantResidentCreateService = adapayMerchantResidentCreateService;
		this.adapayMerchantResidentInfoService = adapayMerchantResidentInfoService;
		this.fileStorageService = fileStorageService;
		this.adapayAlipayIndustryCategoryTreeService = adapayAlipayIndustryCategoryTreeService;
		this.adapayBankCodesQueryService = adapayBankCodesQueryService;
		this.adapayRegionsQueryService = adapayRegionsQueryService;
		this.adapayRegionsThirdQueryService = adapayRegionsThirdQueryService;
		this.adapayOpenAccountGenerateKeyService = adapayOpenAccountGenerateKeyService;
		this.adapayOpenAccountIsOpenService = adapayOpenAccountIsOpenService;
		this.adapayOpenAccountStepService = adapayOpenAccountStepService;
		this.adapaySubmitLicenseInfoService = adapaySubmitLicenseInfoService;
		this.adapayMerchantEntryInfoService = adapayMerchantEntryInfoService;
		this.adapayOpenAccountOtherCatService = adapayOpenAccountOtherCatService;
		this.adapayWxBusinessCategoryQueryService = adapayWxBusinessCategoryQueryService;
	}

	@Activated(routeAlias = "adapay.is_open")
	@GetMapping(value = "/is_open", name = "是否开通")
	public ResponseEntity<ApiResult<Map<String, Object>>> isOpen(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		boolean ok = adapayOpenAccountIsOpenService.isOpen(companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", ok)));
	}

	@Activated(routeAlias = "adapay.merchant_entry.create")
	@PostMapping(value = "/merchant_entry/create", name = "创建开户进件申请")
	public ResponseEntity<ApiResult<Map<String, Object>>> merchantEntryCreate(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		long operatorId = 0L;
		Object operatorIdRaw = jwtMap.get("operator_id");
		if (operatorIdRaw instanceof Number) {
			operatorId = ((Number) operatorIdRaw).longValue();
		}

		Map<String, String> params = normalizeMerchantEntryBody(body == null ? Map.of() : body);
		adapayMerchantEntryCreateService.createOrThrowDisabled(companyId, operatorId, params);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static Map<String, String> normalizeMerchantEntryBody(Map<String, Object> body) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : body.entrySet()) {
			Object v = e.getValue();
			if (v == null) {
				out.put(e.getKey(), "");
			} else if (v instanceof String s) {
				out.put(e.getKey(), s);
			} else {
				out.put(e.getKey(), String.valueOf(v));
			}
		}
		return out;
	}

	@Activated(routeAlias = "adapay.merchant_entry.info")
	@GetMapping(value = "/merchant_entry/info", name = "开户进件详情")
	public ResponseEntity<ApiResult<Object>> merchantEntryInfo(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Object data = adapayMerchantEntryInfoService.merchantEntryInfo(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "adapay.bank.list")
	@GetMapping(value = "/bank/list", name = "获取结算银行列表")
	public ResponseEntity<ApiResult<List<AdapayBankCodesListRowDto>>> getBanksLists(
			@RequestParam(value = "bank_name", required = false) String bankName,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "page_size", required = false) Integer pageSize) {
		return ResponseEntity.ok(
				ApiResult.ok(adapayBankCodesQueryService.getBanksLists(bankName, page, pageSize)));
	}

	@Activated(routeAlias = "adapay.regions.list")
	@GetMapping(value = "/regions/list", name = "获取省市列表(四位码)")
	public ResponseEntity<ApiResult<List<AdapayRegionsListRowDto>>> getRegionsLists(
			@RequestParam(value = "pid", required = false, defaultValue = "0") String pid) {
		return ResponseEntity.ok(ApiResult.ok(adapayRegionsQueryService.getRegionsLists(pid)));
	}

	@Activated(routeAlias = "adapay.regions_third.list")
	@GetMapping(value = "/regions_third/list", name = "获取省市区列表(六位码或九位码)")
	public ResponseEntity<ApiResult<List<AdapayRegionsListRowDto>>> getRegionsThirdLists(
			@RequestParam(value = "pid", required = false, defaultValue = "0") String pid) {
		return ResponseEntity.ok(ApiResult.ok(adapayRegionsThirdQueryService.getRegionsThirdLists(pid)));
	}

	@Activated(routeAlias = "adapay.merchant_resident.create")
	@PostMapping(value = "/merchant_resident/create", name = "商户入驻申请")
	public ResponseEntity<ApiResult<Map<String, Object>>> merchantResidentCreate(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		adapayMerchantResidentCreateService.apply(companyId, body == null ? Map.of() : body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "adapay.merchant_resident.info")
	@GetMapping(value = "/merchant_resident/info", name = "商户入驻详情")
	public ResponseEntity<ApiResult<Object>> merchantResidentInfo(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Object data = adapayMerchantResidentInfoService.merchantResidentInfo(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "adapay.wx_business_cat.list")
	@GetMapping(value = "/wx_business_cat/list", name = "获取微信经营类目")
	public ResponseEntity<ApiResult<Map<String, Object>>> getWxBusinessCatList(
			@RequestParam(value = "fee_type", required = false) String feeType,
			@RequestParam(value = "merchant_type_name", required = false) String merchantTypeName) {
		Map<String, Object> data =
				adapayWxBusinessCategoryQueryService.getWxBusinessCatList(feeType, merchantTypeName);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "adapay.alipay_industry_cat.list")
	@GetMapping(value = "/alipay_industry_cat/list", name = "获取支付宝行业类目")
	public ResponseEntity<ApiResult<List<AdapayAlipayIndustryCategoryTreeNodeDto>>> getAlipayIndustryCatList() {
		return ResponseEntity.ok(
				ApiResult.ok(adapayAlipayIndustryCategoryTreeService.getAlipayIndustryCatList()));
	}

	@Activated(routeAlias = "adapay.license.upload")
	@PostMapping(value = "/license/upload", name = "上传商户证照")
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadLicense(
			HttpServletRequest request,
			@RequestPart("file") MultipartFile file,
			@RequestParam(value = "file_type", required = false) String fileType) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		if (file == null || file.isEmpty()) {
			throw new BadRequestException("请上传图片");
		}
		if (!StringUtils.hasText(fileType)) {
			throw new BadRequestException("证照类型必传");
		}

		String originalFilename = file.getOriginalFilename();
		if (!StringUtils.hasText(originalFilename)) {
			originalFilename = "upload.bin";
		}
		String fileDir =
				"adapay/"
						+ System.currentTimeMillis()
						+ ThreadLocalRandom.current().nextInt(100000, 1000000)
						+ originalFilename;

		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException e) {
			throw new BadRequestException("读取上传文件失败");
		}
		fileStorageService.put("file", fileDir, bytes);

		String fileUrl = fileStorageService.privateDownloadUrl("file", fileDir, 3600);
		if (!StringUtils.hasText(fileUrl)) {
			throw new BadRequestException("图片url必传");
		}
		if (!StringUtils.hasText(fileType)) {
			throw new BadRequestException("证照类型必传");
		}

		adapayUploadLicenseFacadeService.uploadLicense(companyId, fileUrl, fileType, fileDir);
		return ResponseEntity.ok(ApiResult.ok(Map.of()));
	}

	@Activated(routeAlias = "adapay.license_submit.create")
	@PostMapping(value = "/license_submit/create", name = "提交商户证照")
	public ResponseEntity<ApiResult<Map<String, Object>>> submitLicense(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, String> params = normalizeSubmitLicenseBody(body == null ? Map.of() : body);
		adapayLicenseSubmitService.submit(companyId, params);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static Map<String, String> normalizeSubmitLicenseBody(Map<String, Object> body) {
		Map<String, String> out = new LinkedHashMap<>();
		putNorm(out, body, "legal_certId_front_url");
		putNorm(out, body, "legal_cert_id_back_url");
		putNorm(out, body, "account_opening_permit_url");
		putNorm(out, body, "is_sms");
		putNorm(out, body, "social_credit_code_url");
		putNorm(out, body, "business_add");
		putNorm(out, body, "store_url");
		putNorm(out, body, "transaction_test_record_url");
		putNorm(out, body, "web_pic_url");
		putNorm(out, body, "lease_contract_url");
		putNorm(out, body, "settle_account_certificate_url");
		putNorm(out, body, "buss_support_materials_url");
		putNorm(out, body, "icp_registration_license_url");
		putNorm(out, body, "industry_qualify_doc_type");
		putNorm(out, body, "industry_qualify_doc_license_url");
		putNorm(out, body, "cert_back_image_url");
		putNorm(out, body, "cert_front_image_url");
		putNorm(out, body, "cert_id");
		putNorm(out, body, "cert_name");
		return out;
	}

	private static void putNorm(Map<String, String> out, Map<String, Object> body, String key) {
		Object v = body.get(key);
		if (v == null) {
			out.put(key, "");
		} else if (v instanceof String s) {
			out.put(key, s);
		} else {
			out.put(key, String.valueOf(v));
		}
	}

	@Activated(routeAlias = "adapay.license_submit.info")
	@GetMapping(value = "/license_submit/info", name = "商户证照详情")
	public ResponseEntity<ApiResult<Object>> submitLicenseInfo(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Object data = adapaySubmitLicenseInfoService.submitLicenseInfo(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "adapay.open_account.step")
	@GetMapping(value = "/open_account/step", name = "商户开户步骤")
	public ResponseEntity<ApiResult<Map<String, Object>>> openAccountStep(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> data = adapayOpenAccountStepService.openAccountStep(companyId, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "adapay.generate.key")
	@GetMapping(value = "/generate/key", name = "生成RSA密钥")
	public ResponseEntity<ApiResult<Map<String, String>>> generateKey() {
		Map<String, String> data = adapayOpenAccountGenerateKeyService.generateKey();
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "adapay.other.cat")
	@GetMapping(value = "/other/cat", name = "费率 入驻 商户分类")
	public ResponseEntity<ApiResult<Map<String, Object>>> otherCat(
			@RequestParam(value = "merchant_type_name", required = false) String merchantTypeName) {
		Map<String, Object> data = adapayOpenAccountOtherCatService.otherCat(merchantTypeName);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
