package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.Requirement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "InputClusterOp", description = "OpenReqJson with requirements and accepted duplicates dependencies")
public class InputClusterOp implements Serializable {

    @JsonProperty(value="requirements")
    private List<Requirement> requirements;

    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;

    public InputClusterOp() {
        requirements = new ArrayList<>();
        dependencies = new ArrayList<>();
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }


}
