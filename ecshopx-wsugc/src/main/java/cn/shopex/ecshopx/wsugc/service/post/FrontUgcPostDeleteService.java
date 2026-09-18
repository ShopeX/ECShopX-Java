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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontUgcPostDeleteService {

	private final PostMapper postMapper;

	public FrontUgcPostDeleteService(PostMapper postMapper) {
		this.postMapper = postMapper;
	}

	public Map<String, Object> deleteByMember(long userId, Object rawPostId) {
		validatePresent(rawPostId);

		boolean multiFromCollection = rawPostId instanceof Collection<?> col && !col.isEmpty();
		boolean multiFromArray = isArrayNonEmpty(rawPostId);

		if (multiFromCollection || multiFromArray) {
			return deleteMulti(userId, rawPostId);
		}
		return deleteScalar(userId, rawPostId);
	}

	private static void validatePresent(Object rawPostId) {
		if (rawPostId == null) {
			throw new ResourceException("post_id参数不能为空");
		}
		if (rawPostId instanceof String s && !StringUtils.hasText(s)) {
			throw new ResourceException("post_id参数不能为空");
		}
		if (rawPostId instanceof Collection<?> col && col.isEmpty()) {
			throw new ResourceException("post_id参数不能为空");
		}
		if (isArray(rawPostId) && arrayLength(rawPostId) == 0) {
			throw new ResourceException("post_id参数不能为空");
		}
	}

	private Map<String, Object> deleteMulti(long userId, Object rawPostId) {
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		List<Object> displayTokens = new ArrayList<>();

		if (rawPostId instanceof Collection<?> col) {
			for (Object el : col) {
				displayTokens.add(toDisplayToken(el));
				Long v = tryParseLong(el);
				if (v == null || v <= 0L) {
					throw new ResourceException("post_id参数不能为空");
				}
				ids.add(v);
			}
		} else if (isArray(rawPostId)) {
			forEachArrayElement(rawPostId, el -> {
				displayTokens.add(toDisplayToken(el));
				Long v = tryParseLong(el);
				if (v == null || v <= 0L) {
					throw new ResourceException("post_id参数不能为空");
				}
				ids.add(v);
			});
		} else {
			throw new ResourceException("post_id参数不能为空");
		}

		if (ids.isEmpty()) {
			throw new ResourceException("post_id参数不能为空");
		}

		assertOwnership(userId, ids);
		runUpdate(ids);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("post_id", displayTokens);
		out.put("message", "删除成功");
		return out;
	}

	private Map<String, Object> deleteScalar(long userId, Object rawPostId) {
		if (rawPostId instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new ResourceException("post_id参数不能为空");
			}
			LinkedHashSet<Long> ids = new LinkedHashSet<>();
			ids.add(v);
			assertOwnership(userId, ids);
			runUpdate(ids);
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("post_id", v);
			out.put("message", "删除成功");
			return out;
		}
		if (rawPostId instanceof String s) {
			Long v = tryParseLong(s);
			if (v != null && v > 0L) {
				LinkedHashSet<Long> ids = new LinkedHashSet<>();
				ids.add(v);
				assertOwnership(userId, ids);
				runUpdate(ids);
				LinkedHashMap<String, Object> out = new LinkedHashMap<>();
				out.put("post_id", v);
				out.put("message", "删除成功");
				return out;
			}
			throw new ResourceException("post_id参数不能为空");
		}
		Long v = tryParseLong(rawPostId);
		if (v != null && v > 0L) {
			LinkedHashSet<Long> ids = new LinkedHashSet<>();
			ids.add(v);
			assertOwnership(userId, ids);
			runUpdate(ids);
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("post_id", v);
			out.put("message", "删除成功");
			return out;
		}
		throw new ResourceException("post_id参数不能为空");
	}

	private void assertOwnership(long userId, LinkedHashSet<Long> ids) {
		LambdaQueryWrapper<Post> q = new LambdaQueryWrapper<>();
		q.in(Post::getPostId, ids);
		q.select(Post::getPostId, Post::getUserId);
		List<Post> rows = postMapper.selectList(q);
		for (Post row : rows) {
			long rowUid = row.getUserId() == null ? 0L : row.getUserId();
			if (rowUid != userId) {
				throw new ResourceException("此笔记" + row.getPostId() + "不属于您，不能删除");
			}
		}
	}

	private void runUpdate(LinkedHashSet<Long> ids) {
		LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
		uw.set(Post::getDisabled, 1);
		if (ids.size() == 1) {
			uw.eq(Post::getPostId, ids.iterator().next());
		} else {
			uw.in(Post::getPostId, ids);
		}
		postMapper.update(null, uw);
	}

	private static Object toDisplayToken(Object el) {
		if (el == null) {
			return null;
		}
		Long parsed = tryParseLong(el);
		if (parsed != null) {
			return parsed;
		}
		return el.toString().trim();
	}

	private static Long tryParseLong(Object el) {
		if (el == null) {
			return null;
		}
		if (el instanceof Number n) {
			return n.longValue();
		}
		String s = el.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isArrayNonEmpty(Object raw) {
		return isArray(raw) && arrayLength(raw) > 0;
	}

	private static boolean isArray(Object raw) {
		return raw instanceof Object[]
				|| raw instanceof long[]
				|| raw instanceof int[]
				|| raw instanceof short[]
				|| raw instanceof byte[]
				|| raw instanceof char[]
				|| raw instanceof double[]
				|| raw instanceof float[]
				|| raw instanceof boolean[];
	}

	private static int arrayLength(Object raw) {
		if (raw instanceof Object[] a) {
			return a.length;
		}
		if (raw instanceof long[] a) {
			return a.length;
		}
		if (raw instanceof int[] a) {
			return a.length;
		}
		if (raw instanceof short[] a) {
			return a.length;
		}
		if (raw instanceof byte[] a) {
			return a.length;
		}
		if (raw instanceof char[] a) {
			return a.length;
		}
		if (raw instanceof double[] a) {
			return a.length;
		}
		if (raw instanceof float[] a) {
			return a.length;
		}
		if (raw instanceof boolean[] a) {
			return a.length;
		}
		return 0;
	}

	private static void forEachArrayElement(Object raw, Consumer<Object> action) {
		if (raw instanceof Object[] a) {
			for (Object o : a) {
				action.accept(o);
			}
			return;
		}
		if (raw instanceof long[] a) {
			for (long v : a) {
				action.accept(v);
			}
			return;
		}
		if (raw instanceof int[] a) {
			for (int v : a) {
				action.accept(v);
			}
			return;
		}
		if (raw instanceof short[] a) {
			for (short v : a) {
				action.accept(v);
			}
			return;
		}
		if (raw instanceof byte[] a) {
			for (byte v : a) {
				action.accept(v);
			}
			return;
		}
		if (raw instanceof char[] a) {
			for (char c : a) {
				action.accept(String.valueOf(c));
			}
			return;
		}
		if (raw instanceof double[] a) {
			for (double v : a) {
				action.accept(v);
			}
			return;
		}
		if (raw instanceof float[] a) {
			for (float v : a) {
				action.accept(v);
			}
			return;
		}
		if (raw instanceof boolean[] a) {
			for (boolean v : a) {
				action.accept(v);
			}
		}
	}
}
