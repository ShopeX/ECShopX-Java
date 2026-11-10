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

package cn.shopex.ecshopx.goods.search.price;

/**
 * 搜索算价上下文中的会员档位（与 ES {@code member_prices} 桶选择对齐）。
 *
 * @param lvType {@code normal} | {@code vip} | {@code svip}；{@code normal} 使用 grade 桶，其余使用 vipGrade 桶
 */
public record SearchMemberTier(String lvType, long gradeId, int discount, String gradeName) {}
