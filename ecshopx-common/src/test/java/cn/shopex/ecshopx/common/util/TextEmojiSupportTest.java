package cn.shopex.ecshopx.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextEmojiSupportTest {

	@Test
	void containsEmoji_trueForPictographic() {
		assertThat(TextEmojiSupport.containsEmoji("宜山路😀1号")).isTrue();
		assertThat(TextEmojiSupport.containsEmoji("🙂")).isTrue();
	}

	@Test
	void containsEmoji_falseForPlainChinese() {
		assertThat(TextEmojiSupport.containsEmoji("上海市宜山路700号")).isFalse();
		assertThat(TextEmojiSupport.containsEmoji("")).isFalse();
		assertThat(TextEmojiSupport.containsEmoji(null)).isFalse();
	}
}