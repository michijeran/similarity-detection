package upc.similarity.similaritydetectionapi.test;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import upc.similarity.similaritydetectionapi.exception.InternalErrorException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestClusters extends ControllerTest {

    private static class First_Result {
        int httpStatus;
        String id;
        First_Result(int httpStatus, String id) {
            this.httpStatus = httpStatus;
            this.id = id;
        }
    }

    private String path = "../testing/integration/clusters/";
    @LocalServerPort
    private int port;

    @Before
    public void ClearDB() {
        //auxiliary operation
        HttpClient httpclient = HttpClients.createDefault();
        HttpDelete httpdelete = new HttpDelete("http://localhost:9405/upc/Semilar/Clear");

        //Execute and get the response.
        try {
            httpclient.execute(httpdelete);
        } catch (IOException e) {
            System.out.println("Error conecting with server");
        }
    }

    /*
    ModifyThreshold operation
     */

    @Test
    public void aModifyThreshold_simple() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.4&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(create_json_info(first_result.id,"modifyTheshold","true"),second_result.result_info);
        assertEquals(read_file(path+"/modifyThreshold/output_modifyThreshold_simple_stakeholders.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetStakeholders"));
        finished = false;
    }

    /*
    Ini Clusters operation
     */
    @Test
    public void bIniClusters_simple() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.4&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/iniClusters/simple/input_iniClusters_simple.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/iniClusters/simple/output_iniClusters_simple.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"iniClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/iniClusters/simple/output_iniClusters_simple_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        assertEquals(read_file(path+"/iniClusters/simple/output_iniClusters_simple_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void cIniClusters_addReq() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.4&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/iniClusters/addReq/input_iniClusters_addReq.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/iniClusters/addReq/output_iniClusters_addReq.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"iniClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/iniClusters/addReq/output_iniClusters_addReq_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        assertEquals(read_file(path+"/iniClusters/addReq/output_iniClusters_addReq_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void dIniClusters_merge() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.4&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/iniClusters/merge/input_iniClusters_merge.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/iniClusters/merge/output_iniClusters_merge.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"iniClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/iniClusters/merge/output_iniClusters_merge_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        assertEquals(read_file(path+"/iniClusters/merge/output_iniClusters_merge_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void eIniClusters_rejected() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.4&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/iniClusters/rejected/input_iniClusters_rejected.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/iniClusters/rejected/output_iniClusters_rejected.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"iniClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/iniClusters/rejected/output_iniClusters_rejected_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        assertEquals(read_file(path+"/iniClusters/rejected/output_iniClusters_rejected_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        finished = false;
    }

    /*
    Compute clusters operation
     */

    @Test
    public void fComputeClusters_simple() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/computeClusters/simple/input_computeClusters_simple.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ComputeClusters?type=true&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/computeClusters/simple/output_computeClusters_simple.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"computeClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/computeClusters/simple/output_computeClusters_simple_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        assertEquals(read_file(path+"/computeClusters/simple/output_computeClusters_simple_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void gComputeClusters_rejected() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/computeClusters/rejected/input_computeClusters_rejected.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ComputeClusters?type=true&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/computeClusters/rejected/output_computeClusters_rejected.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"computeClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/computeClusters/rejected/output_computeClusters_rejected_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        assertEquals(read_file(path+"/computeClusters/rejected/output_computeClusters_rejected_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void hComputeClusters_only_masters() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/computeClusters/only_masters/input_computeClusters_only_masters.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ComputeClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/computeClusters/only_masters/output_computeClusters_only_masters.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"computeClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/computeClusters/only_masters/output_computeClusters_only_masters_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/computeClusters/only_masters/output_computeClusters_only_masters_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    /*
    Update clusters operation
     */

    @Test
    public void gUpdateClusters_rejected_deleted_dependencies_reqs_not_in_cluster() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/deleted_rejected_dependencies/reqs_not_in_cluster/input1_deleted_rejected_dependencies_reqs_not_in_cluster.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/deleted_rejected_dependencies/reqs_not_in_cluster/input2_deleted_rejected_dependencies_reqs_not_in_cluster.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/deleted_rejected_dependencies/reqs_not_in_cluster/output_deleted_rejected_dependencies_reqs_not_in_cluster.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/deleted_rejected_dependencies/reqs_not_in_cluster/output_deleted_rejected_dependencies_reqs_not_in_cluster_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/deleted_rejected_dependencies/reqs_not_in_cluster/output_deleted_rejected_dependencies_reqs_not_in_cluster_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void gUpdateClusters_rejected_deleted_dependencies_only_reqs_in_cluster() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/deleted_rejected_dependencies/only_reqs_in_cluster/input1_deleted_rejected_dependencies_only_reqs_in_cluster.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/deleted_rejected_dependencies/only_reqs_in_cluster/input2_deleted_rejected_dependencies_only_reqs_in_cluster.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/deleted_rejected_dependencies/only_reqs_in_cluster/output_deleted_rejected_dependencies_only_reqs_in_cluster.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/deleted_rejected_dependencies/only_reqs_in_cluster/output_deleted_rejected_dependencies_only_reqs_in_cluster_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/deleted_rejected_dependencies/only_reqs_in_cluster/output_deleted_rejected_dependencies_only_reqs_in_cluster_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void gUpdateClusters_rejected_deleted_dependencies_more_reqs_in_the_cluster() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/deleted_rejected_dependencies/more_reqs_in_the_cluster/input1_deleted_rejected_dependencies_more_reqs_in_the_cluster.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/deleted_rejected_dependencies/more_reqs_in_the_cluster/input2_deleted_rejected_dependencies_more_reqs_in_the_cluster.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/deleted_rejected_dependencies/more_reqs_in_the_cluster/output_deleted_rejected_dependencies_more_reqs_in_the_cluster.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/deleted_rejected_dependencies/more_reqs_in_the_cluster/output_deleted_rejected_dependencies_more_reqs_in_the_cluster_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/deleted_rejected_dependencies/more_reqs_in_the_cluster/output_deleted_rejected_dependencies_more_reqs_in_the_cluster_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void hUpdateClusters_deleted_requirements_not_recompute() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/deleted_requirements/not_recompute/input1_deleted_requirements_not_recompute.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/deleted_requirements/not_recompute/input2_deleted_requirements_not_recompute.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/deleted_requirements/not_recompute/output_deleted_requirements_not_recompute.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/deleted_requirements/not_recompute/output_deleted_requirements_not_recompute_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/deleted_requirements/not_recompute/output_deleted_requirements_not_recompute_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void hUpdateClusters_deleted_requirements_recompute() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/deleted_requirements/recompute/input1_deleted_requirements_recompute.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/deleted_requirements/recompute/input2_deleted_requirements_recompute.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/deleted_requirements/recompute/output_deleted_requirements_recompute.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/deleted_requirements/recompute/output_deleted_requirements_recompute_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/deleted_requirements/recompute/output_deleted_requirements_recompute_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void iUpdateClusters_added_accepted_dependencies_reqs_in_db() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_accepted_dependencies/reqs_in_db/input1_added_accepted_dependencies_reqs_in_db.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_accepted_dependencies/reqs_in_db/input2_added_accepted_dependencies_reqs_in_db.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/added_accepted_dependencies/reqs_in_db/output_added_accepted_dependencies_reqs_in_db.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/added_accepted_dependencies/reqs_in_db/output_added_accepted_dependencies_reqs_in_db_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/added_accepted_dependencies/reqs_in_db/output_added_accepted_dependencies_reqs_in_db_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void iUpdateClusters_added_accepted_dependencies_reqs_not_in_db() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_accepted_dependencies/reqs_not_db/input1_added_accepted_dependencies_reqs_not_db.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_accepted_dependencies/reqs_not_db/input2_added_accepted_dependencies_reqs_not_db.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/added_accepted_dependencies/reqs_not_db/output_added_accepted_dependencies_reqs_not_db.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/added_accepted_dependencies/reqs_not_db/output_added_accepted_dependencies_reqs_not_db_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/added_accepted_dependencies/reqs_not_db/output_added_accepted_dependencies_reqs_not_db_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void iUpdateClusters_edited_requirements() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/edited_requirements/input1_edited_requirements.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/edited_requirements/input2_edited_requirements.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/edited_requirements/output_edited_requirements.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/edited_requirements/output_edited_requirements_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/edited_requirements/output_edited_requirements_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void jUpdateClusters_added_requirements_masters_not_duplicate() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_requirements/masters/not_duplicate/input1_added_requirements_masters_not_duplicate.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_requirements/masters/not_duplicate/input2_added_requirements_masters_not_duplicate.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/not_duplicate/output_added_requirements_masters_not_duplicate.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/not_duplicate/output_added_requirements_masters_not_duplicate_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/not_duplicate/output_added_requirements_masters_not_duplicate_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void kUpdateClusters_added_requirements_masters_duplicate_one_cluster() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_requirements/masters/duplicate_one_cluster/input1_added_requirements_masters_duplicate_one_cluster.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_requirements/masters/duplicate_one_cluster/input2_added_requirements_masters_duplicate_one_cluster.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/duplicate_one_cluster/output_added_requirements_masters_duplicate_one_cluster.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/duplicate_one_cluster/output_added_requirements_masters_duplicate_one_cluster_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/duplicate_one_cluster/output_added_requirements_masters_duplicate_one_cluster_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void lUpdateClusters_added_requirements_masters_duplicate_multiple_cluster() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_requirements/masters/duplicate_multiple_cluster/input1_added_requirements_masters_duplicate_multiple_cluster.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_requirements/masters/duplicate_multiple_cluster/input2_added_requirements_masters_duplicate_multiple_cluster.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/duplicate_multiple_cluster/output_added_requirements_masters_duplicate_multiple_cluster.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/duplicate_multiple_cluster/output_added_requirements_masters_duplicate_multiple_cluster_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/duplicate_multiple_cluster/output_added_requirements_masters_duplicate_multiple_cluster_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void mUpdateClusters_added_requirements_masters_database_table() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_requirements/masters/database_table/input1_added_requirements_masters_database_table.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/UpdateClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/updateClusters/added_requirements/masters/database_table/input2_added_requirements_masters_database_table.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/database_table/output_added_requirements_masters_database_table.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"updateClusters","true"),second_result.result_info);
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/database_table/output_added_requirements_masters_database_table_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/updateClusters/added_requirements/masters/database_table/output_added_requirements_masters_database_table_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }









    /*
    Exceptions
     */





















    /*
    auxiliary methods
     */

    private String read_file(String path) {
        String result = "";
        String line = "";
        try {
            FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                result = result.concat(line);
            }
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject aux = new JSONObject(result);
        return aux.toString();
    }

    private First_Result connect_to_component(String url, String json) {

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);
        if(json != null) httppost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        int httpStatus = 500;
        String json_response = "";

        //Execute and get the response.
        try {
            HttpResponse response = httpclient.execute(httppost);
            httpStatus = response.getStatusLine().getStatusCode();
            json_response = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            System.out.println("Error conecting with server");
        }
        //System.out.println(json_response);
        JSONObject aux = new JSONObject(json_response);
        String id = aux.getString("id");
        return new First_Result(httpStatus,id);
    }

    private String create_json_info(String id, String operation, String success) {
        JSONObject result = new JSONObject();
        result.put("id",id);
        result.put("success",success);
        result.put("operation",operation);
        return result.toString();
    }

    private String connect_to_component_simple(String url) {
        HttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        String json_response = "";

        //Execute and get the response.
        try {
            HttpResponse response = httpclient.execute(httpget);
            json_response = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            System.out.println("Error conecting with server");
        }
        JSONObject aux = new JSONObject(json_response);
        return aux.toString();
    }

}
