package farvix.solution.client.modules.impl.environment;

import lombok.Getter;
import lombok.Setter;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

@Setter @Getter
@ModuleInfo(name = "Auto Sprint", category = ModuleCategory.ENVIRONMENT, description = "Автоматически спринтует при движении вперёд")
public class AutoSprint extends Module {
    private boolean canSprint = true;
}
