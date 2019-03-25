package upc.similarity.semilarapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import upc.similarity.semilarapi.entity.Requirement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ReqProjNewOp implements Serializable {

    @JsonProperty(value="requirements_to_compare")
    private List<Requirement> requirements;
    @JsonProperty(value="project_requirements")
    private List<Requirement> project_requirements;

    public ReqProjNewOp() {
        this.project_requirements = new ArrayList<>();
        this.requirements = new ArrayList<>();
    }

    public List<Requirement> getProject_requirements() {
        return project_requirements;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }
}
