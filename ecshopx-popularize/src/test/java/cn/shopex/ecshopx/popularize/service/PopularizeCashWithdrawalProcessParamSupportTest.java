package cn.shopex.ecshopx.popularize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PopularizeCashWithdrawalProcessParamSupportTest {

	@Test
	void resolveProcessType_readsFromJsonBodyWhenQueryMissing() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("process_type", "argee");
		body.put("country_code", "zh-CN");

		assertThat(PopularizeCashWithdrawalProcessParamSupport.resolveProcessType(null, body))
				.isEqualTo("argee");
	}

	@Test
	void resolveProcessType_readsRejectFromJsonBody() {
		Map<String, Object> body = Map.of("process_type", "reject", "remarks", "资料不全");

		assertThat(PopularizeCashWithdrawalProcessParamSupport.resolveProcessType(null, body))
				.isEqualTo("reject");
	}

	@Test
	void resolveProcessType_prefersQueryOverBody() {
		Map<String, Object> body = Map.of("process_type", "reject");

		assertThat(PopularizeCashWithdrawalProcessParamSupport.resolveProcessType("argee", body))
				.isEqualTo("argee");
	}

	@Test
	void resolveProcessType_throwsWhenMissing() {
		assertThatThrownBy(() -> PopularizeCashWithdrawalProcessParamSupport.resolveProcessType(null, null))
				.isInstanceOf(ResourceException.class)
				.hasMessage("参数错误");
		assertThatThrownBy(
						() ->
								PopularizeCashWithdrawalProcessParamSupport.resolveProcessType(
										"  ", Map.of("country_code", "zh-CN")))
				.isInstanceOf(ResourceException.class)
				.hasMessage("参数错误");
	}

	@Test
	void resolveRemarks_fallsBackToBodyWhenQueryBlank() {
		Map<String, Object> body = Map.of("remarks", "拒绝原因");

		assertThat(PopularizeCashWithdrawalProcessParamSupport.resolveRemarks(null, body))
				.isEqualTo("拒绝原因");
		assertThat(PopularizeCashWithdrawalProcessParamSupport.resolveRemarks("  ", body))
				.isEqualTo("拒绝原因");
		assertThat(PopularizeCashWithdrawalProcessParamSupport.resolveRemarks("query备注", body))
				.isEqualTo("query备注");
	}
}
