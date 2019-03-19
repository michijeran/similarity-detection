package upc.similarity.similaritydetectionapi.test;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestComputeClusters extends ControllerTest {

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
}
