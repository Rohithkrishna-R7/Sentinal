package sentinel.model;

import java.time.Instant;

public class ThreatAlert {
    private final Instant timestamp;
    private final int score;
    private final Severity severity;
    private final String rule;
    private final String explanation;
    public ThreatAlert(Instant timestamp,int score,Severity severity,String rule,String explanation){
        this.timestamp=timestamp;
        this.score=score;
        this.severity=severity;
        this.rule=rule;
        this.explanation=explanation;
    }
    public Instant getTimestamp(){
        return timestamp;
    }
    public int getScore(){
        return score;
    }
    public Severity getSeverity(){
        return severity;
    }
    public String getRule(){
        return rule;
    }
    public String getExplanation(){
        return explanation;
    }
    @Override
    public String toString(){
        return "ThreatAlert{"+rule+", score="+score+", severity="+severity+"}";
    }
}
