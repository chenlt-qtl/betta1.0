package com.betta.eng.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.betta.eng.domain.mall.MallProduct;

/** 商城商品数据访问接口。 */
public interface MallProductMapper
{
    /** 按条件查询商品列表。 */
    List<MallProduct> selectProductList(MallProduct product);

    /** 按主键查询商品。 */
    MallProduct selectProductById(Long id);

    /** 锁定并查询待兑换商品，保证兑换价格来自事务内数据库快照。 */
    MallProduct selectProductByIdForUpdate(Long id);

    /** 新增商品并回填主键。 */
    int insertProduct(MallProduct product);

    /** 更新商品。 */
    int updateProduct(MallProduct product);

    /** 批量硬删除商品，历史兑换记录不受影响。 */
    int deleteProductByIds(@Param("ids") Long[] ids);
}
