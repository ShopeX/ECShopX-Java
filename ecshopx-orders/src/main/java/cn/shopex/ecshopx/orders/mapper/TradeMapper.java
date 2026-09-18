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

package cn.shopex.ecshopx.orders.mapper;

import cn.shopex.ecshopx.orders.domain.Trade;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TradeMapper extends BaseMapper<Trade> {

	/**
	 * 有数：支付侧金额（分）；与 PHP {@code countPaymentAmount} 一致（
	 * {@code trade_state} + {@code time_expire} 窗）。
	 */
	Long sumTradeTotalFeeCentsForYoushu(
			@Param("companyId") long companyId,
			@Param("startSec") long startSec,
			@Param("endSec") long endSec);

	/** 有数：支付笔数；与 {@code countPaymentNum} 一致。 */
	long countTradesForYoushu(
			@Param("companyId") long companyId,
			@Param("startSec") long startSec,
			@Param("endSec") long endSec);

	List<Map<String, Object>> selectSuccessTradeIndexRows(
			@Param("companyId") long companyId,
			@Param("orderIds") List<Long> orderIds);
}
