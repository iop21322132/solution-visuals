package farvix.solution.api.config;

public class Config {
    public String name;
    public long lastModified;
    
    public Config(String name) {
        this.name = name;
        this.lastModified = System.currentTimeMillis();
    }
}
