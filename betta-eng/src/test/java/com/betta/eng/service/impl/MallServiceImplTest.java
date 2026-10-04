package com.betta.eng.service.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.common.exception.ServiceException;
import com.betta.eng.domain.mall.MallExchangeRecord;
import com.betta.eng.domain.mall.MallProduct;
import com.betta.eng.domain.vo.MallExchangeResultVo;
import com.betta.eng.mapper.EngCoinWalletMapper;
import com.betta.eng.mapper.MallExchangeRecordMapper;
import com.betta.eng.mapper.MallProductMapper;

/** 商城服务独立回归入口，无需启动 Spring 或连接数据库。 */
public class MallServiceImplTest
{
    /** 运行商城兑换及商品边界回归。 */
    public static void main(String[] args) throws Exception
    {
        MallServiceImplTest test = new MallServiceImplTest();
        test.shouldExchangeAndPersistSnapshot();
        test.shouldAllowBalanceExactlyEqualToPrice();
        test.shouldRejectInsufficientBalanceAndMissingWallet();
        test.shouldRejectMissingProduct();
        test.shouldRejectRecordFailureAndDeclareTransaction();
        test.shouldValidateProductBoundaries();
    }

    /** 验证正常兑换使用商品快照并返回记录、消耗和最新余额。 */
    private void shouldExchangeAndPersistSnapshot()
    {
        TestProductMapper products = new TestProductMapper(product(9L, 30L));
        TestRecordMapper records = new TestRecordMapper();
        TestWalletMapper wallet = new TestWalletMapper(100L);
        MallServiceImpl service = new MallServiceImpl(products, records, wallet);
        setTestLoginUser();
        try
        {
            MallExchangeResultVo result = service.exchange(9L);
            assertEquals(1L, result.getRecordId(), "兑换应返回新增记录主键");
            assertEquals(30L, result.getCoinCost(), "兑换应返回数据库中的商品价格");
            assertEquals(70L, result.getCoinBalance(), "兑换应返回扣减后的余额");
            MallExchangeRecord record = records.records.get(0);
            assertEquals("测试商品", record.getProductName(), "记录应保存商品名称快照");
            assertEquals("/a.png,/b.png", record.getProductImages(), "记录应保存商品图片快照");
            assertEquals(30L, record.getCoinPrice(), "记录应保存商品价格快照");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 验证余额恰好等于价格时可以兑换且余额归零。 */
    private void shouldAllowBalanceExactlyEqualToPrice()
    {
        TestWalletMapper wallet = new TestWalletMapper(30L);
        MallServiceImpl service = new MallServiceImpl(new TestProductMapper(product(9L, 30L)),
                new TestRecordMapper(), wallet);
        setTestLoginUser();
        try
        {
            assertEquals(0L, service.exchange(9L).getCoinBalance(), "余额等于价格时不得误判为不足");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 验证余额不足和钱包不存在都不会写入兑换记录。 */
    private void shouldRejectInsufficientBalanceAndMissingWallet()
    {
        for (long balance : List.of(29L, 0L))
        {
            TestRecordMapper records = new TestRecordMapper();
            MallServiceImpl service = new MallServiceImpl(new TestProductMapper(product(9L, 30L)), records,
                    new TestWalletMapper(balance));
            setTestLoginUser();
            try
            {
                assertServiceException(() -> service.exchange(9L), "金币余额不足");
                assertEquals(0, records.records.size(), "扣减失败时不得写入兑换记录");
            }
            finally
            {
                SecurityContextHolder.clearContext();
            }
        }
    }

    /** 验证商品不存在时不会扣减金币。 */
    private void shouldRejectMissingProduct()
    {
        TestWalletMapper wallet = new TestWalletMapper(100L);
        MallServiceImpl service = new MallServiceImpl(new TestProductMapper(null), new TestRecordMapper(), wallet);
        assertServiceException(() -> service.exchange(9L), "商品不存在或已删除");
        assertEquals(100L, wallet.balance, "商品不存在时不得扣减金币");
    }

    /** 验证记录写入失败会抛出运行时异常，且兑换方法声明事务以回滚此前扣减。 */
    private void shouldRejectRecordFailureAndDeclareTransaction() throws Exception
    {
        TestRecordMapper records = new TestRecordMapper();
        records.insertResult = 0;
        MallServiceImpl service = new MallServiceImpl(new TestProductMapper(product(9L, 30L)), records,
                new TestWalletMapper(100L));
        setTestLoginUser();
        try
        {
            assertServiceException(() -> service.exchange(9L), "兑换记录保存失败");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
        Method exchange = MallServiceImpl.class.getMethod("exchange", Long.class);
        assertTrue(exchange.isAnnotationPresent(Transactional.class), "兑换方法必须由 Spring 事务代理统一回滚");
    }

    /** 验证名称、可选描述、图片数量和价格边界。 */
    private void shouldValidateProductBoundaries()
    {
        TestProductMapper products = new TestProductMapper(null);
        MallServiceImpl service = new MallServiceImpl(products, new TestRecordMapper(), new TestWalletMapper(0L));
        setTestLoginUser();
        try
        {
            MallProduct valid = product(null, 1L);
            service.insertProduct(valid);
            assertEquals(1L, valid.getId(), "合法商品应写入并回填主键");

            MallProduct noName = product(null, 1L);
            noName.setName(" ");
            assertServiceException(() -> service.insertProduct(noName), "商品名称不能为空");
            MallProduct noDescription = product(null, 1L);
            noDescription.setDescription(null);
            service.insertProduct(noDescription);
            assertEquals("", noDescription.getDescription(), "新增商品的空描述应规范化为空字符串");
            assertEquals("", products.lastInsertedDescription, "新增商品进入 Mapper 前应完成描述规范化");
            MallProduct blankDescription = product(2L, 1L);
            blankDescription.setDescription("  ");
            service.updateProduct(blankDescription);
            assertEquals("", blankDescription.getDescription(), "更新商品的空白描述应规范化为空字符串");
            assertEquals("", products.lastUpdatedDescription, "更新商品进入 Mapper 前应完成描述规范化");
            MallProduct zeroPrice = product(null, 0L);
            assertServiceException(() -> service.insertProduct(zeroPrice), "商品价格必须为正整数");
            MallProduct noImages = product(null, 1L);
            noImages.setImages("");
            assertServiceException(() -> service.insertProduct(noImages), "商品图片数量必须为1至5张");
            MallProduct tooManyImages = product(null, 1L);
            tooManyImages.setImages("1,2,3,4,5,6");
            assertServiceException(() -> service.insertProduct(tooManyImages), "商品图片数量必须为1至5张");
            MallProduct emptyImage = product(null, 1L);
            emptyImage.setImages("1,,3");
            assertServiceException(() -> service.insertProduct(emptyImage), "商品图片地址不能为空");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 创建测试商品。 */
    private static MallProduct product(Long id, Long price)
    {
        MallProduct product = new MallProduct();
        product.setId(id);
        product.setName("测试商品");
        product.setImages("/a.png,/b.png");
        product.setDescription("# 商品描述");
        product.setCoinPrice(price);
        return product;
    }

    /** 设置固定登录用户。 */
    private static void setTestLoginUser()
    {
        SysUser user = new SysUser();
        user.setUserName("tester");
        LoginUser loginUser = new LoginUser(20L, 30L, user, Set.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                loginUser, null, loginUser.getAuthorities()));
    }

    /** 断言业务异常消息。 */
    private static void assertServiceException(Action action, String message)
    {
        try
        {
            action.run();
            throw new AssertionError("应抛出业务异常: " + message);
        }
        catch (ServiceException exception)
        {
            assertEquals(message, exception.getMessage(), "业务异常消息不一致");
        }
    }

    /** 断言值相等。 */
    private static void assertEquals(Object expected, Object actual, String message)
    {
        if (expected == null ? actual != null : !expected.equals(actual))
        {
            throw new AssertionError(message + "，expected=" + expected + "，actual=" + actual);
        }
    }

    /** 断言条件成立。 */
    private static void assertTrue(boolean condition, String message)
    {
        if (!condition)
        {
            throw new AssertionError(message);
        }
    }

    /** 可抛业务异常的测试动作。 */
    @FunctionalInterface
    private interface Action
    {
        void run();
    }

    /** 商品 Mapper 测试替身。 */
    private static final class TestProductMapper implements MallProductMapper
    {
        private final MallProduct lockedProduct;
        private String lastInsertedDescription;
        private String lastUpdatedDescription;

        private TestProductMapper(MallProduct lockedProduct)
        {
            this.lockedProduct = lockedProduct;
        }

        @Override
        public List<MallProduct> selectProductList(MallProduct product) { return List.of(); }
        @Override
        public MallProduct selectProductById(Long id) { return lockedProduct; }
        @Override
        public MallProduct selectProductByIdForUpdate(Long id) { return lockedProduct; }
        @Override
        public int insertProduct(MallProduct product)
        {
            lastInsertedDescription = product.getDescription();
            product.setId(1L);
            return 1;
        }
        @Override
        public int updateProduct(MallProduct product)
        {
            lastUpdatedDescription = product.getDescription();
            return 1;
        }
        @Override
        public int deleteProductByIds(Long[] ids) { return ids.length; }
    }

    /** 兑换记录 Mapper 测试替身。 */
    private static final class TestRecordMapper implements MallExchangeRecordMapper
    {
        private final List<MallExchangeRecord> records = new ArrayList<>();
        private int insertResult = 1;

        @Override
        public int insertExchangeRecord(MallExchangeRecord record)
        {
            if (insertResult == 1)
            {
                record.setId(1L);
                records.add(record);
            }
            return insertResult;
        }

        @Override
        public List<MallExchangeRecord> selectExchangeRecordList(MallExchangeRecord record) { return records; }
    }

    /** 金币钱包 Mapper 测试替身，模拟数据库条件扣减。 */
    private static final class TestWalletMapper implements EngCoinWalletMapper
    {
        private long balance;

        private TestWalletMapper(long balance)
        {
            this.balance = balance;
        }

        @Override
        public long selectCoinBalance(Long userId) { return balance; }
        @Override
        public int increaseCoinBalance(Long userId, long coinReward, String username)
        {
            balance += coinReward;
            return 1;
        }
        @Override
        public int decreaseCoinBalance(Long userId, long coinCost, String username)
        {
            if (balance < coinCost)
            {
                return 0;
            }
            balance -= coinCost;
            return 1;
        }
    }
}
