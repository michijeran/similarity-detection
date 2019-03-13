package upc.similarity.semilarapi.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Class used to represent dependencies between requirements
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dependency implements Serializable {

    @JsonProperty(value="dependency_score")
    private Float dependency_score;
    @JsonProperty(value="fromid")
    private String fromid;
    @JsonProperty(value="toid")
    private String toid;
    @JsonProperty(value="status")
    private String status;
    @JsonProperty(value="dependency_type")
    private String dependency_type;
    @JsonProperty(value="description")
    private List<String> description;

    public Dependency() {
        description = new ArrayList<>();
    }

    public Dependency(String fromid, String toid, String status, String dependency_type) {
        this.fromid = fromid;
        this.toid = toid;
        this.status = status;
        this.dependency_type = dependency_type;
    }

    public Dependency(Float dependency_score, String fromid, String toid, String status, String dependency_type, String component) {
        this.dependency_score = dependency_score;
        this.fromid = fromid;
        this.toid = toid;
        this.status = status;
        this.dependency_type = dependency_type;
        this.description = new ArrayList<>();
        description.add(component);
    }

    public String getToid() {
        return toid;
    }

    public String getFromid() {
        return fromid;
    }

    public String getDependency_type() {
        return dependency_type;
    }

    public float getDependency_score() {
        return dependency_score;
    }

    public String getStatus() {
        return status;
    }

    public String print_json() {
        JSONObject aux = new JSONObject();
        aux.put("fromid", fromid);
        aux.put("toid", toid);
        if (status != null) aux.put("status",status);
        if (dependency_score != null) aux.put("dependency_score",dependency_score);
        if (dependency_type != null) aux.put("dependency_type",dependency_type);
        if (description != null && description.size() > 0) {
            JSONArray descr = new JSONArray();
            for (String word : description) descr.put(word);
            aux.put("description",descr);
        }
        return aux.toString();
    }

    @Override
    public String toString() {
        return "Dependency between requirement " + fromid + "and requirement " + toid + " with type " + dependency_type + ", status " + status + " and score " + dependency_score + ".";
    }
}