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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class DistributorWriteRepository {

	private final DistributorMapper distributorMapper;

	public DistributorWriteRepository(DistributorMapper distributorMapper) {
		this.distributorMapper = distributorMapper;
	}

	public long countByCompanyAndNameNotDeleted(long companyId, String name) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getName, name).ne(Distributor::getIsValid, "delete");
		return distributorMapper.selectCount(w);
	}

	public long countByCompanyAndShopCodeNotDeleted(long companyId, String shopCode) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getShopCode, shopCode).ne(Distributor::getIsValid, "delete");
		return distributorMapper.selectCount(w);
	}

	public Optional<Long> findDistributorSelfId(long companyId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getDistributorSelf, 1).ne(Distributor::getIsValid, "delete").last("LIMIT 1");
		Distributor row = distributorMapper.selectOne(w);
		return row == null ? Optional.empty() : Optional.of(row.getDistributorId());
	}

	public boolean existsDefaultDistributor(long companyId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getIsDefault, 1).ne(Distributor::getIsValid, "delete");
		return distributorMapper.selectCount(w) > 0;
	}

	public boolean existsOtherWithShopCode(long companyId, String shopCode, Long excludeDistributorId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getShopCode, shopCode).ne(Distributor::getIsValid, "delete");
		if (excludeDistributorId != null) {
			w.ne(Distributor::getDistributorId, excludeDistributorId);
		}
		return distributorMapper.selectCount(w) > 0;
	}

	public boolean existsOtherWithMobile(long companyId, String mobile, Long excludeDistributorId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getMobile, mobile).ne(Distributor::getIsValid, "delete");
		if (excludeDistributorId != null) {
			w.ne(Distributor::getDistributorId, excludeDistributorId);
		}
		return distributorMapper.selectCount(w) > 0;
	}

	public boolean existsOtherWithWdtShopNo(long companyId, String wdtShopNo, Long excludeDistributorId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getWdtShopNo, wdtShopNo).ne(Distributor::getIsValid, "delete");
		if (excludeDistributorId != null) {
			w.ne(Distributor::getDistributorId, excludeDistributorId);
		}
		return distributorMapper.selectCount(w) > 0;
	}

	public boolean existsOtherWithJstShopId(long companyId, Long jstShopId, Long excludeDistributorId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getJstShopId, jstShopId).ne(Distributor::getIsValid, "delete");
		if (excludeDistributorId != null) {
			w.ne(Distributor::getDistributorId, excludeDistributorId);
		}
		return distributorMapper.selectCount(w) > 0;
	}

	public long countDistributorsForMerchantInList(long companyId, long merchantId, List<Long> distributorIds) {
		if (distributorIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getMerchantId, merchantId)
				.in(Distributor::getDistributorId, distributorIds)
				.ne(Distributor::getIsValid, "delete");
		return distributorMapper.selectCount(w);
	}

	public Optional<Distributor> selectSimpleByCompanyAndId(long companyId, long distributorId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getDistributorId, distributorId).ne(Distributor::getIsValid, "delete");
		return Optional.ofNullable(distributorMapper.selectOne(w));
	}

	/** 同城配店名查重：同名且非当前店且非 delete。 */
	public Optional<Distributor> selectByCompanyAndNameNotSelf(long companyId, String name, long excludeDistributorId) {
		if (name == null || name.isBlank()) {
			return Optional.empty();
		}
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getName, name)
				.ne(Distributor::getDistributorId, excludeDistributorId)
				.ne(Distributor::getIsValid, "delete")
				.last("LIMIT 1");
		return Optional.ofNullable(distributorMapper.selectOne(w));
	}

	/**
	 * 线下售后店铺 ID 列表中是否存在归属商户与 {@code merchantId} 不一致的店铺（非 delete）。
	 */
	public long countMerchantMismatchForOfflineAftersalesIds(long companyId, long merchantId, java.util.List<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return 0L;
		}
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.ne(Distributor::getMerchantId, merchantId)
				.in(Distributor::getDistributorId, distributorIds)
				.ne(Distributor::getIsValid, "delete");
		return distributorMapper.selectCount(w);
	}

	public int updatePaymentSubjectOnly(long companyId, long distributorId, int paymentSubject) {
		LambdaUpdateWrapper<Distributor> u = new LambdaUpdateWrapper<>();
		u.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorId, distributorId)
				.ne(Distributor::getIsValid, "delete")
				.set(Distributor::getPaymentSubject, paymentSubject);
		return distributorMapper.update(null, u);
	}

	public int clearIsDefaultForCompanyMainDistributors(long companyId) {
		LambdaUpdateWrapper<Distributor> u = new LambdaUpdateWrapper<>();
		u.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getIsDistributor, true)
				.ne(Distributor::getIsValid, "delete")
				.set(Distributor::getIsDefault, 0);
		return distributorMapper.update(null, u);
	}

	public int setIsDefaultForDistributor(long companyId, long distributorId, int isDefault) {
		LambdaUpdateWrapper<Distributor> u = new LambdaUpdateWrapper<>();
		u.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorId, distributorId)
				.ne(Distributor::getIsValid, "delete")
				.set(Distributor::getIsDefault, isDefault);
		return distributorMapper.update(null, u);
	}

	public Optional<Distributor> selectByCompanyIdAndWechatWorkDepartmentId(long companyId, int wechatWorkDepartmentId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getWechatWorkDepartmentId, wechatWorkDepartmentId)
				.ne(Distributor::getIsValid, "delete")
				.last("LIMIT 1");
		return Optional.ofNullable(distributorMapper.selectOne(w));
	}

	public int unbindWechatWorkDepartmentByCompanyAndWechatDeptId(long companyId, int wechatWorkDepartmentId) {
		LambdaUpdateWrapper<Distributor> u = new LambdaUpdateWrapper<>();
		u.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getWechatWorkDepartmentId, wechatWorkDepartmentId)
				.ne(Distributor::getIsValid, "delete")
				.set(Distributor::getWechatWorkDepartmentId, 0)
				.set(Distributor::getIsDefault, 0);
		return distributorMapper.update(null, u);
	}

	/**
	 * 按企业与可选商户、可选店铺 ID 列表批量更新配送距离；{@code distributorIds} 为 {@code null} 时不按店铺 ID 过滤。
	 */
	public int updateDeliveryDistanceForScope(long companyId, Long merchantId, List<Long> distributorIds, int deliveryDistance) {
		LambdaUpdateWrapper<Distributor> u = new LambdaUpdateWrapper<>();
		u.eq(Distributor::getCompanyId, companyId).ne(Distributor::getIsValid, "delete");
		if (merchantId != null) {
			u.eq(Distributor::getMerchantId, merchantId);
		}
		if (distributorIds != null && !distributorIds.isEmpty()) {
			u.in(Distributor::getDistributorId, distributorIds);
		}
		u.set(Distributor::getDeliveryDistance, deliveryDistance);
		return distributorMapper.update(null, u);
	}
}
