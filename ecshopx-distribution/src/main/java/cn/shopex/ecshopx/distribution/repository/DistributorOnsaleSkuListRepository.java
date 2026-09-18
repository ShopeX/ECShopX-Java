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

package cn.shopex.ecshopx.distribution.repository;

import cn.shopex.ecshopx.distribution.mapper.DistributorOnsaleSkuListMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DistributorOnsaleSkuListRepository {

	private final DistributorOnsaleSkuListMapper mapper;

	public DistributorOnsaleSkuListRepository(DistributorOnsaleSkuListMapper mapper) {
		this.mapper = mapper;
	}

	public long countByFilter(DistributorOnsaleSkuListFilter f) {
		return mapper.countByFilter(f);
	}

	public List<Long> selectItemIdsPageByFilter(DistributorOnsaleSkuListFilter f, int offset, int limit) {
		return mapper.selectItemIdsPageByFilter(f, offset, limit);
	}
}
