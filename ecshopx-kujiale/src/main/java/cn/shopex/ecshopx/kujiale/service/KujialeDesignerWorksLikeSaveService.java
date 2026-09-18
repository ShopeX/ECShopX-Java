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

package cn.shopex.ecshopx.kujiale.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorksLike;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksLikeMapper;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class KujialeDesignerWorksLikeSaveService {

	private final KujialeDesignerWorksLikeMapper kujialeDesignerWorksLikeMapper;
	private final KujialeDesignerWorksMapper kujialeDesignerWorksMapper;

	public KujialeDesignerWorksLikeSaveService(
			KujialeDesignerWorksLikeMapper kujialeDesignerWorksLikeMapper,
			KujialeDesignerWorksMapper kujialeDesignerWorksMapper) {
		this.kujialeDesignerWorksLikeMapper = kujialeDesignerWorksLikeMapper;
		this.kujialeDesignerWorksMapper = kujialeDesignerWorksMapper;
	}

	public void saveLike(String designId, String planId, int userId, String type) {
		try {
			doSaveLike(designId, planId, userId, type);
		} catch (Exception e) {
			throw new ResourceException("点赞异常");
		}
	}

	private void doSaveLike(String designId, String planId, int userId, String type) {
		LambdaQueryWrapper<KujialeDesignerWorksLike> w = new LambdaQueryWrapper<>();
		w.eq(KujialeDesignerWorksLike::getDesignId, designId)
				.eq(KujialeDesignerWorksLike::getPlanId, planId)
				.eq(KujialeDesignerWorksLike::getUserId, userId);
		KujialeDesignerWorksLike like = kujialeDesignerWorksLikeMapper.selectOne(w);

		if (like != null && "like".equals(type)) {
			throw new ResourceException("已经点赞过");
		}
		if (like == null && "like".equals(type)) {
			int now = (int) Instant.now().getEpochSecond();
			KujialeDesignerWorksLike row = new KujialeDesignerWorksLike();
			row.setDesignId(designId);
			row.setPlanId(planId);
			row.setUserId(userId);
			row.setCreated(now);
			row.setUpdated(now);
			kujialeDesignerWorksLikeMapper.insert(row);
			kujialeDesignerWorksMapper.incrementLikeCount(designId, planId);
			return;
		}
		if (like != null && "unlike".equals(type)) {
			kujialeDesignerWorksLikeMapper.deleteById(like.getId());
			kujialeDesignerWorksMapper.decrementLikeCount(designId, planId);
		}
	}
}
