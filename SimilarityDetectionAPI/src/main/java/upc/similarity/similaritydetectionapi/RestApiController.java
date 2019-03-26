package upc.similarity.similaritydetectionapi;

import io.swagger.annotations.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
//import upc.similarity.similaritydetectionapi.test.ControllerTest;
import upc.similarity.similaritydetectionapi.entity.input_output.*;
import upc.similarity.similaritydetectionapi.exception.*;
import upc.similarity.similaritydetectionapi.service.SimilarityService;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import upc.similarity.similaritydetectionapi.test.ControllerTest;

@RestController
@RequestMapping(value = "upc/similarity-detection")
@Api(value = "SimilarityDetectionAPI", produces = MediaType.APPLICATION_JSON_VALUE)
public class RestApiController {

    @Autowired
    SimilarityService similarityService;

    @CrossOrigin
    @RequestMapping(value = "/ModifyThreshold", method = RequestMethod.POST)
    @ApiOperation(value = "ModifyThreshold", notes = "If already exists a threshold for the organization, the threshold is updated and the clusters are recomputed with the new one." +
            " Otherwise, the threshold for the organization is created.", tags = "Auxiliary")
    public ResponseEntity<?> ModifyThreshold(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                             @ApiParam(value="threshold", required = true, example = "0.4") @RequestParam("threshold") float threshold,
                                             @ApiParam(value="Use text attribute in comparison?", required = false, example = "false", defaultValue = "true") @RequestParam(value = "compare", required = false, defaultValue = "true") String compare,
                                             @ApiParam(value="Algorithm type", required = false, example = "true") @RequestParam(value = "type", required = false, defaultValue = "false") boolean type,
                                             @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url) {

        try {
            url_ok(url);
            return new ResponseEntity<>(similarityService.modifyThreshold(type,compare,stakeholderId,threshold,url),HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/InitializeClusters", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "InitializeClusters", notes = "Pre-process the requirements to be used by the similarity algorithm and creates a first set of clusters containing the tacit knowledge of the organization (i.e., accepted and rejected duplicates)",tags = "Clusters")
    public ResponseEntity<?> InitializeClusters(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                      @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                      @ApiParam(value="OpenreqJson with the initial dependencies and requirements", required = true) @RequestBody InputClusterOp json) {

        try {
            url_ok(url);
            return new ResponseEntity<>(similarityService.iniClusters(stakeholderId,url,json),HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }

    }

    @CrossOrigin
    @RequestMapping(value = "/ComputeClusters", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "ComputeClusters", notes = "Computes the non-tacit duplicates for the requirements for this organization stored in the similarity component database. Returns the new duplicate dependencies found.", tags = "Clusters")
    public ResponseEntity<?> ComputeClusters(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                            @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam(value = "compare", required = false) String compare,
                                            @ApiParam(value="Algorithm type", required = true, example = "true") @RequestParam("type") boolean type,
                                            @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url) {

        try {
            url_ok(url);
            if (compare == null) compare = "false";
            return new ResponseEntity<>(similarityService.computeClusters(type,stakeholderId,compare,url),HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }

    }

    @CrossOrigin
    @RequestMapping(value = "/UpdateClusters", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "UpdateClusters", notes = "<p>Updates the clusters for the organization with the information received:</p>" +
            "<ul><li>deleted/rejected duplicate dependencies</li>" +
            "<li>deleted requirements</li>" +
            "<li>added/accepted duplicate dependencies</li>" +
            "<li>added requirements</li>" +
            "<li>edited requirements</li></ul> " +
            "<p>Returns the new duplicate dependencies found.</p>", tags = "Clusters")
    public ResponseEntity<?> UpdateClusters(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                                @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam(value = "compare", required = false) String compare,
                                                @ApiParam(value="Algorithm type", required = true, example = "true") @RequestParam("type") boolean type,
                                                @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                                @ApiParam(value="OpenreqJson with the updated data", required = true) @RequestBody InputClusterOp json) {

        try {
            url_ok(url);
            if (compare == null) compare = "false";
            return new ResponseEntity<>(similarityService.updateClusters(type,stakeholderId,compare,url,json),HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }

    }

    @CrossOrigin
    @RequestMapping(value = "/ResetOrganization", method = RequestMethod.POST)
    @ApiOperation(value = "ResetOrganization", notes = "Deletes from the database all the information related to the organization received as parameter: clusters, pre-processed requirements and dependencies.", tags = "Auxiliary")
    public ResponseEntity<?> ResetOrganization(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                               @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url) {

        try {
            url_ok(url);
            return new ResponseEntity<>(similarityService.resetStakeholder(stakeholderId,url),HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }

    }

    @CrossOrigin
    @RequestMapping(value = "/InputProjectsOp", method = RequestMethod.POST)
    @ApiOperation(value = "InputProjectsOp", notes = "Returns the already computed duplicates inside the requirements of an specific project.", tags = "InputProjectsOp")
    public ResponseEntity<?> Projects(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                      @ApiParam(value="Ids of the projects", required = true, example = "UPC") @RequestParam("projects") List<String> projects,
                                      @ApiParam(value="OpenreqJson with the projects and their requirements", required = true) @RequestBody InputProjectsOp json,
                                      @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url) {

        try {
            url_ok(url);
            return new ResponseEntity<>(similarityService.projects(stakeholderId,projects,url,json),HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        }

    }

    @CrossOrigin
    @RequestMapping(value = "/ReqProject", method = RequestMethod.POST)
    @ApiOperation(value = "ReqProject", notes = "Returns the already computed duplicates of the specified requirement with the requirements of an specific project.", tags = "InputProjectsOp")
    public ResponseEntity<?> ReqProject(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                      @ApiParam(value="Id of the project", required = true, example = "UPC") @RequestParam("project") String project,
                                      @ApiParam(value="Id of the requirement", required = true, example = "UPC") @RequestParam("requirement") String requirement,
                                      @ApiParam(value="OpenreqJson with the project and their requirements", required = true) @RequestBody InputProjectsOp json,
                                      @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url) {

        try {
            url_ok(url);
            return new ResponseEntity<>(similarityService.reqProject(stakeholderId,project,requirement,url,json),HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        }

    }


    @CrossOrigin
    @RequestMapping(value = "/ReqProjectNew", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "ReqProjectNew", notes = "Computes and return the duplicates of the specified requirements with the requirements of an specific project without storing any knowledge in the database.",tags = "ProjectsNew")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=510, message = "Internal Error")})
    public ResponseEntity<?> reqProjectNew(@ApiParam(value="Id of the requirements to compare", required = true, example = "SQ-132") @RequestParam("requirement") List<String> req,
                                           @ApiParam(value="Id of the project to compare", required = true, example = "SM") @RequestParam("project") String project,
                                           @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam(value = "compare", required = false) String compare,
                                           @ApiParam(value="Algorithm type", required = false, example = "all/one") @RequestParam(value = "type", required = false) boolean type,
                                           @ApiParam(value="Float between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.3") @RequestParam("threshold") Float threshold,
                                           @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                           @ApiParam(value="OpenReqJson with requirements and projects", required = true) @RequestBody InputProjectsNewOp json) {
        try {
            url_ok(url);
            if (compare == null) compare = "false";
            return new ResponseEntity<>(similarityService.reqProjectNew(req,project,compare,type,threshold,url,json), HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/ProjectsNew", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "ProjectsNew", notes = "Computes and returns the duplicates inside the requirements of specific projects without storing any knowledge in the database.",tags = "ProjectsNew")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=510, message = "Internal Error")})
    public ResponseEntity<?> projectsNew(  @ApiParam(value="Ids of the projects to compare", required = true, example = "SM") @RequestParam("project") List<String> project,
                                           @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam(value = "compare", required = false) String compare,
                                           @ApiParam(value="Algorithm type", required = false, example = "all/one") @RequestParam(value = "type", required = false) boolean type,
                                           @ApiParam(value="Float between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.3") @RequestParam("threshold") float threshold,
                                           @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                           @ApiParam(value="OpenReqJson with requirements and projects", required = true) @RequestBody InputProjectsNewOp json) {
        try {
            url_ok(url);
            if (compare == null) compare = "false";
            return new ResponseEntity<>(similarityService.projectsNew(project,compare,type,threshold,url,json), HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }
    }

    /*
    auxiliary operations
     */


    @CrossOrigin
    @RequestMapping(value = "/Test", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "Testing result")
    public ResponseEntity<?> testing(@RequestParam("result") MultipartFile file,
                                     @RequestParam("info") JSONObject json) {

        //System.out.println("Enter");
        try {
            InputStream reader = file.getInputStream();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = reader.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            byte[] byteArray = buffer.toByteArray();

            String text = new String(byteArray, StandardCharsets.UTF_8);

            ControllerTest.setResult(text, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /*@CrossOrigin
    @RequestMapping(value = "/DB/Clear", method = RequestMethod.DELETE)
    @ApiOperation(value = "Clear the Semilar library database", notes = "It's useful to clear the database of old requirements.")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=511, message = "Component Error")})
    public ResponseEntity<?> clearDB() {
        try {
            similarityService.clearDB();
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        }
    }*/

    private void url_ok(String url) throws BadRequestException {
        try {
            new URL(url).toURI();
            /*HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.connect();
            int code = connection.getResponseCode();
            if (code != 200) throw new BadRequestException("Output server doesn't return status code 200");*/
        } catch (Exception e) {
            throw new BadRequestException("Output server doesn't exist");
        }
    }

    private ResponseEntity<?> getResponseNotFound(NotFoundException e) {
        e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", "410");
        result.put("error", "Not Found");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<?> getResponseBadRequest(BadRequestException e) {
        e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", "411");
        result.put("error", "Bad request");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<?> getInternalError(InternalErrorException e) {
        e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", "510");
        result.put("error", "Server Error");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<?> getComponentError(ComponentException e) {
        e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", "511");
        result.put("error", "Component error");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}