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
public class TestAuxiliaryOperations extends ControllerTest {

    private String path = "../testing/integration/auxiliary/";
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
        assertEquals(read_file(path+"/modifyThreshold/simple/output_modifyThreshold_simple_stakeholders.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetStakeholders"));
        finished = false;
    }

    @Test
    public void ModifyThreshold_recompute() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/modifyThreshold/recompute/input_auxiliary_modifyThreshold_recompute.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ComputeClusters?type=false&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?type=false&compare=true&threshold=0.4&stakeholderId=Test&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/modifyThreshold/recompute/output_auxiliary_modifyThreshold_recompute.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"modifyThreshold","true"),second_result.result_info);
        assertEquals(read_file(path+"/modifyThreshold/recompute/output_auxiliary_modifyThreshold_recompute_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/modifyThreshold/recompute/output_auxiliary_modifyThreshold_recompute_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }

    @Test
    public void cResetOrganization() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/resetOrganization/input_auxiliary_resetOrganization.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ResetOrganization?&stakeholderId=Test&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/resetOrganization/output_auxiliary_resetOrganization.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"resetOrganization","true"),second_result.result_info);
        assertEquals(read_file(path+"/resetOrganization/output_auxiliary_resetOrganization_dependencies.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetDependencies?stakeholderId=Test"));
        assertEquals(read_file(path+"/resetOrganization/output_auxiliary_resetOrganization_requirements.json"), connect_to_component_simple("http://localhost:9405/upc/Semilar/TestGetRequirements?stakeholderId=Test"));
        finished = false;
    }




}
