package upc.similarity.semilarapi.service;

import java.sql.SQLException;
import java.util.List;

import org.json.JSONObject;
import upc.similarity.semilarapi.entity.input.IniClusterOp;
import upc.similarity.semilarapi.entity.input.PairReq;
import upc.similarity.semilarapi.entity.input.ProjOp;
import upc.similarity.semilarapi.entity.input.ReqProjOp;
import upc.similarity.semilarapi.entity.output.Dependencies;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;

public interface SemilarService {

    //Similarity
    public void similarity(String stakeholderId, String compare, String filename, PairReq input) throws SQLException, BadRequestException, InternalErrorException;

    public void similarityReqProj(String stakeholderId, String compare, float threshold, String filename, ReqProjOp input) throws InternalErrorException;

    public void similarityProj(String stakeholderId, String compare, float threshold, String filename, ProjOp input) throws InternalErrorException;

    public void similarityProj_Large(String stakeholderId, String compare, float threshold, String filename, ProjOp input) throws InternalErrorException;

    public void iniClusters(String compare, String stakeholderId, String filename, IniClusterOp input) throws InternalErrorException, BadRequestException;

    public void updateClusters(boolean type, String compare, String stakeholderId, String filename, IniClusterOp input) throws InternalErrorException, BadRequestException;

    public void computeClusters(boolean type, String compare, String stakeholderId, String filename) throws InternalErrorException, BadRequestException;

    public void modifyThreshold(String stakeholderId, float threshold) throws InternalErrorException, BadRequestException;


    //Database
    public void savePreprocessed(String stakeholderId, List<Requirement> reqs) throws InternalErrorException;

    public void clearDB() throws InternalErrorException;

    //Test
    public String getDependencies(String stakeholderid) throws InternalErrorException;
    public String getRequirements(String stakeholderid) throws InternalErrorException;
    public String getStakeholders() throws InternalErrorException;
}