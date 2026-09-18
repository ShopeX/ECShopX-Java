package cn.shopex.ecshopx.companys.api.admin.v1;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.web.FlexibleBodyMethodArgumentResolver;
import cn.shopex.ecshopx.companys.service.shopex.ShopexAdminBindService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ShopexBindControllerWebSliceTest {

	private ShopexAdminBindService shopexAdminBindService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		shopexAdminBindService = Mockito.mock(ShopexAdminBindService.class);
		ObjectMapper objectMapper = new ObjectMapper();
		FlexibleBodyMethodArgumentResolver flexibleResolver =
				new FlexibleBodyMethodArgumentResolver(objectMapper, null);
		mockMvc =
				MockMvcBuilders.standaloneSetup(new ShopexBindController(shopexAdminBindService))
						.setCustomArgumentResolvers(flexibleResolver)
						.build();
	}

	@Test
	void status_returnsBoundFalse() throws Exception {
		when(shopexAdminBindService.getStatusForOperatorId(1L)).thenReturn(Map.of("bound", false));

		mockMvc
				.perform(get("/api/v1/operator/shopex-bind/status").requestAttr(
						OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA, adminJwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.bound").value(false));

		verify(shopexAdminBindService).getStatusForOperatorId(1L);
	}

	@Test
	void bind_delegatesCredentials() throws Exception {
		when(shopexAdminBindService.bindForAdminOperator(Mockito.eq(1L), Mockito.any()))
				.thenReturn(Map.of("bound", true));

		mockMvc
				.perform(
						post("/api/v1/operator/shopex-bind")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"username\":\"u\",\"password\":\"p\"}")
								.requestAttr(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA, adminJwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.bound").value(true));
	}

	private static Map<String, Object> adminJwt() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "admin");
		jwt.put("operator_id", 1L);
		jwt.put("company_id", 1L);
		return jwt;
	}
}
