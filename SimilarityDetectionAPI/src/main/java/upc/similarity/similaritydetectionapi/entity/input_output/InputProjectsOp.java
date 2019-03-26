package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "InputProjectsOp", description = "OpenReqJson with projects")
public class InputProjectsOp implements Serializable {

    @JsonProperty(value="projects")
    private List<Project> projects;

    public InputProjectsOp() {
        this.projects = new ArrayList<>();
    }

    public List<Project> getProjects() {
        return projects;
    }

}
