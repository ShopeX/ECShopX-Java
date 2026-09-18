package cn.shopex.ecshopx.companys.api.admin.v1;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.shopex.ecshopx.common.auth.OperatorJwtBlacklistPort;
import cn.shopex.ecshopx.common.auth.OperatorJwtIssuerPort;
import cn.shopex.ecshopx.common.web.FlexibleBodyMethodArgumentResolver;
import cn.shopex.ecshopx.companys.config.CommonApiTokenProperties;
import cn.shopex.ecshopx.companys.service.OperatorBasicUserService;
import cn.shopex.ecshopx.companys.service.OperatorGetUserDataService;
import cn.shopex.ecshopx.companys.service.OperatorDistributorSelectionService;
import cn.shopex.ecshopx.companys.service.OperatorLogsQueryService;
import cn.shopex.ecshopx.companys.service.OperatorStatusChangeService;
import cn.shopex.ecshopx.companys.service.OperatorsCommandService;
import cn.shopex.ecshopx.companys.service.OperatorsImageVcodeService;
import cn.shopex.ecshopx.companys.service.OperatorsYdleadsService;
import cn.shopex.ecshopx.companys.service.auth.OperatorAuthService;
import cn.shopex.ecshopx.companys.service.auth.OperatorForgetPasswordSmsSendService;
import cn.shopex.ecshopx.companys.service.auth.OperatorPasswordResetService;
import cn.shopex.ecshopx.companys.service.auth.OperatorShopexOAuthAuthorizeUrlService;
import cn.shopex.ecshopx.companys.service.auth.OperatorTokenRefreshService;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassApplyService;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassApproveService;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassDetailPort;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassListService;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassLogQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * HTTP slice for {@link OperatorsController#getCredentials}: common-api token gate, merged query input, and delegation
 * to {@link OperatorAuthService#retrieveByCredentials(Map)} for the Shuyun admin branch (staff auto-provision occurs
 * deeper in the service graph and is covered by dispatch flow tests).
 */
class OperatorsGetCredentialsWebSliceTest {

	private static final String CREDENTIAL_PATH = "/api/v1/operator/credential";

	@Test
	void getCredentials_withValidCommonApiToken_invokesRetrieveByCredentialsAndReturnsOkBody() throws Exception {
		OperatorAuthService operatorAuthService = Mockito.mock(OperatorAuthService.class);

		CommonApiTokenProperties commonApiTokenProperties = new CommonApiTokenProperties();
		commonApiTokenProperties.setApiToken("fixture-common-api-token");

		Map<String, Object> operatorRow = new LinkedHashMap<>();
		operatorRow.put("operator_id", 501L);
		operatorRow.put("company_id", 9001L);
		operatorRow.put("login_name", "13100000000");
		operatorRow.put("merchant_id", 0L);
		operatorRow.put("shop_ids", java.util.List.of());
		when(operatorAuthService.retrieveByCredentials(Mockito.any())).thenReturn(operatorRow);

		OperatorsController controller =
				new OperatorsController(
						Mockito.mock(OperatorDataPassApplyService.class),
						Mockito.mock(OperatorDataPassApproveService.class),
						Mockito.mock(OperatorDataPassListService.class),
						Mockito.mock(OperatorDataPassDetailPort.class),
						Mockito.mock(OperatorsYdleadsService.class),
						operatorAuthService,
						Mockito.mock(OperatorJwtIssuerPort.class),
						Mockito.mock(OperatorJwtBlacklistPort.class),
						Mockito.mock(OperatorPasswordResetService.class),
						Mockito.mock(OperatorDistributorSelectionService.class),
						Mockito.mock(OperatorForgetPasswordSmsSendService.class),
						Mockito.mock(OperatorsImageVcodeService.class),
						Mockito.mock(OperatorStatusChangeService.class),
						Mockito.mock(OperatorsCommandService.class),
						Mockito.mock(OperatorLogsQueryService.class),
						Mockito.mock(OperatorDataPassLogQueryService.class),
						Mockito.mock(OperatorShopexOAuthAuthorizeUrlService.class),
						Mockito.mock(OperatorBasicUserService.class),
						Mockito.mock(OperatorGetUserDataService.class),
						commonApiTokenProperties,
						Mockito.mock(OperatorTokenRefreshService.class));

		ObjectMapper objectMapper = new ObjectMapper();
		FlexibleBodyMethodArgumentResolver flexibleResolver = new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		MockMvc mockMvc =
				MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(flexibleResolver).build();

		mockMvc
				.perform(
						get(CREDENTIAL_PATH)
								.param("token", "fixture-common-api-token")
								.param("logintype", "shuyunadmin")
								.param("code", "shopextest2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.operator_id").value(501))
				.andExpect(jsonPath("$.data.company_id").value(9001));

		verify(operatorAuthService)
				.retrieveByCredentials(
						argThat(
								m ->
										m != null
												&& "shuyunadmin"
														.equals(String.valueOf(m.get("logintype")).trim())));
	}
}
