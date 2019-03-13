package upc.similarity.semilarapi;


import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upc.similarity.semilarapi.entity.input.*;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;
import upc.similarity.semilarapi.service.SemilarService;

import java.sql.SQLException;

@RestController
@RequestMapping(value = "/")
public class RestApiController {

    @Autowired
    SemilarService semilarService;

    //Similarity
    @RequestMapping(value = "/upc/Semilar/PairSim", method = RequestMethod.POST)
    public ResponseEntity<?> similarity(@RequestParam("compare") String compare,
                                        @RequestParam("stakeholderId") String stakeholderId,
                                        @RequestParam("filename") String filename,
                                        @RequestBody PairReq input) {
        try {
            semilarService.similarity(stakeholderId,compare,filename,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(411));
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        } catch (InternalErrorException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        }
    }

    @RequestMapping(value = "/upc/Semilar/ReqProjSim", method = RequestMethod.POST)
    public ResponseEntity<?> similarityReqProj(@RequestParam("compare") String compare,
                                               @RequestParam("stakeholderId") String stakeholderId,
                                               @RequestParam("threshold") float threshold,
                                               @RequestParam("filename") String filename,
                                               @RequestBody ReqProjOp input) {
        try {
            semilarService.similarityReqProj(stakeholderId,compare,threshold,filename,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        }
    }

    @RequestMapping(value = "/upc/Semilar/ProjSim", method = RequestMethod.POST)
    public ResponseEntity<?> similarityProj(@RequestParam("compare") String compare,
                                            @RequestParam("stakeholderId") String stakeholderId,
                                            @RequestParam("threshold") float threshold,
                                            @RequestParam("filename") String filename,
                                            @RequestBody ProjOp input) {
        try {
            /*if (input.getRequirements().size() < 1000)*/ semilarService.similarityProj(stakeholderId,compare,threshold,filename,input);
            //else semilarService.similarityProj_Large(stakeholderid,compare,threshold,filename,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        }
    }

    @RequestMapping(value = "/upc/Semilar/iniClusters", method = RequestMethod.POST)
    public ResponseEntity<?> iniClusters(@RequestParam("compare") String compare,
                                               @RequestParam("stakeholderId") String stakeholderId,
                                               @RequestParam("filename") String filename,
                                               @RequestBody IniClusterOp input) {
        try {
            semilarService.iniClusters(compare,stakeholderId,filename,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @RequestMapping(value = "/upc/Semilar/computeClusters", method = RequestMethod.POST)
    public ResponseEntity<?> computeClusters(@RequestParam("compare") String compare,
                                            @RequestParam("stakeholderId") String stakeholderId,
                                            @RequestParam("filename") String filename) {
        try {
            semilarService.computeClusters(compare,stakeholderId,filename);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @RequestMapping(value = "/upc/Semilar/updateClusters", method = RequestMethod.POST)
    public ResponseEntity<?> updateClusters(@RequestParam("compare") String compare,
                                         @RequestParam("stakeholderId") String stakeholderId,
                                         @RequestParam("filename") String filename,
                                         @RequestBody IniClusterOp input) {
        try {
            semilarService.updateClusters(compare,stakeholderId,filename,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @RequestMapping(value = "/upc/Semilar/modifyThreshold", method = RequestMethod.POST)
    public ResponseEntity<?> modifyThreshold(@RequestParam("stakeholderId") String stakeholderId,
                                         @RequestParam("threshold") float threshold) {
        try {
            semilarService.modifyThreshold(stakeholderId,threshold);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        }
    }


    //Database
    @RequestMapping(value = "/upc/Semilar/Preprocess", method = RequestMethod.POST)
    public ResponseEntity<?> preprocess(@RequestParam("stakeholderId") String stakeholderId,
                                        @RequestBody Requirements input) {
        System.out.println("Preprocessing");
        try {
            semilarService.savePreprocessed(stakeholderId,input.getRequirements());
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(411));
        }
    }

    @RequestMapping(value = "/upc/Semilar/Clear", method = RequestMethod.DELETE)
    public ResponseEntity<?> clearDB() {
        try {
            semilarService.clearDB();
            System.out.println("DB cleared");
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(411));
        }
    }



    //Test
    @RequestMapping(value = "/upc/Semilar/TestGetDependencies", method = RequestMethod.GET)
    public ResponseEntity<?> TestGetDependencies(@RequestParam("stakeholderId") String stakeholderId) {
        try {
            return new ResponseEntity<>(semilarService.getDependencies(stakeholderId), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(500));
        }
    }

    @RequestMapping(value = "/upc/Semilar/TestGetRequirements", method = RequestMethod.GET)
    public ResponseEntity<?> TestGetRequirements(@RequestParam("stakeholderId") String stakeholderId) {
        try {
            return new ResponseEntity<>(semilarService.getRequirements(stakeholderId), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(500));
        }
    }

    @RequestMapping(value = "/upc/Semilar/TestGetStakeholders", method = RequestMethod.GET)
    public ResponseEntity<?> TestGetStakeholders() {
        try {
            return new ResponseEntity<>(semilarService.getStakeholders(), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(500));
        }
    }



}