package upc.similarity.semilarapi.dao;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sqlite.SQLiteConfig;
import semilar.data.DependencyStructure;
import semilar.data.Sentence;
import semilar.data.Word;
import upc.similarity.semilarapi.entity.Cluster;
import upc.similarity.semilarapi.entity.Dependency;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.entity.Stakeholder;

import java.sql.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class SQLiteDAO implements RequirementDAO {

    private static Connection c;

    public static final String DB_URL = "jdbc:sqlite:../semilar.db";
    public static final String DRIVER = "org.sqlite.JDBC";

    public static Connection getConnection() throws ClassNotFoundException {
        Class.forName(DRIVER);
        Connection connection = null;
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            connection = DriverManager.getConnection(DB_URL,config.toProperties());
        } catch (SQLException ex) {}
        return connection;
    }

    private void createDatabase() {


        /*
        Stakeholders table
         */

        String sql = "CREATE TABLE IF NOT EXISTS stakeholders (\n"
                + "	id varchar NOT NULL, \n"
                + " threshold float NOT NULL, \n"
                + " last_id_cluster integer NOT NULL, \n"
                + " PRIMARY KEY(id)"
                + ");";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:../semilar.db");
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        /*
        Prepocessed requirements
        id -> primary key
        name -> values in range (0,1) 0-> name null 1-> name not null
        text -> values in range (0,1) 0-> text null 1-> text not null

         */
        sql = "CREATE TABLE IF NOT EXISTS prepocessed (\n"
                + "	id varchar NOT NULL, \n"
                + " created_at long, \n"
                + " name integer, \n"
                + " text integer, \n"
                + "	sentence_name text, \n"
                + "	sentence_text text, \n"
                + " clusterid integer, \n"
                + " master boolean, \n"
                + " stakeholderid varchar NOT NULL, \n"
                + " PRIMARY KEY(id, stakeholderid), \n"
                + " FOREIGN KEY(stakeholderid) REFERENCES stakeholders(id)"
                + ");";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:../semilar.db");
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        /*
        Dependencies table
         */

        sql = "CREATE TABLE IF NOT EXISTS dependencies (\n"
                + "	fromid varchar NOT NULL, \n"
                + " toid varchar NOT NULL, \n"
                + " accepted boolean NOT NULL, \n"
                + " stakeholderid varchar NOT NULL, \n"
                + " PRIMARY KEY(fromid, toid, stakeholderid), \n"
                + " FOREIGN KEY(fromid, stakeholderid) REFERENCES prepocessed(id,stakeholderid), \n"
                + " FOREIGN KEY(toid,stakeholderid) REFERENCES prepocessed(id,stakeholderid)"
                + ");";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:../semilar.db");
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public SQLiteDAO() throws ClassNotFoundException {
        c = getConnection();
    }

    @Override
    public void createStakeholder(String stakeholderid, float threshold, long last_id_cluster) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();

        try {
            PreparedStatement ps;

            ps = c.prepareStatement("INSERT INTO stakeholders (id, threshold, last_id_cluster) VALUES (?, ?, ?)");
            ps.setString(1,stakeholderid);
            ps.setFloat(2,threshold);
            ps.setLong(3,last_id_cluster);
            ps.execute();
        } finally {
            //c.close();
        }

    }

    @Override
    public boolean existStakeholder(String stakeholderid) throws SQLException, ClassNotFoundException {

        PreparedStatement ps;
        ps = c.prepareStatement("SELECT COUNT(*) FROM stakeholders WHERE id = ?");
        ps.setString(1, stakeholderid);
        ps.execute();
        ResultSet rs = ps.getResultSet();
        rs.next();
        int count = rs.getInt(1);

        return (count == 1);

    }

    @Override
    public void savePreprocessed(Requirement r, String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();


        PreparedStatement ps;
        ps = c.prepareStatement("DELETE FROM prepocessed WHERE id = ? AND stakeholderid = ?");
        ps.setString(1, r.getId());
        ps.setString(2, stakeholderid);
        ps.execute();

        ps = c.prepareStatement ("INSERT INTO prepocessed (id, created_at, name, text, sentence_name, sentence_text, clusterid, master, stakeholderid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        ps.setString(1, r.getId());
        if (!(r.getCreated_at() == null)) ps.setLong(2,r.getCreated_at());
        if (r.getName() == null) {
            ps.setInt(3,0);
            ps.setString(5,"");
        } else {
            ps.setInt(3,1);
            ps.setString(5,sentence2JSON(r.getSentence_name()).toString());
        }
        if (r.getText() == null) {
            ps.setInt(4,0);
            ps.setString(6,"");
        } else {
            ps.setInt(4,1);
            ps.setString(6,sentence2JSON(r.getSentence_text()).toString());
        }
        if (r.getCluster() != null) {
            ps.setLong(7,r.getCluster().getClusterid());
            ps.setBoolean(8,r.isMaster());
        }
        ps.setString(9, stakeholderid);
        ps.execute();
    }

    @Override
    public void updateThreshold(String stakeholder, float threshold) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();

        try {
            PreparedStatement ps;
            ps = c.prepareStatement("UPDATE stakeholders SET threshold = ? WHERE id = ?");
            ps.setFloat(1, threshold);
            ps.setString(2, stakeholder);

            ps.execute();
        } finally {
            //c.close();
        }
    }

    @Override
    public void updateRequirementCluster(Requirement requirement, String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();

        try {
            PreparedStatement ps;
            ps = c.prepareStatement("UPDATE prepocessed SET master = ?, clusterid = ? WHERE id = ? AND stakeholderid = ?");
            ps.setBoolean(1,requirement.isMaster());
            ps.setLong(2,requirement.getCluster().getClusterid());
            ps.setString(3,requirement.getId());
            ps.setString(4,stakeholderid);
            ps.execute();
        } finally {
            //c.close();
        }

    }

    @Override
    public void updateLastClusterId(long new_value, String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();

        try {
            PreparedStatement ps;
            ps = c.prepareStatement("UPDATE stakeholders SET last_id_cluster = ? WHERE id = ?");
            ps.setLong(1,new_value);
            ps.setString(2,stakeholderid);
            ps.execute();
        } finally {
            //c.close();
        }
    }



    @Override
    public void saveDependency(Dependency dependency, boolean accepted, String stakeholderid) throws SQLException, ClassNotFoundException {

        if (c == null) c = getConnection();


        try {
            PreparedStatement ps;
            /*ps = c.prepareStatement("DELETE FROM dependencies WHERE id = ?");
            ps.setInt(1, cluster.getClusterid());
            ps.execute();*/

            ps = c.prepareStatement("INSERT INTO dependencies (fromid, toid, accepted, stakeholderid) VALUES (?, ?, ?, ?)");
            ps.setString(1,dependency.getFromid());
            ps.setString(2,dependency.getToid());
            ps.setBoolean(3,accepted);
            ps.setString(4,stakeholderid);
            ps.execute();
        } finally {
            //c.close();
        }
    }

    @Override
    public long getLastClusterId(String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();

        PreparedStatement ps;
        ps = c.prepareStatement("SELECT last_id_cluster FROM stakeholders WHERE id = ?");
        ps.setString(1,stakeholderid);
        ps.execute();
        ResultSet rs = ps.getResultSet();

        if (rs.next()) {
            return rs.getLong("last_id_cluster");
        } else {
            throw new SQLException("Stakeholder with id " + stakeholderid + " does not exist in DB");
        }

    }

    @Override
    public float getThreshold(String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();
        PreparedStatement ps;
        ps = c.prepareStatement("SELECT threshold FROM stakeholders WHERE id = ?");
        ps.setString(1,stakeholderid);
        ps.execute();
        ResultSet rs = ps.getResultSet();

        if (rs.next()) {
            return rs.getFloat("threshold");
        } else {
            throw new SQLException("Stakeholder with id " + stakeholderid + " does not exist in DB");
        }
    }

    @Override
    public Requirement getRequirement(String id_aux, String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();
        PreparedStatement ps;
        ps = c.prepareStatement("SELECT id, clusterid, master, created_at, name, text, sentence_name, sentence_text FROM prepocessed WHERE id = ? AND stakeholderid = ?");
        ps.setString(1,id_aux);
        ps.setString(2,stakeholderid);
        ps.execute();
        ResultSet rs = ps.getResultSet();

        if (rs.next()) {
            String id = rs.getString("id");
            long clusterid = rs.getLong("clusterid");
            boolean master = rs.getBoolean("master");
            Long created_at = rs.getLong("created_at");
            int name = rs.getInt("name");
            int text = rs.getInt("text");
            Sentence sentence_name = null;
            Sentence sentence_text = null;
            if (name == 1) sentence_name = JSON2Sentence(rs.getString("sentence_name"));
            if (text == 1) sentence_text = JSON2Sentence(rs.getString("sentence_text"));
            return new Requirement(id,clusterid,master,name,text,sentence_name,sentence_text,created_at);
        }
        else {
            throw new SQLException("The requirement with id " + id_aux + " does not exist in the database");
        }
    }

    @Override
    public List<String> getClusterRequirementsId(long cluster_id, String stakeholderid) throws SQLException, ClassNotFoundException {

        if (c == null) c = getConnection();
        PreparedStatement ps;
        ps = c.prepareStatement("SELECT id FROM prepocessed WHERE clusterid = ? AND stakeholderid = ?");
        ps.setLong(1,cluster_id);
        ps.setString(2,stakeholderid);
        ps.execute();
        ResultSet rs = ps.getResultSet();

        List<String> result = new ArrayList<>();
        while (rs.next()) {
            result.add(rs.getString("id"));
        }

        return result;
    }


    @Override
    public Dependency getDependency(String fromid, String toid, String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();
        PreparedStatement ps;
        ps = c.prepareStatement("SELECT accepted FROM dependencies WHERE ((fromid = ? AND toid = ?) OR (toid = ? AND fromid = ?)) AND stakeholderid = ?");
        ps.setString(1,fromid);
        ps.setString(2,toid);
        ps.setString(3,fromid);
        ps.setString(4,toid);
        ps.setString(5,stakeholderid);
        ps.execute();
        ResultSet rs = ps.getResultSet();

        if (rs.next()) {
            boolean accepted = rs.getBoolean("accepted");
            String status = "accepted";
            if (!accepted) status = "rejected";
            Dependency dependency = new Dependency(fromid,toid,status,"duplicates");
            return dependency;
        }
        else {
            throw new SQLException("The dependency does not exist in the database");
        }
    }

    @Override
    public List<Requirement> getRequirements(String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();

        PreparedStatement ps;

        try {
            ps = c.prepareStatement("SELECT id, clusterid, master, created_at, name, text, sentence_name, sentence_text FROM prepocessed WHERE stakeholderid = ?");
            ps.setString(1, stakeholderid);
            ps.execute();
            ResultSet rs = ps.getResultSet();

            List<Requirement> result = new ArrayList<>();

            while (rs.next()) {
                String id = rs.getString("id");
                Long created_at = rs.getLong("created_at");
                long cluster = rs.getLong("clusterid");
                boolean master = rs.getBoolean("master");
                int name = rs.getInt("name");
                int text = rs.getInt("text");
                Sentence sentence_name = null;
                Sentence sentence_text = null;
                if (name == 1) sentence_name = JSON2Sentence(rs.getString("sentence_name"));
                if (text == 1) sentence_text = JSON2Sentence(rs.getString("sentence_text"));
                Requirement aux = new Requirement(id,cluster,master,name,text,sentence_name,sentence_text,created_at);
                result.add(aux);
            }

            return result;
        } finally {
            //c.close();
        }
    }

    public String getRequirements_JSON(String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();

        PreparedStatement ps;

        try {
            //GROUP BY clusterid
            ps = c.prepareStatement("SELECT clusterid, COUNT(*) FROM prepocessed WHERE stakeholderid = ? GROUP BY clusterid");
            ps.setString(1, stakeholderid);
            ps.execute();
            ResultSet rs = ps.getResultSet();

            JSONObject result = new JSONObject();
            JSONArray array = new JSONArray();

            while (rs.next()) {
                JSONObject aux = new JSONObject();
                int uno = rs.getInt(1);
                int dos = rs.getInt(2);
                aux.put("cluster",uno);
                aux.put("number",dos);
                array.put(aux);
            }
            result.put("clusters", array);
            return result.toString();
        } finally {
            //c.close();
        }
    }

    @Override
    public List<Dependency> getDependencies(String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();

        PreparedStatement ps;

        try {
            ps = c.prepareStatement("SELECT fromid, toid, accepted FROM dependencies WHERE stakeholderid = ?");
            ps.setString(1, stakeholderid);
            ps.execute();
            ResultSet rs = ps.getResultSet();

            List<Dependency> result = new ArrayList<>();

            while (rs.next()) {
                String fromid = rs.getString("fromid");
                String toid = rs.getString("toid");
                boolean accepted = rs.getBoolean("accepted");
                String status = "accepted";
                if (!accepted) status = "rejected";
                result.add(new Dependency(fromid,toid,status,"duplicates"));
            }

            return result;
        } finally {
            //c.close();
        }
    }

    @Override
    public List<Stakeholder> getStakeholders() throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();

        PreparedStatement ps;

        try {
            ps = c.prepareStatement("SELECT id, threshold FROM stakeholders");
            ps.execute();
            ResultSet rs = ps.getResultSet();

            List<Stakeholder> result = new ArrayList<>();

            while (rs.next()) {
                String id = rs.getString("id");
                float threshold = rs.getFloat("threshold");
                result.add(new Stakeholder(id,threshold));
            }

            return result;
        } finally {
            //c.close();
        }
    }

    @Override
    public void deleteRequirement(Requirement requirement, String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();
        PreparedStatement ps;

        ps = c.prepareStatement("DELETE FROM prepocessed WHERE id = ? AND stakeholderid = ?");
        ps.setString(1,requirement.getId());
        ps.setString(2, stakeholderid);
        ps.execute();
    }

    @Override
    public void deleteDependency(Dependency dependency, String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();
        PreparedStatement ps;

        ps = c.prepareStatement("DELETE FROM dependencies WHERE fromid = ? AND toid = ? AND stakeholderid = ?");
        ps.setString(1, dependency.getFromid());
        ps.setString(2, dependency.getToid());
        ps.setString(3, stakeholderid);
        ps.execute();

        ps = c.prepareStatement("DELETE FROM dependencies WHERE fromid = ? AND toid = ? AND stakeholderid = ?");
        ps.setString(1, dependency.getToid());
        ps.setString(2, dependency.getFromid());
        ps.setString(3, stakeholderid);
        ps.execute();
    }

    @Override
    public void deleteRequirementDependencies(Requirement requirement, String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();
        PreparedStatement ps;
        ps = c.prepareStatement("DELETE FROM dependencies WHERE (fromid = ? OR toid = ?) AND stakeholderid = ?");
        ps.setString(1, requirement.getId());
        ps.setString(2, requirement.getId());
        ps.setString(3, stakeholderid);
        ps.execute();

    }


    @Override
    public void clearDB() throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();
        PreparedStatement ps;
        try {
            ps = c.prepareStatement("DELETE FROM dependencies");
            ps.execute();
            ps = c.prepareStatement("DELETE FROM prepocessed");
            ps.execute();
            ps = c.prepareStatement("DELETE FROM stakeholders");
            ps.execute();

        } finally {
            //c.close();
        }
    }

    @Override
    public void resetStakeholder(String stakeholderid) throws SQLException, ClassNotFoundException {
        if (c == null) c = getConnection();
        PreparedStatement ps;
        try {
            ps = c.prepareStatement("DELETE FROM dependencies WHERE stakeholderid = ?");
            ps.setString(1,stakeholderid);
            ps.execute();
            ps = c.prepareStatement("DELETE FROM prepocessed WHERE stakeholderid = ?");
            ps.setString(1,stakeholderid);
            ps.execute();

        } finally {
            //c.close();
        }
    }


    // Conversion Sentence <=> JSON

    private JSONObject sentence2JSON(Sentence sentence) {

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("rawForm", sentence.getRawForm());
            jsonObject.put("words", words2JSON(sentence.getWords()));
            jsonObject.put("dependencies", dependencies2JSON(sentence.getDependencies()));
            jsonObject.put("syntacticTreeString", sentence.getSyntacticTreeString());
            jsonObject.put("dependencyTreeString", sentence.getDependencyTreeString());
            jsonObject.put("collocations", new JSONObject(sentence.getCollocations()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  jsonObject;
    }

    private Sentence JSON2Sentence(String sentence) {

        
        Sentence s = new Sentence();
        
        try {
			JSONObject jsonObject = new JSONObject(sentence);

			s.setRawForm(jsonObject.getString("rawForm"));
			s.setWords(JSON2Words(jsonObject.getJSONArray("words")));
			s.setDependencies(JSON2Dependencies(jsonObject.getJSONArray("dependencies")));
			s.setSyntacticTreeString(jsonObject.getString("syntacticTreeString"));
//			s.setDependencyTreeString(jsonObject.getString("dependencyTreeString"));
			s.setCollocations(JSON2Collocations(jsonObject.getJSONObject("collocations")));
		} catch (JSONException e) {
			e.printStackTrace();
		}

        return  s;
    }

    private Hashtable<Integer,Integer> JSON2Collocations(JSONObject collocations) {

        Hashtable<Integer, Integer> c = new Hashtable<>();

        JSONArray jsonArray = collocations.names();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
					int key = jsonArray.getInt(i);
					int value = collocations.getInt("" + key);
					c.put(key, value);
				} catch (JSONException e) {
					e.printStackTrace();
				}
            }
        }

        return c;
    }

    private JSONArray words2JSON(ArrayList<Word> words) {

        JSONArray jsonArray = new JSONArray();
        for (Word w : words) jsonArray.put(word2JSON(w));
        return  jsonArray;
    }

    private ArrayList<Word> JSON2Words(JSONArray words) {

        ArrayList<Word> ws = new ArrayList<>();
        for (int i = 0; i < words.length(); i++)
			try {
				ws.add(JSON2Word(words.getJSONObject(i)));
			} catch (JSONException e) {
				e.printStackTrace();
			}
        return ws;
    }

    private JSONObject word2JSON(Word w) {

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("rawForm", w.getRawForm());
            jsonObject.put("baseForm", w.getBaseForm());
            jsonObject.put("pos", w.getPos());
            jsonObject.put("sentenceIndex", w.getSentenceIndex());
            jsonObject.put("isStopWord", w.isIsStopWord());
            jsonObject.put("isPunctuaton", w.isIsPunctuaton());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    private Word JSON2Word(JSONObject jsonObject) {

        Word word = new Word();

        try {
			word.getBaseForm();
			word.setRawForm(jsonObject.getString("rawForm"));
			word.setBaseForm(jsonObject.getString("baseForm"));
			word.setPos(jsonObject.getString("pos"));
			word.setSentenceIndex(jsonObject.getInt("sentenceIndex"));
			word.setIsStopWord(jsonObject.getBoolean("isStopWord"));
			word.setIsPunctuaton(jsonObject.getBoolean("isPunctuaton"));
		} catch (JSONException e) {
			e.printStackTrace();
		}

        return word;
    }

    private JSONArray dependencies2JSON(ArrayList<DependencyStructure> dependencies) {

        JSONArray jsonArray = new JSONArray();
//        for (DependencyStructure d : dependencies) jsonArray.put(dependency2JSON(d));
        return  jsonArray;
    }

    private ArrayList<DependencyStructure> JSON2Dependencies(JSONArray dependencies) {

        ArrayList<DependencyStructure> ds = new ArrayList<>();
        for (int i = 0; i < dependencies.length(); i++)
			try {
				ds.add(JSON2Dependency(dependencies.getJSONObject(i)));
			} catch (JSONException e) {
				e.printStackTrace();
			}
        return ds;
    }

    private JSONObject dependency2JSON(DependencyStructure d) {

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("type", d.getType());
            jsonObject.put("head", d.getHead());
            jsonObject.put("modifier", d.getModifier());
            jsonObject.put("strHead", d.getStrHead());
            jsonObject.put("strModifier", d.getStrModifier());
            jsonObject.put("strPoshead", d.getStrPoshead());
            jsonObject.put("strPosModifier", d.getStrPosModifier());
            jsonObject.put("depthInTree", d.getDepthInTree());
            jsonObject.put("sentenceIndex", d.getSentenceIndex());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  jsonObject;
    }

    private DependencyStructure JSON2Dependency(JSONObject jsonObject) {

        DependencyStructure d = new DependencyStructure();

        try {
			d.setType(jsonObject.getString("type"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
        try {
			d.setHead(jsonObject.getInt("head"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
        try {
			d.setModifier(jsonObject.getInt("modifier"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
        try {
            d.setStrHead(jsonObject.getString("strHead"));
        } catch (JSONException e) {
            //e.printStackTrace();
        }
        try {
            d.setStrModifier(jsonObject.getString("strModifier"));
        } catch (JSONException e) {
            //e.printStackTrace();
        }
        try {
            d.setStrPoshead(jsonObject.getString("strPoshead"));
        } catch (JSONException e) {
            //e.printStackTrace();
        }
        try {
            d.setStrPosModifier(jsonObject.getString("strPosModifier"));
        } catch (JSONException e) {
            //e.printStackTrace();
        }
        try {
			d.setDepthInTree(jsonObject.getInt("depthInTree"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
        try {
			d.setSentenceIndex(jsonObject.getInt("sentenceIndex"));
		} catch (JSONException e) {
			e.printStackTrace();
		}

        return d;
    }
}
