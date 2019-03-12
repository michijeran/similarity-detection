package upc.similarity.semilarapi;

import upc.similarity.semilarapi.dao.SQLiteDAO;
import upc.similarity.semilarapi.service.SemilarService;
import upc.similarity.semilarapi.service.SemilarServiceImpl;

public class DriverDatabase {

    public static void main(String[] args) {
        try {
            SQLiteDAO db = new SQLiteDAO();
            //db.createDatabase();
            SemilarService ss = new SemilarServiceImpl();
            System.out.println(ss.getStakeholders());
            System.out.println(ss.getRequirements("Test"));
            System.out.println(ss.getDependencies("Test"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
