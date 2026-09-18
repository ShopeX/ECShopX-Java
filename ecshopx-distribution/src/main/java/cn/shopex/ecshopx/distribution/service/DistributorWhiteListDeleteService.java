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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.domain.DistributorWhiteList;
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DistributorWhiteListDeleteService {

	private final DistributorWhiteListMapper mapper;

	public DistributorWhiteListDeleteService(DistributorWhiteListMapper mapper) {
		this.mapper = mapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteWhiteList(
			boolean deleteByWhiteListId, Object idRaw, String operatorType, Long jwtDistributorId) {
		if (deleteByWhiteListId) {
			long scopedDistributorId = 0L;
			String ot = operatorType == null ? "" : operatorType;
			if ("distributor".equalsIgnoreCase(ot.trim())) {
				scopedDistributorId = jwtDistributorId == null ? 0L : jwtDistributorId;
			}
			List<Long> whiteIds = parseWhiteListPrimaryKeyIdsLenient(idRaw);
			if (whiteIds.isEmpty()) {
				return;
			}
			List<DistributorWhiteList> rows =
					mapper.selectList(Wrappers.<DistributorWhiteList>lambdaQuery().in(DistributorWhiteList::getId, whiteIds));
			for (DistributorWhiteList row : rows) {
				String mobile = row.getMobile();
				if (mobile == null || !StringUtils.hasText(mobile)) {
					continue;
				}
				var w = Wrappers.<DistributorWhiteList>lambdaQuery().eq(DistributorWhiteList::getMobile, mobile);
				if (scopedDistributorId > 0L) {
					w = w.eq(DistributorWhiteList::getDistributorId, scopedDistributorId);
				}
				mapper.delete(w);
			}
		} else {
			List<Long> distributorIds = parseIdLongList(idRaw);
			if (distributorIds.isEmpty()) {
				return;
			}
			if (distributorIds.size() == 1) {
				mapper.delete(
						Wrappers.<DistributorWhiteList>lambdaQuery()
								.eq(DistributorWhiteList::getDistributorId, distributorIds.get(0)));
			} else {
				mapper.delete(
						Wrappers.<DistributorWhiteList>lambdaQuery()
								.in(DistributorWhiteList::getDistributorId, distributorIds));
			}
		}
	}

	/**
	 * Delete-by-id path: {@code lists(['id' => [...]])} ignores
	 * non-matching id filters; non-numeric array elements do not surface as client errors.
	 */
	private static List<Long> parseWhiteListPrimaryKeyIdsLenient(Object idRaw) {
		if (idRaw == null) {
			return Collections.emptyList();
		}
		if (idRaw instanceof Number n) {
			return List.of(n.longValue());
		}
		if (idRaw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("请求参数格式错误");
			}
			try {
				return List.of(Long.parseLong(t));
			} catch (NumberFormatException ex) {
				throw new BadRequestException("请求参数格式错误");
			}
		}
		if (idRaw instanceof List<?> list) {
			if (list.isEmpty()) {
				return Collections.emptyList();
			}
			List<Long> out = new ArrayList<>(list.size());
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				if (o instanceof Number num) {
					long v = num.longValue();
					if (v > 0L) {
						out.add(v);
					}
					continue;
				}
				if (o instanceof String str) {
					String ts = str.trim();
					if (!StringUtils.hasText(ts)) {
						continue;
					}
					try {
						long v = Long.parseLong(ts);
						if (v > 0L) {
							out.add(v);
						}
					} catch (NumberFormatException ex) {
						// skip: IN-list would not match non-numeric tokens
					}
					continue;
				}
				// unsupported element types: ignore (no positive integer id)
			}
			return out;
		}
		throw new BadRequestException("请求参数格式错误");
	}

	private static List<Long> parseIdLongList(Object idRaw) {
		if (idRaw == null) {
			return Collections.emptyList();
		}
		if (idRaw instanceof Number n) {
			return List.of(n.longValue());
		}
		if (idRaw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("请求参数格式错误");
			}
			try {
				return List.of(Long.parseLong(t));
			} catch (NumberFormatException ex) {
				throw new BadRequestException("请求参数格式错误");
			}
		}
		if (idRaw instanceof List<?> list) {
			if (list.isEmpty()) {
				return Collections.emptyList();
			}
			List<Long> out = new ArrayList<>(list.size());
			for (Object o : list) {
				if (o == null) {
					throw new BadRequestException("请求参数格式错误");
				}
				if (o instanceof Number num) {
					out.add(num.longValue());
				} else if (o instanceof String str) {
					String ts = str.trim();
					if (!StringUtils.hasText(ts)) {
						throw new BadRequestException("请求参数格式错误");
					}
					try {
						out.add(Long.parseLong(ts));
					} catch (NumberFormatException ex) {
						throw new BadRequestException("请求参数格式错误");
					}
				} else {
					throw new BadRequestException("请求参数格式错误");
				}
			}
			return out;
		}
		throw new BadRequestException("请求参数格式错误");
	}
}
