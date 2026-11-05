package cn.shopex.ecshopx.distribution.api.admin.v1.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CreatePickupLocationRequestTest {

	@Test
	void deserializesCountryCodeFromAdminRequestBody() throws Exception {
		String json =
				"{\"name\":\"禾门健康\",\"province\":\"江苏省\",\"city\":\"徐州市\",\"area\":\"泉山区\","
						+ "\"address\":\"珠山艺术产业园9号楼\",\"area_code\":\"0516\",\"contract_phone\":\"8581806\","
						+ "\"hours\":[[\"08:30\",\"17:30\"]],\"workdays\":[\"2\",\"1\",\"3\",\"4\",\"5\"],"
						+ "\"wait_pickup_days\":\"5\",\"latest_pickup_time\":\"17:00\",\"country_code\":\"zh-CN\"}";

		CreatePickupLocationRequest req = new ObjectMapper().readValue(json, CreatePickupLocationRequest.class);

		assertThat(req.getName()).isEqualTo("禾门健康");
		assertThat(req.getCountryCode()).isEqualTo("zh-CN");
	}
}
