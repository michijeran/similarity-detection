package upc.similarity.semilarapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Cluster implements Serializable {

    private long clusterid;
    private Requirement req_older;
    private List<Requirement> specifiedRequirements;
    private static int count = 1;

    private Timestamp req_time;

    public Cluster(long id) {
        clusterid = id;
        req_older = null;
        specifiedRequirements = new ArrayList<>();
    }

    /*public Cluster() {
        clusterid = count;
        ++count;
        req_older = null;
        specifiedRequirements = new ArrayList<>();
    }*/

    public Requirement getReq_older() {
        return req_older;
    }

    public List<Requirement> getSpecifiedRequirements() {
        return specifiedRequirements;
    }

    public long getClusterid() {
        return clusterid;
    }

    public void addReq(Requirement requirement) {
        requirement.setClusterid(this);
        requirement.setMaster(false);
        if (req_older == null) {
            req_older = requirement;
            requirement.setMaster(true);
            if (requirement.getCreated_at() != null && requirement.getCreated_at() != 0) {
                req_time = new Timestamp(requirement.getCreated_at());
            }
        } else {
            if (requirement.getCreated_at() != null && requirement.getCreated_at() != 0) {
                Timestamp stampNew = new Timestamp(requirement.getCreated_at());
                if (req_time == null || stampNew.before(req_time)) {
                    req_older.setMaster(false);
                    req_older = requirement;
                    req_time = stampNew;
                    requirement.setMaster(true);
                }
            }
        }
        specifiedRequirements.add(requirement);
    }

    public void removeReq(Requirement requirement) {
        specifiedRequirements.remove(requirement);
        requirement.setMaster(false);
        requirement.setClusterid(null);
        if (requirement == req_older) {
            if (specifiedRequirements.size() > 0) {
                req_older = specifiedRequirements.get(0);
                req_older.setMaster(true);
                req_time = null;
                if (req_older.getCreated_at() != null && req_older.getCreated_at() != 0)  req_time = new Timestamp(req_older.getCreated_at());
                for (Requirement requirement2: specifiedRequirements) {
                    if (requirement2 != null && requirement2.getCreated_at() != 0) {
                        Timestamp stampNew = new Timestamp(requirement2.getCreated_at());
                        if (req_time == null || stampNew.before(req_time)) {
                            req_older.setMaster(false);
                            req_older = requirement2;
                            req_time = stampNew;
                            requirement2.setMaster(true);
                        }
                    }
                }
            } else {
                req_older = null;
                req_time = null;
            }
        }
    }
}
