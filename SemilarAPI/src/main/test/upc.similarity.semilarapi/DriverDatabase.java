package upc.similarity.semilarapi;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.semilarapi.dao.SQLiteDAO;
import upc.similarity.semilarapi.exception.InternalErrorException;
import upc.similarity.semilarapi.service.SemilarService;
import upc.similarity.semilarapi.service.SemilarServiceImpl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class DriverDatabase {

    public static void main(String[] args) {
        try {
            SQLiteDAO db = new SQLiteDAO();
            String filename = "";
            Path p = Paths.get(filename);
            //db.createDatabase();
            /*SemilarService ss = new SemilarServiceImpl();
            //System.out.println(ss.getStakeholders());
            //System.out.println(ss.getRequirements("Test"));
            //System.out.println(ss.getDependencies("Test"));
            JSONObject json = new JSONObject(db.getRequirements_JSON("UPC"));
            JSONArray clusters = json.getJSONArray("clusters");
            for (int i = 0; i < clusters.length(); ++i) {
                JSONObject aux = clusters.getJSONObject(i);
                write_to_file(aux.toString() + System.lineSeparator(),p);
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void write_to_file(String text, Path p) throws InternalErrorException {
        try (BufferedWriter writer = Files.newBufferedWriter(p, StandardOpenOption.APPEND)) {
            writer.write(text);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new InternalErrorException("Write start to file fail");
        }
    }

}
