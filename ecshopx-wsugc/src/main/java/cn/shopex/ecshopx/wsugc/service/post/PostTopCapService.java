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

package cn.shopex.ecshopx.wsugc.service.post;

import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PostTopCapService {

	private final PostMapper postMapper;

	public PostTopCapService(PostMapper postMapper) {
		this.postMapper = postMapper;
	}

	public void applyTopCapAfterRequest(long postId) {
		Post current = postMapper.selectById(postId);
		if (current == null) {
			return;
		}
		LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<>();
		w.select(Post::getPostId, Post::getCreated)
				.eq(Post::getDisabled, 0)
				.eq(Post::getIsTop, 1)
				.orderByDesc(Post::getCreated);
		List<Post> nowTop = postMapper.selectList(w);

		Post curMini = new Post();
		curMini.setPostId(current.getPostId());
		curMini.setCreated(current.getCreated());

		if (nowTop != null && !nowTop.isEmpty()) {
			List<Post> merged = new ArrayList<>(nowTop);
			merged.add(curMini);
			merged.sort(
					Comparator.comparing(Post::getCreated, Comparator.nullsLast(Comparator.naturalOrder()))
							.reversed());
			Post lastRow = merged.get(merged.size() - 1);
			int max = 2;
			for (int klast = 0; klast < merged.size(); klast++) {
				Post vlast = merged.get(klast);
				if (klast + 1 > max) {
					LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
					uw.eq(Post::getPostId, vlast.getPostId()).set(Post::getPOrder, 0).set(Post::getIsTop, 0);
					postMapper.update(null, uw);
				} else {
					LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
					uw.eq(Post::getPostId, lastRow.getPostId())
							.set(Post::getPOrder, (max - klast) * (-1))
							.set(Post::getIsTop, 1);
					postMapper.update(null, uw);
				}
			}
		} else {
			LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
			uw.eq(Post::getPostId, postId).set(Post::getPOrder, -1).set(Post::getIsTop, 1);
			postMapper.update(null, uw);
		}
	}
}
