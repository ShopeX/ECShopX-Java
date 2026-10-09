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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/** 切片 0 骨架：明确未实现路径（SSOT §5.5 · D7）。 */
@Service
public class MemberTaskSkeletonService {

	private final MessageSource messageSource;

	public MemberTaskSkeletonService(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void throwNotImplemented(Locale locale) {
		throw new BadRequestException(
				messageSource.getMessage(
						"promotions.member_task.not_implemented",
						null,
						"该功能尚未开放",
						locale));
	}
}
