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

package cn.shopex.ecshopx.common.util;

import java.util.Set;

/** 文本是否含常见表情类 Unicode 码点（图形表情块，非通用符号 So）。 */
public final class TextEmojiSupport {

	private static final Set<Character.UnicodeBlock> EMOJI_BLOCKS =
			Set.of(
					Character.UnicodeBlock.EMOTICONS,
					Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS,
					Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS,
					Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS,
					Character.UnicodeBlock.DINGBATS,
					Character.UnicodeBlock.MAHJONG_TILES,
					Character.UnicodeBlock.PLAYING_CARDS,
					Character.UnicodeBlock.ENCLOSED_ALPHANUMERIC_SUPPLEMENT);

	private TextEmojiSupport() {}

	public static boolean containsEmoji(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		return text.codePoints().anyMatch(TextEmojiSupport::isEmojiBlockCodePoint);
	}

	private static boolean isEmojiBlockCodePoint(int codePoint) {
		if (codePoint == 0xFE0F || codePoint == 0x200D) {
			return false;
		}
		if (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF) {
			return true;
		}
		Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
		return block != null && EMOJI_BLOCKS.contains(block);
	}
}