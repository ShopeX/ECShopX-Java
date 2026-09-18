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

package cn.shopex.ecshopx.wsugc.service.message;

import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class FrontUgcMessageMarkReadService {

	private final MessageMapper messageMapper;

	public FrontUgcMessageMarkReadService(MessageMapper messageMapper) {
		this.messageMapper = messageMapper;
	}

	public void markRead(long toUserId, String type) {
		LambdaUpdateWrapper<Message> w = new LambdaUpdateWrapper<Message>()
				.eq(Message::getType, type)
				.eq(Message::getToUserId, toUserId)
				.set(Message::getHasRead, true)
				.set(Message::getToUserId, toUserId)
				.set(Message::getType, type);
		messageMapper.update(null, w);
	}
}
