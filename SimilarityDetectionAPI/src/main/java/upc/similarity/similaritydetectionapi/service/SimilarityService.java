package upc.similarity.similaritydetectionapi.service;

import upc.similarity.similaritydetectionapi.entity.input_output.*;
import upc.similarity.similaritydetectionapi.exception.*;

import java.util.List;

public interface SimilarityService {

    // Req - Req
    public Result_id simReqReq(String stakeholderId, String req1, String req2, String compare, String url, JsonReqReq input) throws BadRequestException, InternalErrorException, NotFoundException;

    // Req - Project
    public Result_id simReqProj(String stakeholderId, List<String> req, String project, String compare, float threshold, String url, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException;

    // Project
    public Result_id simProj(String stakeholderId, String project, String compare, float threshold, String url, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException;

    // Clusters
    public Result_id simCluster(String project, String compare, float threshold, String url, String type, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException;

    public Result_id iniClusters(String stakeholderId, String compare, String url, JsonCluster input) throws BadRequestException, InternalErrorException, NotFoundException;

    public Result_id updateClusters(boolean type, String stakeholderId, String compare, String url, JsonCluster input) throws BadRequestException, InternalErrorException, NotFoundException;

    public Result_id computeClusters(boolean type, String stakeholderId, String compare, String url) throws BadRequestException, InternalErrorException, NotFoundException;

    public Result_id modifyThreshold(boolean type, String compare, String stakeholderId, float threshold, String url) throws InternalErrorException;

    //Gets
    public Result_id projects(String stakeholderId, List<String> projects, String url, Projects input) throws InternalErrorException, BadRequestException, NotFoundException;

    public Result_id reqProject(String stakeholderId, String project, String requirement, String url, Projects input) throws InternalErrorException, BadRequestException, NotFoundException;

    // DB
    public Result_id addRequirements(String stakeholderId, Requirements input, String url) throws ComponentException, BadRequestException, NotFoundException;

    //Auxiliary
    public Result_id resetStakeholder(String stakeholderId, String url) throws InternalErrorException, BadRequestException, NotFoundException;

    void clearDB() throws SemilarException, BadRequestException, NotFoundException;
}