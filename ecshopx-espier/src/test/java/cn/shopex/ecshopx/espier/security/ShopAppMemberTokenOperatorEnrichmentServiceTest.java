package cn.shopex.ecshopx.espier.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ShopAppMemberTokenOperatorEnrichmentServiceTest {

	@Mock
	private MembersMapper membersMapper;

	@Mock
	private CompanysMapper companysMapper;

	@Mock
	private OperatorsQueryService operatorsQueryService;

	private ShopAppMemberTokenOperatorEnrichmentService service;

	@BeforeEach
	void setUp() {
		service =
				new ShopAppMemberTokenOperatorEnrichmentService(
						membersMapper, companysMapper, operatorsQueryService, new ObjectMapper());
		ReflectionTestUtils.setField(service, "defaultProductModel", "platform");
	}

	@Test
	void enrichIfApplicable_mergesCamelCaseOperatorFields() {
		Members member = new Members();
		member.setUserId(1172L);
		member.setCompanyId(141L);
		member.setMobile("15601636091");
		when(membersMapper.selectOne(any(Wrapper.class))).thenReturn(member);

		Companys company = new Companys();
		company.setMenuType(3);
		when(companysMapper.selectById(141L)).thenReturn(company);

		Map<String, Object> operatorRow = new LinkedHashMap<>();
		operatorRow.put("operatorId", 749L);
		operatorRow.put("operatorType", "distributor");
		operatorRow.put(
				"distributorIds",
				"[{\"distributor_id\":285,\"name\":\"BBC店铺1号\"}]");
		operatorRow.put("isDisable", 0);
		when(operatorsQueryService.getInfo(any())).thenReturn(operatorRow);

		Map<String, Object> userData = new LinkedHashMap<>();
		userData.put(
				"id",
				"1172_espier_o2s9l5RUv5NRsYkvFzok4YT0umv8_espier_o2Ank0ixr2v2Aw-rsOfVLx4dUUuQg");
		userData.put("company_id", 141);
		userData.put("operator_type", "user");

		service.enrichIfApplicable(userData);

		assertThat(userData.get("operator_type")).isEqualTo("distributor");
		assertThat(userData.get("operator_id")).isEqualTo(749L);
		assertThat(userData.get("source")).isEqualTo("user");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> distributorIds = (List<Map<String, Object>>) userData.get("distributor_ids");
		assertThat(distributorIds).hasSize(1);
		assertThat(distributorIds.get(0).get("distributor_id")).isEqualTo(285);
	}

}
