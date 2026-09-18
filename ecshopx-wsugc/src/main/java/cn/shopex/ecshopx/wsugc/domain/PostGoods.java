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

package cn.shopex.ecshopx.wsugc.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 笔记 */
@Data
@MpTable(value = "wsugc_post_goods", comment = "笔记", indexes = {@MpIndex(name = "idx_post_id", columns = {"post_id"})})
public class PostGoods {

    @MpId(value = "post_goods_id", type = IdType.AUTO, columnType = "bigint")
    private Long postGoodsId;

    /** 笔记id */
    @MpField(value = "post_id", columnType = "bigint", comment = "笔记id")
    private Long postId;

    /** 视频id */
    @MpField(value = "goods_id", columnType = "bigint", comment = "视频id")
    private Long goodsId;
}
