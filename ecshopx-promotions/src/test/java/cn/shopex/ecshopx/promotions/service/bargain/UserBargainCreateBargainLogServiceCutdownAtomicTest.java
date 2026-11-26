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

package cn.shopex.ecshopx.promotions.service.bargain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.BargainFinishSendSmsNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.BargainLog;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.UserBargains;
import cn.shopex.ecshopx.promotions.mapper.BargainLogMapper;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

/**
 * ECX-11446: concurrent help must lock the user_bargain row and atomically increment cutdown_amount.
 */
class UserBargainCreateBargainLogServiceCutdownAtomicTest {

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, BargainPromotions.class);
		TableInfoHelper.initTableInfo(assistant, UserBargains.class);
		TableInfoHelper.initTableInfo(assistant, BargainLog.class);
	}

	@Test
	@DisplayName("createBargainLog locks user_bargains FOR UPDATE and increments cutdown_amount in SQL")
	void createBargainLog_locksRowAndIncrementsCutdownInSql() {
		long endSec = Instant.now().getEpochSecond() + 3600;
		BargainPromotions promotion = new BargainPromotions();
		promotion.setBargainId(10L);
		promotion.setCompanyId(1L);
		promotion.setMktPrice(10_000);
		promotion.setPrice(5_000);
		promotion.setEndTime(endSec);

		UserBargains ub = new UserBargains();
		ub.setBargainId(10L);
		ub.setCompanyId(1L);
		ub.setUserId(20L);
		ub.setMktPrice(10_000);
		ub.setPrice(5_000);
		ub.setCutdownAmount(100);
		ub.setIsOrdered(false);
		ub.setItemName("砍价商品");
		ub.setCutpriceRange("[{\"cut\":50,\"used\":0},{\"cut\":60,\"used\":0}]");

		BargainPromotionsMapper bargainPromotionsMapper = mock(BargainPromotionsMapper.class);
		BargainLogMapper bargainLogMapper = mock(BargainLogMapper.class);
		UserBargainsMapper userBargainsMapper = mock(UserBargainsMapper.class);
		MessageSource messageSource = mock(MessageSource.class);
		when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("err");
		BargainFinishSendSmsNoticeJobDispatchPublisher publisher =
				mock(BargainFinishSendSmsNoticeJobDispatchPublisher.class);

		AtomicReference<String> selectSql = new AtomicReference<>("");
		AtomicReference<String> updateSetSql = new AtomicReference<>("");

		when(bargainPromotionsMapper.selectOne(any())).thenReturn(promotion);
		when(bargainLogMapper.selectOne(any())).thenReturn(null);
		when(bargainLogMapper.insert(any(BargainLog.class)))
				.thenAnswer(
						inv -> {
							BargainLog log = inv.getArgument(0);
							log.setBargainLogId(9001L);
							return 1;
						});
		when(userBargainsMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							Wrapper<?> w = inv.getArgument(0);
							String custom = w.getCustomSqlSegment();
							String seg = w.getSqlSegment();
							selectSql.set(
									((custom == null ? "" : custom) + " " + (seg == null ? "" : seg)).trim());
							return ub;
						});
		when(userBargainsMapper.update(any(), any()))
				.thenAnswer(
						inv -> {
							Wrapper<?> w = inv.getArgument(1);
							String set = w.getSqlSet();
							updateSetSql.set(set == null ? "" : set);
							return 1;
						});

		UserBargainCreateBargainLogService svc =
				new UserBargainCreateBargainLogService(
						bargainPromotionsMapper,
						bargainLogMapper,
						userBargainsMapper,
						messageSource,
						new ObjectMapper(),
						publisher);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", 1L);
		merged.put("bargain_id", "10");
		merged.put("user_id", "20");
		merged.put("open_id", "o-test-1");
		merged.put("authorizer_appid", "wxauth");
		merged.put("wxa_appid", "wxa");
		merged.put("nickname", "n");
		merged.put("headimgurl", "h");

		svc.createBargainLog(merged, Locale.SIMPLIFIED_CHINESE);

		verify(userBargainsMapper).selectOne(any());
		verify(userBargainsMapper).update(any(), any());
		org.assertj.core.api.Assertions.assertThat(selectSql.get().toUpperCase()).contains("FOR UPDATE");
		org.assertj.core.api.Assertions.assertThat(updateSetSql.get())
				.contains("cutdown_amount")
				.contains("+")
				.contains("50");
	}
}
