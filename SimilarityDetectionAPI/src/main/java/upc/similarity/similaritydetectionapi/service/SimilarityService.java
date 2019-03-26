package upc.similarity.similaritydetectionapi.service;

import upc.similarity.similaritydetectionapi.entity.input_output.*;
import upc.similarity.similaritydetectionapi.exception.*;

import java.util.List;

public interface SimilarityService {

    public Result_id reqProjectNew(List<String> req, String project, String compare, boolean type, float threshold, String url, InputProjectsNewOp json) throws BadRequestException, InternalErrorException, NotFoundException;

    public Result_id projectsNew(List<String> project, String compare, boolean type, float threshold, String url, InputProjectsNewOp json) throws BadRequestException, InternalErrorException, NotFoundException;

    public Result_id iniClusters(String stakeholderId, String url, InputClusterOp input) throws BadRequestException, InternalErrorException, NotFoundException;

    public Result_id updateClusters(boolean type, String stakeholderId, String compare, String url, InputClusterOp input) throws BadRequestException, InternalErrorException, NotFoundException;

    public Result_id computeClusters(boolean type, String stakeholderId, String compare, String url) throws BadRequestException, InternalErrorException, NotFoundException;

    public Result_id modifyThreshold(boolean type, String compare, String stakeholderId, float threshold, String url) throws InternalErrorException;

    //Gets
    public Result_id projects(String stakeholderId, List<String> projects, String url, InputProjectsOp input) throws InternalErrorException, BadRequestException, NotFoundException;

    public Result_id reqProject(String stakeholderId, String project, String requirement, String url, InputProjectsOp input) throws InternalErrorException, BadRequestException, NotFoundException;

    //Auxiliary
    public Result_id resetStakeholder(String stakeholderId, String url) throws InternalErrorException, BadRequestException, NotFoundException;

    void clearDB() throws SemilarException, BadRequestException, NotFoundException;
}