package upc.similarity.semilarapi.service;

import java.sql.SQLException;
import java.util.List;

import upc.similarity.semilarapi.entity.input.*;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;

public interface SemilarService {

    //Similarity
    public void similarity(String stakeholderId, String compare, String filename, PairReq input) throws SQLException, BadRequestException, InternalErrorException;

    public void reqProjectNew(boolean type, String compare, float threshold, String filename, ReqProjNewOp input) throws InternalErrorException;

    public void projectsNew(boolean type, String compare, float threshold, String filename, Requirements input) throws InternalErrorException;

    public void iniClusters(String stakeholderId, String filename, IniClusterOp input) throws InternalErrorException, BadRequestException;

    public void updateClusters(boolean type, String compare, String stakeholderId, String filename, IniClusterOp input) throws InternalErrorException, BadRequestException;

    public void computeClusters(boolean type, String compare, String stakeholderId, String filename) throws InternalErrorException, BadRequestException;

    public void modifyThreshold(boolean type, String filename, String compare, String stakeholderId, float threshold) throws InternalErrorException, BadRequestException;

    public void resetStakeholder(String stakeholderId) throws InternalErrorException, BadRequestException;

    public void reqProject(String stakeholderId, String filename, String requirement_id, Projects input) throws BadRequestException, InternalErrorException;

    public void projects(String stakeholderId, String filename, Projects input) throws BadRequestException, InternalErrorException;




    //Database
    public void savePreprocessed(String stakeholderId, List<Requirement> reqs) throws InternalErrorException;

    public void clearDB() throws InternalErrorException;

    //Test
    public String getDependencies(String stakeholderid) throws InternalErrorException;
    public String getRequirements(String stakeholderid) throws InternalErrorException;
    public String getStakeholders() throws InternalErrorException;
}