package upc.similarity.semilarapi.entity;

public class Stakeholder {

    private String id;
    private float threshold;

    public Stakeholder(String id, float threshold) {
        this.id = id;
        this.threshold = threshold;
    }

    public String getId() {
        return id;
    }

    public float getThreshold() {
        return threshold;
    }
}
