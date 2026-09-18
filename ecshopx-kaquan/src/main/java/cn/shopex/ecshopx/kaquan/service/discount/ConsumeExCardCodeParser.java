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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class ConsumeExCardCodeParser {

	public record Parsed(long userCardId, String verifySegment) {}

	public Parsed parse(String rawCode) {
		if (rawCode == null || rawCode.isEmpty()) {
			throw new BadRequestException("code 错误");
		}
		String[] parts = rawCode.split("-", 2);
		if (parts.length < 2) {
			throw new BadRequestException("code 错误");
		}
		String head = parts[0] == null ? "" : parts[0].trim();
		String tail = parts[1] == null ? "" : parts[1].trim();
		if (head.isEmpty() || tail.isEmpty()) {
			throw new BadRequestException("code 错误");
		}
		try {
			long id = Long.parseLong(head);
			if (id <= 0L) {
				throw new BadRequestException("code 错误");
			}
			return new Parsed(id, tail);
		} catch (NumberFormatException e) {
			throw new BadRequestException("code 错误");
		}
	}
}
