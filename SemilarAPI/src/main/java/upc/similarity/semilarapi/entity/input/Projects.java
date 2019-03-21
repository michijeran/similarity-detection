package upc.similarity.semilarapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Projects implements Serializable {

    @JsonProperty(value="requirements")
    private List<RequirementId> requirements;

    public Projects() {
        requirements = new ArrayList<>();
    }

    public List<RequirementId> getRequirements() {
        return requirements;
    }

}
