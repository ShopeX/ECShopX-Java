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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.UserBargains;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Locale;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

/** ECX-10934：报名只能参加当前公司的砍价活动。 */
@ExtendWith(MockitoExtension.class)
class UserBargainCreateUserBargainServiceCompanyScopeTest {

	@Mock
	private BargainPromotionsMapper bargainPromotionsMapper;

	@Mock
	private UserBargainsMapper userBargainsMapper;

	@Mock
	private MessageSource messageSource;

	private UserBargainCreateUserBargainService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, BargainPromotions.class);
		TableInfoHelper.initTableInfo(assistant, UserBargains.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new UserBargainCreateUserBargainService(
						bargainPromotionsMapper, userBargainsMapper, messageSource, new ObjectMapper());
		lenient()
				.when(messageSource.getMessage(
						eq("promotions.bargain.bargain_activity_not_exist_with_bargain_id"),
						any(),
						any(Locale.class)))
				.thenAnswer(
						inv -> {
							Object[] args = inv.getArgument(1);
							Object id = args == null || args.length == 0 || args[0] == null ? "" : args[0];
							return "bargain_id为" + id + "的砍价活动不存在！";
						});
	}

	private static BargainPromotions promotion(long bargainId, Long companyId) {
		long now = Instant.now().getEpochSecond();
		BargainPromotions promotion = new BargainPromotions();
		promotion.setBargainId(bargainId);
		promotion.setCompanyId(companyId);
		promotion.setItemName("砍价商品");
		promotion.setMktPrice(1000);
		promotion.setPrice(800);
		promotion.setPeopleRange("{\"min\":1,\"max\":1}");
		promotion.setBeginTime(now - 60);
		promotion.setEndTime(now + 3600);
		return promotion;
	}

	@Test
	void foreignCompanyBargain_isRejectedWithoutInsert() {
		BargainPromotions foreign = promotion(88L, 99L);
		when(bargainPromotionsMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							Wrapper<?> wrapper = inv.getArgument(0);
							String sql = wrapper.getSqlSegment() == null ? "" : wrapper.getSqlSegment();
							return sql.contains("company_id") ? null : foreign;
						});

		assertThatThrownBy(
						() ->
								service.createUserBargain(
										11L, 7L, "app", "wxa", "88", Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(ResourceException.class)
				.hasMessage("bargain_id为88的砍价活动不存在！");

		verify(userBargainsMapper, never()).insert(any(UserBargains.class));
	}

	@Test
	void promotionWithoutCompany_isRejectedWithoutInsert() {
		BargainPromotions unloaded = promotion(88L, 99L);
		unloaded.setCompanyId(null);
		when(bargainPromotionsMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							Wrapper<?> wrapper = inv.getArgument(0);
							String sql = wrapper.getSqlSegment() == null ? "" : wrapper.getSqlSegment();
							return sql.contains("company_id") ? null : unloaded;
						});

		assertThatThrownBy(
						() ->
								service.createUserBargain(
										11L, 7L, "app", "wxa", "88", Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(ResourceException.class)
				.hasMessage("bargain_id为88的砍价活动不存在！");

		verify(userBargainsMapper, never()).insert(any(UserBargains.class));
	}

	@Test
	void ownCompanyBargain_insertsEnrollment() {
		when(bargainPromotionsMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							Wrapper<?> wrapper = inv.getArgument(0);
							String sql = wrapper.getSqlSegment() == null ? "" : wrapper.getSqlSegment();
							return sql.contains("company_id") ? promotion(88L, 11L) : null;
						});
		UserBargains saved = new UserBargains();
		saved.setBargainId(88L);
		saved.setCompanyId(11L);
		saved.setUserId(7L);
		saved.setItemName("砍价商品");
		saved.setMktPrice(1000);
		saved.setPrice(800);
		saved.setCutpriceNum(1);
		saved.setCutpriceRange("[{\"cut\":200,\"used\":0}]");
		saved.setCutdownAmount(0);
		saved.setIsOrdered(Boolean.FALSE);
		when(userBargainsMapper.selectOne(any())).thenReturn(null, saved);

		var out = service.createUserBargain(11L, 7L, "app", "wxa", "88", Locale.SIMPLIFIED_CHINESE);

		assertThat(out.get("item_name")).isEqualTo("砍价商品");
		assertThat(out.get("company_id")).isEqualTo("11");
		verify(userBargainsMapper).insert(any(UserBargains.class));
	}
}
