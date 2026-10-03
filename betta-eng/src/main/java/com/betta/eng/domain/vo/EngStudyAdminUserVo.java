package com.betta.eng.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

/**
 * 管理员积分汇总展示对象，同时承载用户名和昵称查询条件。
 */
@Data
public class EngStudyAdminUserVo
{
    /** 用户主键。 */
    private Long userId;
    /** 登录用户名。 */
    private String userName;
    /** 用户昵称。 */
    private String nickName;
    /** 历史累计积分，无学习记录时为零。 */
    private Long totalScore;
    /** 闯关提交次数，无学习记录时为零。 */
    private Long studyCount;
    /** 最近一次学习时间，无学习记录时为空。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastStudyTime;
}
