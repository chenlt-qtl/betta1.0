package com.betta.eng.service;

import java.util.List;
import com.betta.eng.domain.mall.MallExchangeRecord;
import com.betta.eng.domain.mall.MallProduct;
import com.betta.eng.domain.vo.MallExchangeResultVo;

/** 金币商城业务接口。 */
public interface IMallService
{
    /** 查询商品列表。 */
    List<MallProduct> selectProductList(MallProduct product);

    /** 查询商品详情。 */
    MallProduct selectProductById(Long id);

    /** 新增商品并返回含主键实体。 */
    MallProduct insertProduct(MallProduct product);

    /** 更新商品并返回影响行数。 */
    int updateProduct(MallProduct product);

    /** 批量删除商品并返回影响行数。 */
    int deleteProductByIds(Long[] ids);

    /** 查询当前用户金币余额，钱包不存在时返回零。 */
    long getCurrentCoinBalance();

    /** 使用当前用户金币兑换一件商品。 */
    MallExchangeResultVo exchange(Long productId);

    /** 查询当前用户兑换记录。 */
    List<MallExchangeRecord> selectCurrentExchangeList(MallExchangeRecord record);

    /** 管理端查询全部兑换记录。 */
    List<MallExchangeRecord> selectExchangeList(MallExchangeRecord record);
}
