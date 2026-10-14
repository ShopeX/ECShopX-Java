package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MemberTaskFrontDisplaySupportTest {

	@Test
	void frequencyText_onceAndCycle() {
		assertThat(MemberTaskFrontDisplaySupport.frequencyText("once", null))
				.isEqualTo("任务周期内仅可完成一次");
		assertThat(MemberTaskFrontDisplaySupport.frequencyText("cycle", "daily"))
				.isEqualTo("每日可完成");
	}

	@Test
	void shareFieldsFromRuleConfig_parsesShareTriplet() throws Exception {
		String json =
				"{\"share_cover\":\"https://cdn/c.png\",\"share_title\":\"标题\",\"share_jump_page\":\"/pages/a\"}";
		Map<String, String> share =
				MemberTaskFrontDisplaySupport.shareFieldsFromRuleConfig(json, new ObjectMapper());
		assertThat(share.get("share_cover")).isEqualTo("https://cdn/c.png");
		assertThat(share.get("share_title")).isEqualTo("标题");
		assertThat(share.get("share_jump_page")).isEqualTo("/pages/a");
	}

	@Test
	void frontGrantStatusText_mapsGranted() {
		assertThat(MemberTaskFrontDisplaySupport.frontGrantStatusText("granted"))
				.isEqualTo("奖励已发放");
	}
}
