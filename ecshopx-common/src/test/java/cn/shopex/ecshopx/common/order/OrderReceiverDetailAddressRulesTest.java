package cn.shopex.ecshopx.common.order;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderReceiverDetailAddressRulesTest {

	@Test
	void assertDetailAddressNoEmojiWhenLogistics_rejectsEmoji() {
		Map<String, Object> p = baseLogisticsParams();
		p.put("receiver_address", "宜山路😀");

		assertThatThrownBy(() -> OrderReceiverDetailAddressRules.assertDetailAddressNoEmojiWhenLogistics(p))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining(OrderReceiverDetailAddressRules.EMOJI_NOT_ALLOWED_MSG);
	}

	@Test
	void assertDetailAddressNoEmojiWhenLogistics_skipsZiti() {
		Map<String, Object> p = baseLogisticsParams();
		p.put("receipt_type", "ziti");
		p.put("receiver_address", "宜山路😀");

		assertThatCode(() -> OrderReceiverDetailAddressRules.assertDetailAddressNoEmojiWhenLogistics(p))
				.doesNotThrowAnyException();
	}

	private static Map<String, Object> baseLogisticsParams() {
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("receipt_type", "logistics");
		p.put("receiver_address", "宜山路");
		return p;
	}
}