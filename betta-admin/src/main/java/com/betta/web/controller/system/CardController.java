package com.betta.web.controller.system;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.betta.common.annotation.Log;
import com.betta.common.core.controller.BaseController;
import com.betta.common.core.domain.AjaxResult;
import com.betta.common.enums.BusinessType;
import com.betta.system.domain.CardAccount;
import com.betta.system.domain.CardHistory;
import com.betta.system.domain.CardItem;
import com.betta.system.service.ICardAccountService;
import com.betta.system.service.ICardHistoryService;
import com.betta.system.service.ICardItemService;

/**
 * 加扣卡 业务处理
 *
 * @author betta
 */
@RestController
@RequestMapping("/system/card")
public class CardController extends BaseController {

    @Autowired
    private ICardAccountService cardAccountService;

    @Autowired
    private ICardHistoryService cardHistoryService;

    @Autowired
    private ICardItemService cardItemService;

    /**
     * 加扣卡请求
     */
    @PreAuthorize("@ss.hasPermi('system:card:operate')")
    @Log(title = "加扣卡", businessType = BusinessType.UPDATE)
    @PostMapping("/operate")
    @Transactional
    public AjaxResult operate(@Validated @RequestBody CardOperateRequest request) {
        // 查询或创建账户
        CardAccount account = cardAccountService.selectCardAccountByName(request.getName());
        if (account == null) {
            account = new CardAccount();
            account.setName(request.getName());
            account.setBalance(0);
            account.setCreateBy(getUsername());
            cardAccountService.insertCardAccount(account);
        }

        // 计算变动值
        Integer changeValue = request.getChangeValue();
        if (request.getItemId() != null) {
            CardItem item = cardItemService.selectCardItemById(request.getItemId());
            if (item != null) {
                changeValue = item.getValue();
            }
        }

        if (changeValue == null) {
            return error("变动值不能为空");
        }

        // 更新余额
        account.setBalance(account.getBalance() + changeValue);
        account.setUpdateBy(getUsername());
        cardAccountService.updateCardAccount(account);

        // 记录历史
        CardHistory history = new CardHistory();
        history.setAccountId(account.getId());
        history.setChangeValue(changeValue);
        history.setRemainValue(account.getBalance());
        history.setContent(request.getContent());
        history.setCreateBy(getUsername());
        cardHistoryService.insertCardHistory(history);

        return success("操作成功");
    }

    /**
     * 批量加扣卡
     */
    @PreAuthorize("@ss.hasPermi('system:card:operate')")
    @Log(title = "批量加扣卡", businessType = BusinessType.UPDATE)
    @PostMapping("/batchOperate")
    @Transactional
    public AjaxResult batchOperate(@Validated @RequestBody CardBatchOperateRequest request) {
        List<String> names = request.getNames();
        if (names == null || names.isEmpty()) {
            return error("请选择人员");
        }

        // 计算变动值
        Integer changeValue = request.getChangeValue();
        if (request.getItemId() != null) {
            CardItem item = cardItemService.selectCardItemById(request.getItemId());
            if (item != null) {
                changeValue = item.getValue();
            }
        }

        if (changeValue == null) {
            return error("变动值不能为空");
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        for (String name : names) {
            try {
                // 查询或创建账户
                CardAccount account = cardAccountService.selectCardAccountByName(name);
                if (account == null) {
                    account = new CardAccount();
                    account.setName(name);
                    account.setBalance(0);
                    account.setCreateBy(getUsername());
                    cardAccountService.insertCardAccount(account);
                }


                // 更新余额
                account.setBalance(account.getBalance() + changeValue);
                account.setUpdateBy(getUsername());
                cardAccountService.updateCardAccount(account);

                // 记录历史
                CardHistory history = new CardHistory();
                history.setAccountId(account.getId());
                history.setChangeValue(changeValue);
                history.setRemainValue(account.getBalance());
                history.setContent(request.getContent());
                history.setCreateBy(getUsername());
                cardHistoryService.insertCardHistory(history);

                successCount++;
            } catch (Exception e) {
                failCount++;
                errorMsg.append(name).append("操作失败;");
            }
        }

        if (failCount > 0) {
            return AjaxResult.warn("成功" + successCount + "人，失败" + failCount + "人: " + errorMsg);
        }
        return success("批量操作成功，共" + successCount + "人");
    }

    /**
     * 加扣卡请求
     */
    public static class CardOperateRequest {
        private String name;
        private Long itemId;
        private Integer changeValue;
        private String content;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Integer getChangeValue() {
            return changeValue;
        }

        public void setChangeValue(Integer changeValue) {
            this.changeValue = changeValue;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * 批量加扣卡请求
     */
    public static class CardBatchOperateRequest {
        private List<String> names;
        private Long itemId;
        private Integer changeValue;
        private String content;

        public List<String> getNames() {
            return names;
        }

        public void setNames(List<String> names) {
            this.names = names;
        }

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Integer getChangeValue() {
            return changeValue;
        }

        public void setChangeValue(Integer changeValue) {
            this.changeValue = changeValue;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
