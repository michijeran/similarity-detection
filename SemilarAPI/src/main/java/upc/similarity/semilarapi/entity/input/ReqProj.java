package upc.similarity.semilarapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class ReqProj {

    @JsonProperty(value="requirement")
    private RequirementId requirement;
    @JsonProperty(value="project_requirements")
    private List<RequirementId> project_requirements;

    public ReqProj() {
        this.project_requirements = new ArrayList<>();
    }

    public RequirementId getRequirement() {
        return requirement;
    }

    public List<RequirementId> getProject_requirements() {
        return project_requirements;
    }
}
