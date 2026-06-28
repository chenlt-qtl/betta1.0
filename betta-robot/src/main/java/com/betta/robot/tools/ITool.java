package com.betta.robot.tools;

import com.betta.robot.dto.ActionResult;
import com.betta.robot.dto.CommandDTO;

public interface ITool {
    ActionResult execute(CommandDTO commandDTO);
}
