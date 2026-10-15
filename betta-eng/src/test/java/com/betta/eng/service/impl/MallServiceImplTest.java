package com.betta.eng.service.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
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
import com.betta.system.domain.SysConfig;
import com.betta.system.service.ISysConfigService;

/** 商城服务独立回归入口，无需启动 Spring 或连接数据库。 */
public class MallServiceImplTest
{
    /** 运行商城定价、批量新增和兑换事务回归。 */
    public static void main(String[] args) throws Exception
    {
        MallServiceImplTest test = new MallServiceImplTest();
        test.shouldCalculateCurrentCoinPrice();
        test.shouldReadAndUpdateExchangeRate();
        test.shouldRejectRateOverflowForExistingProducts();
        test.shouldValidateProductAndBatchBoundaries();
        test.shouldExchangeAndPersistSnapshot();
        test.shouldRejectChangedPriceWithoutDeduction();
        test.shouldRejectExchangeFailuresAndDeclareTransactions();
    }

    /** 验证列表统一使用一次汇率，并覆盖四舍五入和最低一金币规则。 */
    private void shouldCalculateCurrentCoinPrice()
    {
        TestProductMapper products = new TestProductMapper(null);
        products.list = List.of(product(null, "1.25"), product(null, "0.01"));
        TestConfig config = new TestConfig("2");
        MallServiceImpl service = service(products, new TestRecordMapper(), new TestWalletMapper(0L), config);
        List<MallProduct> result = service.selectProductList(null);
        assertEquals(3L, result.get(0).getCoinPrice(), "2.5金币应四舍五入为3");
        assertEquals(1L, result.get(1).getCoinPrice(), "正价商品最低应为1金币");
        assertEquals(1, config.selectByKeyCount, "同一列表只应读取一次汇率");
    }

    /** 验证合法汇率可保存，缺失配置使用默认值，非法汇率被拒绝。 */
    private void shouldReadAndUpdateExchangeRate()
    {
        TestConfig config = new TestConfig("");
        MallServiceImpl service = service(new TestProductMapper(null), new TestRecordMapper(),
                new TestWalletMapper(0L), config);
        assertEquals(BigDecimal.ONE, service.getExchangeRate(), "缺失汇率应使用默认值1");
        setTestLoginUser();
        try
        {
            assertEquals(1, service.updateExchangeRate(new BigDecimal("2.500")), "合法汇率应保存成功");
            assertEquals("2.5", config.updatedValue, "保存汇率应去除无意义尾零");
            assertServiceException(() -> service.updateExchangeRate(BigDecimal.ZERO), "人民币兑金币汇率必须大于0");
            config.rate = "922337203";
            assertEquals(new BigDecimal("922337203"), service.getExchangeRate(), "最大安全汇率应合法");
            config.rate = "922337204";
            assertServiceException(service::getExchangeRate, "人民币兑金币汇率不能超过922337203");
            config.rate = "not-a-number";
            assertServiceException(service::getExchangeRate, "商城汇率配置无效");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 验证设置汇率时会结合数据库现有最大价格阻止 long 溢出。 */
    private void shouldRejectRateOverflowForExistingProducts()
    {
        TestProductMapper products = new TestProductMapper(null);
        products.maxYuanPrice = new BigDecimal("9223372036854775807.00");
        MallServiceImpl service = service(products, new TestRecordMapper(), new TestWalletMapper(0L),
                new TestConfig("1"));
        setTestLoginUser();
        try
        {
            assertServiceException(() -> service.updateExchangeRate(new BigDecimal("2")),
                    "人民币兑金币汇率过大，现有商品换算后超出允许范围");
            assertEquals(1, service.updateExchangeRate(BigDecimal.ONE), "现有最大旧价格在汇率1时应可安全换算");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 验证空名称、人民币价格、图片和批量数量边界，及批量事务声明。 */
    private void shouldValidateProductAndBatchBoundaries() throws Exception
    {
        TestProductMapper products = new TestProductMapper(null);
        MallServiceImpl service = service(products, new TestRecordMapper(), new TestWalletMapper(0L),
                new TestConfig("1"));
        setTestLoginUser();
        try
        {
            MallProduct blankName = product(null, "10.00");
            blankName.setName("  ");
            blankName.setDescription(null);
            service.insertProduct(blankName);
            assertEquals("", blankName.getName(), "空白名称应规范化为空字符串");
            assertEquals("", blankName.getDescription(), "空描述应规范化为空字符串");

            MallProduct tooPrecise = product(null, "1.001");
            assertServiceException(() -> service.insertProduct(tooPrecise), "商品人民币价格必须大于0且最多两位小数");
            MallProduct noImages = product(null, "1");
            noImages.setImages("");
            assertServiceException(() -> service.insertProduct(noImages), "商品图片数量必须为1至5张");
            MallProduct emptyImage = product(null, "1");
            emptyImage.setImages("1,,3");
            assertServiceException(() -> service.insertProduct(emptyImage), "商品图片地址不能为空");

            assertServiceException(() -> service.insertProductBatch(List.of()), "批量商品不能为空");
            List<MallProduct> tooMany = new ArrayList<>();
            for (int i = 0; i < 51; i++)
            {
                tooMany.add(product(null, "1"));
            }
            assertServiceException(() -> service.insertProductBatch(tooMany), "单次最多批量新增50个商品");
            MallProduct invalid = batchProduct("0");
            assertServiceException(() -> service.insertProductBatch(List.of(batchProduct("1"), invalid)),
                    "商品人民币价格必须大于0且最多两位小数");
            assertEquals(0, products.batchCalls, "任一商品非法时不得调用批量写入");
            assertServiceException(() -> service.insertProductBatch(List.of(product(null, "1"))),
                    "批量新增商品每项只能包含1张图片");
            assertEquals(2, service.insertProductBatch(List.of(batchProduct("1"), batchProduct("2"))),
                    "合法批次应一次写入全部商品");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
        Method batch = MallServiceImpl.class.getMethod("insertProductBatch", List.class);
        assertTrue(batch.isAnnotationPresent(Transactional.class), "批量新增必须声明事务");
    }

    /** 验证正常兑换按当前汇率扣减，并保存空名称及价格快照。 */
    private void shouldExchangeAndPersistSnapshot()
    {
        MallProduct product = product(9L, "15");
        product.setName("");
        TestRecordMapper records = new TestRecordMapper();
        TestWalletMapper wallet = new TestWalletMapper(100L);
        MallServiceImpl service = service(new TestProductMapper(product), records, wallet, new TestConfig("2"));
        setTestLoginUser();
        try
        {
            MallExchangeResultVo result = service.exchange(9L, 30L);
            assertEquals(30L, result.getCoinCost(), "兑换应使用当前汇率计算价格");
            assertEquals(70L, result.getCoinBalance(), "兑换应返回扣减后的余额");
            MallExchangeRecord record = records.records.get(0);
            assertEquals("", record.getProductName(), "空商品名应原样保存为空快照");
            assertEquals(30L, record.getCoinPrice(), "兑换记录应保存成交金币价快照");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    /** 验证确认后汇率变化会拒绝兑换，且不扣余额、不写记录。 */
    private void shouldRejectChangedPriceWithoutDeduction()
    {
        TestWalletMapper wallet = new TestWalletMapper(100L);
        TestRecordMapper records = new TestRecordMapper();
        MallServiceImpl service = service(new TestProductMapper(product(9L, "15")), records, wallet,
                new TestConfig("3"));
        assertServiceException(() -> service.exchange(9L, 30L), "商品价格已变化，请刷新后重新确认");
        assertEquals(100L, wallet.balance, "价格变化时不得扣减金币");
        assertEquals(0, records.records.size(), "价格变化时不得写兑换记录");
    }

    /** 验证余额、商品和记录失败边界，并确认兑换方法声明事务。 */
    private void shouldRejectExchangeFailuresAndDeclareTransactions() throws Exception
    {
        TestRecordMapper records = new TestRecordMapper();
        MallServiceImpl insufficient = service(new TestProductMapper(product(9L, "30")), records,
                new TestWalletMapper(29L), new TestConfig("1"));
        setTestLoginUser();
        try
        {
            assertServiceException(() -> insufficient.exchange(9L, 30L), "金币余额不足");
            assertEquals(0, records.records.size(), "扣减失败时不得写兑换记录");
            assertServiceException(() -> service(new TestProductMapper(null), new TestRecordMapper(),
                    new TestWalletMapper(100L), new TestConfig("1")).exchange(9L, 30L), "商品不存在或已删除");

            TestRecordMapper failedRecord = new TestRecordMapper();
            failedRecord.insertResult = 0;
            MallServiceImpl recordFailure = service(new TestProductMapper(product(9L, "30")), failedRecord,
                    new TestWalletMapper(100L), new TestConfig("1"));
            assertServiceException(() -> recordFailure.exchange(9L, 30L), "兑换记录保存失败");
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
        Method exchange = MallServiceImpl.class.getMethod("exchange", Long.class, Long.class);
        assertTrue(exchange.isAnnotationPresent(Transactional.class), "兑换方法必须声明事务");
    }

    /** 创建包含测试替身的商城服务。 */
    private static MallServiceImpl service(TestProductMapper products, TestRecordMapper records,
            TestWalletMapper wallet, TestConfig config)
    {
        return new MallServiceImpl(products, records, wallet, config.proxy());
    }

    /** 创建测试商品。 */
    private static MallProduct product(Long id, String yuanPrice)
    {
        MallProduct product = new MallProduct();
        product.setId(id);
        product.setName("测试商品");
        product.setImages("/a.png,/b.png");
        product.setDescription("# 商品描述");
        product.setYuanPrice(new BigDecimal(yuanPrice));
        return product;
    }

    /** 创建仅含一张图片的批量商品。 */
    private static MallProduct batchProduct(String yuanPrice)
    {
        MallProduct product = product(null, yuanPrice);
        product.setImages("/a.png");
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
        private List<MallProduct> list = List.of();
        private int batchCalls;
        private BigDecimal maxYuanPrice;

        private TestProductMapper(MallProduct lockedProduct)
        {
            this.lockedProduct = lockedProduct;
        }

        @Override
        public List<MallProduct> selectProductList(MallProduct product) { return list; }
        @Override
        public MallProduct selectProductById(Long id) { return lockedProduct; }
        @Override
        public MallProduct selectProductByIdForUpdate(Long id) { return lockedProduct; }
        @Override
        public BigDecimal selectMaxYuanPrice() { return maxYuanPrice; }
        @Override
        public int insertProduct(MallProduct product)
        {
            product.setId(1L);
            return 1;
        }
        @Override
        public int insertProductBatch(List<MallProduct> products)
        {
            batchCalls++;
            return products.size();
        }
        @Override
        public int updateProduct(MallProduct product) { return 1; }
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

    /** 系统参数服务测试替身，仅实现商城汇率所需行为。 */
    private static final class TestConfig
    {
        private String rate;
        private String updatedValue;
        private int selectByKeyCount;

        private TestConfig(String rate)
        {
            this.rate = rate;
        }

        private ISysConfigService proxy()
        {
            return (ISysConfigService) Proxy.newProxyInstance(ISysConfigService.class.getClassLoader(),
                    new Class<?>[] { ISysConfigService.class }, (proxy, method, args) -> {
                        if ("selectConfigByKey".equals(method.getName()))
                        {
                            selectByKeyCount++;
                            return rate;
                        }
                        if ("selectConfigList".equals(method.getName()))
                        {
                            SysConfig config = new SysConfig();
                            config.setConfigId(1L);
                            config.setConfigKey("mall.rmbCoinRate");
                            config.setConfigValue(rate);
                            return List.of(config);
                        }
                        if ("updateConfig".equals(method.getName()))
                        {
                            updatedValue = ((SysConfig) args[0]).getConfigValue();
                            rate = updatedValue;
                            return 1;
                        }
                        Class<?> type = method.getReturnType();
                        if (type == boolean.class)
                        {
                            return false;
                        }
                        if (type == int.class)
                        {
                            return 0;
                        }
                        return null;
                    });
        }
    }
}
