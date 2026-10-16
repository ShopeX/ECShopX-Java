package cn.shopex.ecshopx.promotions.service.membersignin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MemberSignInRewardSnapshotTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void writeAndParseRewardItems() {
		String snapshot =
				MemberSignInRewardSnapshot.writeTierItems(
						objectMapper,
						List.of(
								MemberSignInRewardSnapshot.pointsItem(500),
								MemberSignInRewardSnapshot.couponItem(99L, "满减券")));
		List<Map<String, Object>> items =
				MemberSignInRewardSnapshot.parseItems(objectMapper, snapshot);
		assertThat(items).hasSize(2);
		assertThat(items.get(0).get("type")).isEqualTo("points");
		assertThat(items.get(1).get("coupon_id")).isEqualTo(99);
	}

	@Test
	void parseLegacySnapshot() {
		List<Map<String, Object>> items =
				MemberSignInRewardSnapshot.parseItems(
						objectMapper, "{\"points\":10,\"coupon_id\":5}");
		assertThat(items).hasSize(2);
	}
}
