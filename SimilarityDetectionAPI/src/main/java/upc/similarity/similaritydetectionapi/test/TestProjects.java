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
public class TestProjects extends ControllerTest {

    private String path = "../testing/integration/projects/";
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
    ReqProject operation
     */

    @Test
    public void aReqproject() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/reqproject/input1_projects_reqproject.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ComputeClusters?type=true&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ReqProject?project=P1&requirement=QM-1&stakeholderId=Test&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/reqproject/input2_projects_reqproject.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/reqproject/output_projects_reqproject.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"reqProject","true"),second_result.result_info);
        finished = false;
    }

    /*
    Projects operation
     */

    @Test
    public void bProjects() throws InterruptedException {
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ModifyThreshold?stakeholderId=Test&threshold=0.3&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/InitializeClusters?stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/projects/input1_projects_projects.json"));
        while(!finished) {Thread.sleep(2000);}
        finished = false;
        connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ComputeClusters?type=true&stakeholderId=Test&compare=true&url=http://localhost:"+port+"/upc/similarity-detection/Test",null);
        while(!finished) {Thread.sleep(2000);}
        finished = false;

        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/Projects?projects=P1&projects=P2&stakeholderId=Test&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"/projects/input2_projects_projects.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"/projects/output_projects_projects.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"projects","true"),second_result.result_info);
        finished = false;
    }
}
