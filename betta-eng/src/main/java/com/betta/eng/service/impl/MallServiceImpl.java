package com.betta.eng.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.betta.system.domain.SysConfig;
import com.betta.system.service.ISysConfigService;

/** 金币商城业务实现，负责人民币定价、实时换算和兑换事务。 */
@Service
public class MallServiceImpl implements IMallService
{
    private static final String EXCHANGE_RATE_KEY = "mall.rmbCoinRate";
    private static final int MAX_BATCH_SIZE = 50;
    private static final BigDecimal MAX_YUAN_PRICE = new BigDecimal("9999999999.99");
    private static final BigDecimal MAX_EXCHANGE_RATE = new BigDecimal("922337203");

    private final MallProductMapper productMapper;
    private final MallExchangeRecordMapper exchangeRecordMapper;
    private final EngCoinWalletMapper coinWalletMapper;
    private final ISysConfigService configService;

    /** 创建商城服务，参数依次负责商品、兑换记录、金币钱包和系统参数访问。 */
    public MallServiceImpl(MallProductMapper productMapper, MallExchangeRecordMapper exchangeRecordMapper,
            EngCoinWalletMapper coinWalletMapper, ISysConfigService configService)
    {
        this.productMapper = productMapper;
        this.exchangeRecordMapper = exchangeRecordMapper;
        this.coinWalletMapper = coinWalletMapper;
        this.configService = configService;
    }

    @Override
    public List<MallProduct> selectProductList(MallProduct product)
    {
        List<MallProduct> products = productMapper.selectProductList(product == null ? new MallProduct() : product);
        BigDecimal rate = getExchangeRate();
        products.forEach(item -> fillCoinPrice(item, rate));
        return products;
    }

    @Override
    public MallProduct selectProductById(Long id)
    {
        MallProduct product = id == null ? null : productMapper.selectProductById(id);
        if (product != null)
        {
            fillCoinPrice(product, getExchangeRate());
        }
        return product;
    }

    @Override
    public MallProduct insertProduct(MallProduct product)
    {
        validateProduct(product, false, false);
        product.setCreateBy(SecurityUtils.getUsername());
        fillCoinPrice(product, getExchangeRate());
        productMapper.insertProduct(product);
        return product;
    }

    @Override
    @Transactional
    public int insertProductBatch(List<MallProduct> products)
    {
        if (products == null || products.isEmpty())
        {
            throw new ServiceException("批量商品不能为空");
        }
        if (products.size() > MAX_BATCH_SIZE)
        {
            throw new ServiceException("单次最多批量新增50个商品");
        }
        BigDecimal rate = getExchangeRate();
        String username = SecurityUtils.getUsername();
        for (MallProduct product : products)
        {
            validateProduct(product, false, true);
            product.setCreateBy(username);
            fillCoinPrice(product, rate);
        }
        int rows = productMapper.insertProductBatch(products);
        if (rows != products.size())
        {
            throw new ServiceException("批量新增商品失败");
        }
        return rows;
    }

    @Override
    public int updateProduct(MallProduct product)
    {
        validateProduct(product, true, false);
        product.setUpdateBy(SecurityUtils.getUsername());
        fillCoinPrice(product, getExchangeRate());
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
    public MallExchangeResultVo exchange(Long productId, Long expectedCoinPrice)
    {
        if (productId == null)
        {
            throw new ServiceException("商品主键不能为空");
        }
        if (expectedCoinPrice == null || expectedCoinPrice <= 0)
        {
            throw new ServiceException("确认金币价格不能为空");
        }
        // 先锁定商品，再按当前汇率计算价格，确保校验、扣减和快照使用同一商品版本。
        MallProduct product = productMapper.selectProductByIdForUpdate(productId);
        if (product == null)
        {
            throw new ServiceException("商品不存在或已删除");
        }
        long currentCoinPrice = calculateCoinPrice(product.getYuanPrice(), getExchangeRate());
        if (expectedCoinPrice.longValue() != currentCoinPrice)
        {
            throw new ServiceException("商品价格已变化，请刷新后重新确认");
        }
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        if (coinWalletMapper.decreaseCoinBalance(userId, currentCoinPrice, username) == 0)
        {
            // 钱包不存在与余额不足都不得创建记录，避免暴露内部钱包状态差异。
            throw new ServiceException("金币余额不足");
        }
        MallExchangeRecord record = new MallExchangeRecord();
        record.setUserId(userId);
        record.setProductId(product.getId());
        record.setProductName(product.getName());
        record.setProductImages(product.getImages());
        record.setCoinPrice(currentCoinPrice);
        record.setCreateBy(username);
        if (exchangeRecordMapper.insertExchangeRecord(record) != 1)
        {
            // 通过运行时业务异常触发整个事务回滚，金币扣减不会单独提交。
            throw new ServiceException("兑换记录保存失败");
        }
        return new MallExchangeResultVo(record.getId(), currentCoinPrice,
                coinWalletMapper.selectCoinBalance(userId));
    }

    @Override
    public BigDecimal getExchangeRate()
    {
        String value = configService.selectConfigByKey(EXCHANGE_RATE_KEY);
        if (isBlank(value))
        {
            return BigDecimal.ONE;
        }
        try
        {
            return validateRate(new BigDecimal(value));
        }
        catch (NumberFormatException exception)
        {
            throw new ServiceException("商城汇率配置无效");
        }
    }

    @Override
    public int updateExchangeRate(BigDecimal rate)
    {
        BigDecimal validRate = validateRate(rate);
        BigDecimal maxYuanPrice = productMapper.selectMaxYuanPrice();
        if (maxYuanPrice != null)
        {
            try
            {
                calculateCoinPrice(maxYuanPrice, validRate);
            }
            catch (ServiceException exception)
            {
                throw new ServiceException("人民币兑金币汇率过大，现有商品换算后超出允许范围");
            }
        }
        SysConfig condition = new SysConfig();
        condition.setConfigKey(EXCHANGE_RATE_KEY);
        SysConfig config = configService.selectConfigList(condition).stream()
                .filter(item -> EXCHANGE_RATE_KEY.equals(item.getConfigKey()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("商城汇率配置不存在"));
        config.setConfigValue(validRate.stripTrailingZeros().toPlainString());
        config.setUpdateBy(SecurityUtils.getUsername());
        int rows = configService.updateConfig(config);
        if (rows == 0)
        {
            throw new ServiceException("商城汇率保存失败");
        }
        return rows;
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

    /** 校验商品字段并将空白名称、描述规范化为空字符串。 */
    private void validateProduct(MallProduct product, boolean requireId, boolean batchItem)
    {
        if (product == null || requireId && product.getId() == null)
        {
            throw new ServiceException(requireId ? "商品主键不能为空" : "商品不能为空");
        }
        if (isBlank(product.getName()))
        {
            product.setName("");
        }
        if (isBlank(product.getDescription()))
        {
            product.setDescription("");
        }
        BigDecimal yuanPrice = product.getYuanPrice();
        if (yuanPrice == null || yuanPrice.compareTo(BigDecimal.ZERO) <= 0
                || yuanPrice.stripTrailingZeros().scale() > 2 || yuanPrice.compareTo(MAX_YUAN_PRICE) > 0)
        {
            throw new ServiceException("商品人民币价格必须大于0且最多两位小数");
        }
        String[] images = isBlank(product.getImages()) ? new String[0] : product.getImages().split(",", -1);
        if (images.length < 1 || images.length > 5)
        {
            throw new ServiceException("商品图片数量必须为1至5张");
        }
        if (batchItem && images.length != 1)
        {
            throw new ServiceException("批量新增商品每项只能包含1张图片");
        }
        for (String image : images)
        {
            if (isBlank(image))
            {
                throw new ServiceException("商品图片地址不能为空");
            }
        }
    }

    /** 按当前汇率计算整数金币价，四舍五入且最低为一金币。 */
    private long calculateCoinPrice(BigDecimal yuanPrice, BigDecimal rate)
    {
        if (yuanPrice == null || yuanPrice.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("商品人民币价格无效");
        }
        try
        {
            long result = yuanPrice.multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact();
            return Math.max(1L, result);
        }
        catch (ArithmeticException exception)
        {
            throw new ServiceException("商品金币价格超出允许范围");
        }
    }

    /** 将实时计算的金币价格写入接口实体。 */
    private void fillCoinPrice(MallProduct product, BigDecimal rate)
    {
        product.setCoinPrice(calculateCoinPrice(product.getYuanPrice(), rate));
    }

    /** 校验汇率必须为正数且不超过全局安全上限。 */
    private BigDecimal validateRate(BigDecimal rate)
    {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("人民币兑金币汇率必须大于0");
        }
        if (rate.compareTo(MAX_EXCHANGE_RATE) > 0)
        {
            throw new ServiceException("人民币兑金币汇率不能超过922337203");
        }
        return rate;
    }

    /** 判断文本是否为空白。 */
    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
