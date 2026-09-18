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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsModifySmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsSignWriteService {

	private final SignMapper signMapper;
	private final AliyunsmsModifySmsSignJobDispatchPublisher modifySmsSignJobDispatchPublisher;
	private final AliyunsmsAddSmsSignJobDispatchPublisher addSmsSignJobDispatchPublisher;

	public AliyunsmsSignWriteService(
			SignMapper signMapper,
			AliyunsmsModifySmsSignJobDispatchPublisher modifySmsSignJobDispatchPublisher,
			AliyunsmsAddSmsSignJobDispatchPublisher addSmsSignJobDispatchPublisher) {
		this.signMapper = signMapper;
		this.modifySmsSignJobDispatchPublisher = modifySmsSignJobDispatchPublisher;
		this.addSmsSignJobDispatchPublisher = addSmsSignJobDispatchPublisher;
	}

	public void addSign(
			long companyId,
			String signName,
			int signSource,
			String remark,
			boolean thirdParty,
			String qualificationId,
			String signFile,
			String delegateFile) {
		Sign duplicate =
				signMapper.selectOne(
						new LambdaQueryWrapper<Sign>()
								.eq(Sign::getCompanyId, companyId)
								.eq(Sign::getSignName, signName));
		if (duplicate != null) {
			throw new ResourceException("签名不能重复");
		}
		addSmsSignJobDispatchPublisher.publish(companyId, signName, signSource, remark, thirdParty, qualificationId);
		int now = (int) Instant.now().getEpochSecond();
		Sign row = new Sign();
		row.setCompanyId(companyId);
		row.setSignName(signName);
		row.setSignSource(Integer.toString(signSource));
		row.setRemark(remark);
		row.setThirdParty(thirdParty ? 1 : 0);
		row.setQualificationId(qualificationId);
		row.setStatus("0");
		row.setReason("");
		row.setCreated(now);
		row.setUpdated(now);
		if (signFile != null) {
			row.setSignFile(signFile);
		}
		if (delegateFile != null) {
			row.setDelegateFile(delegateFile);
		}
		signMapper.insert(row);
	}

	public void modifySign(
			long companyId,
			Object rawId,
			boolean signNameKeyPresent,
			Object rawSignName,
			int signSource,
			String remark,
			boolean thirdParty,
			String qualificationId,
			String signFile,
			String delegateFile) {
		Sign existing = resolveRowAfterModifySignCheckValid(companyId, rawId, signNameKeyPresent, rawSignName);
		long signId = existing.getId();
		String signNameForCloud = existing.getSignName();

		LambdaUpdateWrapper<Sign> wrapper =
				new LambdaUpdateWrapper<Sign>()
						.eq(Sign::getCompanyId, companyId)
						.eq(Sign::getId, signId)
						.set(Sign::getSignSource, Integer.toString(signSource))
						.set(Sign::getRemark, remark)
						.set(Sign::getThirdParty, thirdParty ? 1 : 0)
						.set(Sign::getQualificationId, qualificationId)
						.set(Sign::getStatus, "0")
						.set(Sign::getUpdated, (int) Instant.now().getEpochSecond());
		if (signFile != null) {
			wrapper.set(Sign::getSignFile, signFile);
		}
		if (delegateFile != null) {
			wrapper.set(Sign::getDelegateFile, delegateFile);
		}
		int rows = signMapper.update(null, wrapper);
		if (rows != 1) {
			throw new ResourceException("未查询到更新数据");
		}
		modifySmsSignJobDispatchPublisher.publish(
				companyId,
				signNameForCloud,
				signSource,
				remark,
				thirdParty,
				qualificationId,
				signFile,
				delegateFile);
	}

	/**
	 * Validates modify parameters and resolves the {@code status=2} row to update.
	 * When {@code id} is truthy (non-null, non-zero, non-empty except {@code "0"}), requires {@code sign_name} key
	 * to be present — missing key throws 500. When {@code id} is falsy, checks for duplicate sign name instead.
	 */
	private Sign resolveRowAfterModifySignCheckValid(
			long companyId, Object rawId, boolean signNameKeyPresent, Object rawSignName) {
		if (hasValidSignId(rawId)) {
			Sign sign =
					signMapper.selectOne(
							new LambdaQueryWrapper<Sign>()
									.eq(Sign::getCompanyId, companyId)
									.apply("id = {0}", rawId)
									.eq(Sign::getStatus, "2"));
			if (sign == null) {
				throw new ResourceException("未审核通过的签名才能修改");
			}
			assertSignNamePresent(signNameKeyPresent, rawSignName);
			return sign;
		}
		assertSignNamePresent(signNameKeyPresent, rawSignName);
		String signName = normalizeSignNameForDuplicateCheck(rawSignName);
		Sign duplicate =
				signMapper.selectOne(
						new LambdaQueryWrapper<Sign>()
								.eq(Sign::getCompanyId, companyId)
								.eq(Sign::getSignName, signName));
		if (duplicate != null) {
			throw new ResourceException("签名不能重复");
		}
		long idFilter = coerceLongForModifyFilter(rawId);
		Sign byId =
				signMapper.selectOne(
						new LambdaQueryWrapper<Sign>()
								.eq(Sign::getCompanyId, companyId)
								.eq(Sign::getId, idFilter)
								.eq(Sign::getStatus, "2"));
		if (byId == null) {
			throw new ResourceException("未审核通过的签名才能修改");
		}
		return byId;
	}

	/**
	 * Checks whether {@code id} is "truthy": false for null, 0, "0", "", and numeric zero; true for
	 * non-empty strings other than exact {@code "0"} (e.g. {@code "abc"}).
	 */
	private static boolean hasValidSignId(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			double d = n.doubleValue();
			return !Double.isNaN(d) && d != 0.0;
		}
		if (v instanceof String s) {
			if (s.isEmpty()) {
				return false;
			}
			return !"0".equals(s);
		}
		return true;
	}

	private static void assertSignNamePresent(boolean signNameKeyPresent, Object rawSignName) {
		if (!signNameKeyPresent || rawSignName == null || rawSignName.toString().trim().isEmpty()) {
			throw new BadRequestException("缺少必填字段: sign_name");
		}
	}

	private static String normalizeSignNameForDuplicateCheck(Object rawSignName) {
		if (rawSignName instanceof String s) {
			return s.trim();
		}
		return String.valueOf(rawSignName).trim();
	}

	private static long coerceLongForModifyFilter(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		if (v instanceof Boolean) {
			return 0L;
		}
		return 0L;
	}
}
