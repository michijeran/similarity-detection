package upc.similarity.semilarapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import upc.similarity.semilarapi.entity.Dependency;
import upc.similarity.semilarapi.entity.Requirement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Input class for iniClusters operation with requirements and dependencies
public class IniClusterOp implements Serializable {

    @JsonProperty(value="requirements")
    private List<Requirement> requirements;
    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;

    public IniClusterOp() {
        this.requirements = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}