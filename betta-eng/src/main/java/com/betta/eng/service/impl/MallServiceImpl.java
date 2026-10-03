package com.betta.eng.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.SecurityUtils;
import com.betta.eng.domain.mall.MallExchangeRecord;
import com.betta.eng.domain.mall.MallProduct;
import com.betta.eng.domain.vo.MallExchangeResultVo;
import com.betta.eng.mapper.EngCoinWalletMapper;
import com.betta.eng.mapper.MallExchangeRecordMapper;
import com.betta.eng.mapper.MallProductMapper;
import com.betta.eng.service.IMallService;

/** 金币商城业务实现，负责商品边界校验与兑换事务。 */
@Service
public class MallServiceImpl implements IMallService
{
    private final MallProductMapper productMapper;
    private final MallExchangeRecordMapper exchangeRecordMapper;
    private final EngCoinWalletMapper coinWalletMapper;

    /** 创建商城服务，参数依次负责商品、兑换记录和金币钱包访问。 */
    public MallServiceImpl(MallProductMapper productMapper, MallExchangeRecordMapper exchangeRecordMapper,
            EngCoinWalletMapper coinWalletMapper)
    {
        this.productMapper = productMapper;
        this.exchangeRecordMapper = exchangeRecordMapper;
        this.coinWalletMapper = coinWalletMapper;
    }

    @Override
    public List<MallProduct> selectProductList(MallProduct product)
    {
        return productMapper.selectProductList(product == null ? new MallProduct() : product);
    }

    @Override
    public MallProduct selectProductById(Long id)
    {
        return id == null ? null : productMapper.selectProductById(id);
    }

    @Override
    public MallProduct insertProduct(MallProduct product)
    {
        validateProduct(product, false);
        product.setCreateBy(SecurityUtils.getUsername());
        productMapper.insertProduct(product);
        return product;
    }

    @Override
    public int updateProduct(MallProduct product)
    {
        validateProduct(product, true);
        product.setUpdateBy(SecurityUtils.getUsername());
        int rows = productMapper.updateProduct(product);
        if (rows == 0)
        {
            throw new ServiceException("商品不存在");
        }
        return rows;
    }

    @Override
    public int deleteProductByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("商品主键不能为空");
        }
        return productMapper.deleteProductByIds(ids);
    }

    @Override
    public long getCurrentCoinBalance()
    {
        return coinWalletMapper.selectCoinBalance(SecurityUtils.getUserId());
    }

    @Override
    @Transactional
    public MallExchangeResultVo exchange(Long productId)
    {
        if (productId == null)
        {
            throw new ServiceException("商品主键不能为空");
        }
        // 先锁定商品再读取价格，确保本次扣减和快照使用同一个数据库版本。
        MallProduct product = productMapper.selectProductByIdForUpdate(productId);
        if (product == null)
        {
            throw new ServiceException("商品不存在或已删除");
        }
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        if (coinWalletMapper.decreaseCoinBalance(userId, product.getCoinPrice(), username) == 0)
        {
            // 钱包不存在与余额不足都不得创建记录，避免暴露内部钱包状态差异。
            throw new ServiceException("金币余额不足");
        }
        MallExchangeRecord record = new MallExchangeRecord();
        record.setUserId(userId);
        record.setProductId(product.getId());
        record.setProductName(product.getName());
        record.setProductImages(product.getImages());
        record.setCoinPrice(product.getCoinPrice());
        record.setCreateBy(username);
        if (exchangeRecordMapper.insertExchangeRecord(record) != 1)
        {
            // 通过运行时业务异常触发整个事务回滚，金币扣减不会单独提交。
            throw new ServiceException("兑换记录保存失败");
        }
        return new MallExchangeResultVo(record.getId(), product.getCoinPrice(),
                coinWalletMapper.selectCoinBalance(userId));
    }

    @Override
    public List<MallExchangeRecord> selectCurrentExchangeList(MallExchangeRecord record)
    {
        MallExchangeRecord condition = record == null ? new MallExchangeRecord() : record;
        // 本人记录查询强制覆盖客户端传入用户，避免越权读取他人兑换历史。
        condition.setUserId(SecurityUtils.getUserId());
        condition.setUserName(null);
        return exchangeRecordMapper.selectExchangeRecordList(condition);
    }

    @Override
    public List<MallExchangeRecord> selectExchangeList(MallExchangeRecord record)
    {
        return exchangeRecordMapper.selectExchangeRecordList(record == null ? new MallExchangeRecord() : record);
    }

    /** 校验商品必填项、图片数量和正整数价格；更新时还要求商品主键。 */
    private void validateProduct(MallProduct product, boolean requireId)
    {
        if (product == null || requireId && product.getId() == null)
        {
            throw new ServiceException(requireId ? "商品主键不能为空" : "商品不能为空");
        }
        if (isBlank(product.getName()))
        {
            throw new ServiceException("商品名称不能为空");
        }
        if (isBlank(product.getDescription()))
        {
            throw new ServiceException("商品描述不能为空");
        }
        if (product.getCoinPrice() == null || product.getCoinPrice() <= 0)
        {
            throw new ServiceException("商品价格必须为正整数");
        }
        String[] images = isBlank(product.getImages()) ? new String[0] : product.getImages().split(",", -1);
        if (images.length < 1 || images.length > 5)
        {
            throw new ServiceException("商品图片数量必须为1至5张");
        }
        for (String image : images)
        {
            if (isBlank(image))
            {
                throw new ServiceException("商品图片地址不能为空");
            }
        }
    }

    /** 判断文本是否为空白。 */
    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
