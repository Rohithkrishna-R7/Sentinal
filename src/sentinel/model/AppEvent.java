package sentinel.model;

import java.time.Instant;

public class AppEvent {
    private final Instant timestamp;
    private final String eventType;
    private final String description;
    private final String source;
    public AppEvent(Instant timestamp,String eventType,String description,String source){
        this.timestamp=timestamp;
        this.eventType=eventType;
        this.description=description;
        this.source=source;
    }
    public Instant getTimestamp(){
        return timestamp;
    }
    public String getEventType(){
        return eventType;
    }
    public String getDescription(){
        return description;
    }
    public String getSource(){
        return source;
    }
    @Override
    public String toString(){
        return "AppEvent{"+eventType+", "+source+", "+timestamp+"}";
    }
}
