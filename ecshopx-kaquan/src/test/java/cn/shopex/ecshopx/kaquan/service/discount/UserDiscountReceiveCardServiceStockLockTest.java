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

package cn.shopex.ecshopx.kaquan.service.discount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.RelItems;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.salesperson.service.SalespersonTaskCouponCompleteService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmDiscountCardSendPort;
import cn.shopex.ecshopx.wechat.service.WxaTemplateMsgCouponNotifyService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** ECX-10897：领券须先锁住卡券，库存已满时不插入。 */
class UserDiscountReceiveCardServiceStockLockTest {

	private UserDiscountMapper userDiscountMapper;
	private DiscountCardsMapper discountCardsMapper;
	private RelItemsMapper relItemsMapper;
	private DiscountCardsRowMapperService rowMapper;
	private MemberAccountService memberAccountService;
	private VipGradeUserVipGradeGetService vipGradeService;
	private DmCrmDiscountCardSendPort dmCrmDiscountCardSendPort;
	private UserDiscountReceiveCardService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, DiscountCards.class);
		TableInfoHelper.initTableInfo(assistant, RelItems.class);
		TableInfoHelper.initTableInfo(assistant, UserDiscount.class);
	}

	@BeforeEach
	void setUp() {
		userDiscountMapper = mock(UserDiscountMapper.class);
		discountCardsMapper = mock(DiscountCardsMapper.class);
		relItemsMapper = mock(RelItemsMapper.class);
		rowMapper = mock(DiscountCardsRowMapperService.class);
		memberAccountService = mock(MemberAccountService.class);
		vipGradeService = mock(VipGradeUserVipGradeGetService.class);
		dmCrmDiscountCardSendPort = mock(DmCrmDiscountCardSendPort.class);
		service = new UserDiscountReceiveCardService(
				userDiscountMapper,
				discountCardsMapper,
				relItemsMapper,
				memberAccountService,
				vipGradeService,
				mock(SalespersonTaskCouponCompleteService.class),
				rowMapper,
				new ObjectMapper(),
				dmCrmDiscountCardSendPort,
				mock(WxaTemplateMsgCouponNotifyService.class));
	}

	@Test
	void receiveCard_locksCardAndRejectsOutOfStockWithoutInsert() throws Exception {
		DiscountCards card = new DiscountCards();
		card.setCardId(9L);
		card.setCompanyId(100L);
		card.setQuantity(1);
		when(discountCardsMapper.selectOne(any())).thenReturn(card);
		when(userDiscountMapper.countIssuedGroupByCardId(100L, List.of(9L))).thenReturn(List.of(agg(9L, 1L)));

		assertThatThrownBy(() -> service.receiveCard(100L, 5L, "13800000000", 9L, 0L, ""))
				.isInstanceOf(ResourceException.class)
				.hasMessage("优惠券已领完");

		ArgumentCaptor<LambdaQueryWrapper<DiscountCards>> cardQuery = cardQueryCaptor();
		verify(discountCardsMapper).selectOne(cardQuery.capture());
		assertThat(cardQuery.getValue().getSqlSegment()).contains("FOR UPDATE");
		verify(userDiscountMapper, never()).insert(any(UserDiscount.class));
		assertReadCommitted("receiveCard", long.class, long.class, String.class, Long.class, long.class, String.class);
		assertReadCommitted(
				"receiveCard", long.class, long.class, String.class, Long.class, long.class, String.class, String.class);
		assertReadCommitted(
				"receiveCard",
				long.class,
				long.class,
				String.class,
				Long.class,
				long.class,
				String.class,
				String.class,
				String.class);
	}

	@Test
	void receiveCard_rejectsUserLimitWithoutInsert() {
		DiscountCards card = card(10);
		card.setKqStatus(0);
		when(discountCardsMapper.selectOne(any())).thenReturn(card);
		when(userDiscountMapper.countIssuedGroupByCardId(100L, List.of(9L))).thenReturn(List.of(agg(9L, 0L)));
		when(userDiscountMapper.countIssuedGroupByCardIdForUser(100L, 5L, List.of(9L)))
				.thenReturn(List.of(agg(9L, 1L)));
		when(rowMapper.toSnakeCaseMap(any())).thenReturn(cardInfo(1));
		when(relItemsMapper.selectList(any())).thenReturn(List.of());
		when(memberAccountService.getMemberInfo(5L, 100L)).thenReturn(Map.of());
		when(vipGradeService.userVipGradeGet(100L, 5L, false)).thenReturn(Map.of());

		assertThatThrownBy(() -> service.receiveCard(100L, 5L, "13800000000", 9L, 0L, ""))
				.isInstanceOf(ResourceException.class)
				.hasMessage("已超过个人限领数量");

		verify(userDiscountMapper, never()).insert(any(UserDiscount.class));
	}

	@Test
	void receiveCard_insertsWhenStockAndUserLimitRemain() {
		DiscountCards card = card(10);
		card.setKqStatus(0);
		when(discountCardsMapper.selectOne(any())).thenReturn(card);
		when(userDiscountMapper.countIssuedGroupByCardId(100L, List.of(9L))).thenReturn(List.of(agg(9L, 0L)));
		when(userDiscountMapper.countIssuedGroupByCardIdForUser(100L, 5L, List.of(9L)))
				.thenReturn(List.of(agg(9L, 0L)));
		when(rowMapper.toSnakeCaseMap(any())).thenReturn(cardInfo(1));
		when(relItemsMapper.selectList(any())).thenReturn(List.of());
		when(memberAccountService.getMemberInfo(5L, 100L)).thenReturn(Map.of());
		when(vipGradeService.userVipGradeGet(100L, 5L, false)).thenReturn(Map.of());
		when(dmCrmDiscountCardSendPort.isOpen(100L)).thenReturn(false);
		when(userDiscountMapper.selectCount(any())).thenReturn(0L);
		when(userDiscountMapper.selectOne(any())).thenReturn(null);
		when(userDiscountMapper.insert(any(UserDiscount.class))).thenReturn(1);

		Map<String, Object> result = service.receiveCard(100L, 5L, "13800000000", 9L, 0L, "");

		assertThat(result.get("code")).isNotNull();
		ArgumentCaptor<UserDiscount> inserted = ArgumentCaptor.forClass(UserDiscount.class);
		verify(userDiscountMapper).insert(inserted.capture());
		assertThat(inserted.getValue().getCardId()).isEqualTo(9L);
		assertThat(inserted.getValue().getUserId()).isEqualTo(5L);
		assertThat(inserted.getValue().getCompanyId()).isEqualTo(100L);
	}

	private static DiscountCards card(int quantity) {
		DiscountCards card = new DiscountCards();
		card.setCardId(9L);
		card.setCompanyId(100L);
		card.setQuantity(quantity);
		return card;
	}

	private static Map<String, Object> cardInfo(int getLimit) {
		Map<String, Object> cardInfo = new LinkedHashMap<>();
		cardInfo.put("get_limit", getLimit);
		cardInfo.put("card_id", 9L);
		cardInfo.put("card_type", "cash");
		return cardInfo;
	}

	private static void assertReadCommitted(String name, Class<?>... parameterTypes) throws Exception {
		Transactional transactional =
				UserDiscountReceiveCardService.class.getMethod(name, parameterTypes).getAnnotation(Transactional.class);
		assertThat(transactional.isolation()).isEqualTo(Isolation.READ_COMMITTED);
	}

	private static UserDiscountCardAggRow agg(long cardId, long num) {
		UserDiscountCardAggRow row = new UserDiscountCardAggRow();
		row.setCardId(cardId);
		row.setNum(num);
		return row;
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<LambdaQueryWrapper<DiscountCards>> cardQueryCaptor() {
		return ArgumentCaptor.forClass(LambdaQueryWrapper.class);
	}
}
