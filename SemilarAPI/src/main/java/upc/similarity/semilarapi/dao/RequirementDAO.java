package upc.similarity.semilarapi.dao;

import upc.similarity.semilarapi.entity.Dependency;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.entity.Stakeholder;

import java.sql.SQLException;
import java.util.List;

public interface RequirementDAO {

    public void savePreprocessed(Requirement r, String stakeholderid) throws SQLException, ClassNotFoundException;

    public Requirement getRequirement(String id, String stakeholderid) throws SQLException, ClassNotFoundException;

    public Dependency getDependency(String fromid, String toid, String stakeholderid) throws SQLException, ClassNotFoundException;

    public void updateThreshold(String stakeholder, float threshold) throws SQLException, ClassNotFoundException;

    public void updateRequirementCluster(Requirement requirement, String stakeholderid) throws SQLException, ClassNotFoundException;

    public void saveDependency(Dependency dependency, boolean accepted, String stakeholderid) throws SQLException, ClassNotFoundException;

    public void deleteDependency(Dependency dependency, String stakeholderid) throws SQLException, ClassNotFoundException;

    public void deleteRequirement(Requirement requirement, String stakeholderid) throws SQLException, ClassNotFoundException;

    public void deleteRequirementDependencies(Requirement requirement, String stakeholderid) throws SQLException, ClassNotFoundException;

    public void clearDB() throws SQLException, ClassNotFoundException;

    public float getThreshold(String stakeholderid) throws SQLException, ClassNotFoundException;

    public List<Requirement> getRequirements(String stakeholderid) throws SQLException, ClassNotFoundException;

    public List<Dependency> getDependencies(String stakeholderid) throws SQLException, ClassNotFoundException;

    public List<Stakeholder> getStakeholders() throws SQLException, ClassNotFoundException;
}
