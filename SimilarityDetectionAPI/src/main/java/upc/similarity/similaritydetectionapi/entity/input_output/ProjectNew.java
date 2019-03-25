package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Project;
import upc.similarity.similaritydetectionapi.entity.Requirement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "JsonProjectNew", description = "OpenReqJson with requirements and projects")
public class ProjectNew implements Serializable {

    @JsonProperty(value="projects")
    private List<Project> projects;

    @JsonProperty(value="requirements")
    private List<Requirement> requirements;

    public ProjectNew() {
        this.projects = new ArrayList<>();
        this.requirements = new ArrayList<>();
    }

    public List<Project> getProjects() {
        return projects;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }
}
