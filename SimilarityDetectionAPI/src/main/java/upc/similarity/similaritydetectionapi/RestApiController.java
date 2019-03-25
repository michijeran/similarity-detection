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
   @RequestMapping(value = "/ReqProjectNew", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
   @ApiOperation(value = "", notes = "",tags = "ProjectsNew")
   @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
           @ApiResponse(code=410, message = "Not Found"),
           @ApiResponse(code=411, message = "Bad request"),
           @ApiResponse(code=510, message = "Internal Error"),
           @ApiResponse(code=511, message = "Component Error")})
   public ResponseEntity<?> reqProjectNew(@ApiParam(value="Id of the requirements to compare", required = true, example = "SQ-132") @RequestParam("req") List<String> req,
                                          @ApiParam(value="Id of the project to compare", required = true, example = "SM") @RequestParam("project") String project,
                                          @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam(value = "compare", required = false) String compare,
                                          @ApiParam(value="Algorithm type", required = false, example = "all/one") @RequestParam(value = "type", required = false) boolean type,
                                          @ApiParam(value="Float between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.3") @RequestParam("threshold") Float threshold,
                                          @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                          @ApiParam(value="Json with requirements", required = true) @RequestBody ProjectNew json) {
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
    @ApiOperation(value = "", notes = "",tags = "ProjectsNew")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=510, message = "Internal Error"),
            @ApiResponse(code=511, message = "Component Error")})
    public ResponseEntity<?> projectsNew(  @ApiParam(value="Ids of the projects to compare", required = true, example = "SM") @RequestParam("project") List<String> project,
                                           @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam(value = "compare", required = false) String compare,
                                           @ApiParam(value="Algorithm type", required = false, example = "all/one") @RequestParam(value = "type", required = false) boolean type,
                                           @ApiParam(value="Float between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.3") @RequestParam("threshold") float threshold,
                                           @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                           @ApiParam(value="Json with requirements", required = true) @RequestBody ProjectNew json) {
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

    /*
    Testing operations
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

    @CrossOrigin
    @RequestMapping(value = "/ModifyThreshold", method = RequestMethod.POST)
    @ApiOperation(value = "ModifyThreshold",tags = "Auxiliary")
    public ResponseEntity<?> ModifyThreshold(@ApiParam(value="stakeholderId", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                             @ApiParam(value="threshold", required = true, example = "0.4") @RequestParam("threshold") float threshold,
                                             @ApiParam(value="Use text attribute in comparison?", required = false, example = "false", defaultValue = "true") @RequestParam(value = "compare", required = false) String compare,
                                             @ApiParam(value="Compute all requirements?", required = false, example = "true") @RequestParam(value = "type", required = false) boolean type,
                                             @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url) {

        try {
            url_ok(url);
            if (compare == null) compare = "false";
            return new ResponseEntity<>(similarityService.modifyThreshold(type,compare,stakeholderId,threshold,url),HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/InitializeClusters", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "InitializeClusters",tags = "Clusters")
    public ResponseEntity<?> InitializeClusters(@ApiParam(value="stakeholderId", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                      @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                      @ApiParam(value="OpenreqJson with the initial dependencies and requirements", required = true) @RequestBody JsonCluster json) {

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
    @RequestMapping(value = "/ComputeClusters", method = RequestMethod.POST)
    @ApiOperation(value = "ComputeClusters",tags = "Clusters")
    public ResponseEntity<?> ComputeClusters(@ApiParam(value="stakeholderId", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                            @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam(value = "compare", required = false) String compare,
                                            @ApiParam(value="Compute all requirements?", required = true, example = "true") @RequestParam("type") boolean type,
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
    @RequestMapping(value = "/UpdateClusters", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "UpdateClusters",tags = "Clusters")
    public ResponseEntity<?> UpdateClusters(@ApiParam(value="stakeholderId", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                                @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam(value = "compare", required = false) String compare,
                                                @ApiParam(value="Compute all requirements?", required = true, example = "true") @RequestParam("type") boolean type,
                                                @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                                @ApiParam(value="OpenreqJson with the updated data", required = true) @RequestBody JsonCluster json) {

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
    @ApiOperation(value = "ResetOrganization",tags = "Auxiliary")
    public ResponseEntity<?> ResetOrganization(@ApiParam(value="stakeholderId", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                               @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                               @ApiParam(value="OpenreqJson with the updated data", required = true) @RequestBody JsonCluster json) {

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
    @RequestMapping(value = "/Projects", method = RequestMethod.POST)
    @ApiOperation(value = "Projects",tags = "Projects")
    public ResponseEntity<?> Projects(@ApiParam(value="stakeholderId", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                      @ApiParam(value="Ids of the projects", required = true, example = "UPC") @RequestParam("projects") List<String> projects,
                                      @ApiParam(value="OpenreqJson with the projects and their requirements", required = true) @RequestBody Projects json,
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
    @ApiOperation(value = "ReqProject",tags = "Projects")
    public ResponseEntity<?> ReqProject(@ApiParam(value="stakeholderId", required = true, example = "UPC") @RequestParam("stakeholderId") String stakeholderId,
                                      @ApiParam(value="Id of the project", required = true, example = "UPC") @RequestParam("project") String project,
                                      @ApiParam(value="Id of the requirement", required = true, example = "UPC") @RequestParam("requirement") String requirement,
                                      @ApiParam(value="OpenreqJson with the project and their requirements", required = true) @RequestBody Projects json,
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

    /*
    auxiliary operations
     */

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