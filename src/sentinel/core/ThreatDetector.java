package sentinel.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import sentinel.model.AppEvent;
import sentinel.model.Severity;
import sentinel.model.ThreatAlert;

public class ThreatDetector {
    static final String[] EXECUTABLE_EXTENSIONS={".exe",".bat",".cmd",".ps1",".msi",".scr",".vbs",".jar"};
    static final int RULE_A_SCORE=40;
    static final String[] SUSPICIOUS_PATTERNS={"sentinel-demo"};
    static final int RULE_B_SCORE=70;
    static final int BURST_THRESHOLD=5;
    static final long BURST_WINDOW_MS=10_000L;
    static final long BURST_COOLDOWN_MS=10_000L;
    static final int RULE_C_SCORE=60;
    static final int LOW_MAX=25;
    static final int MEDIUM_MAX=50;
    static final int HIGH_MAX=75;

    private final List<Long> recentBurstTimestamps=new ArrayList<>();
    private long lastBurstAlertTime=0L;
    private int sessionRiskScore=0;

    public List<ThreatAlert> evaluate(AppEvent event){
        List<ThreatAlert> alerts=new ArrayList<>();
        String type=event.getEventType();
        if("FILE_CREATED".equals(type)){
            ThreatAlert a=checkRuleA(event);
            if(a!=null){
                alerts.add(a);
            }
        }
        if("PROCESS_STARTED".equals(type)){
            ThreatAlert b=checkRuleB(event);
            if(b!=null){
                alerts.add(b);
            }
        }
        if("FILE_MODIFIED".equals(type)||"FILE_DELETED".equals(type)){
            ThreatAlert c=checkRuleC(event);
            if(c!=null){
                alerts.add(c);
            }
        }
        for(ThreatAlert alert:alerts){
            if(alert.getScore()>sessionRiskScore){
                sessionRiskScore=alert.getScore();
            }
        }
        return alerts;
    }
    public int getSessionRiskScore(){
        return sessionRiskScore;
    }
    public void reset(){
        recentBurstTimestamps.clear();
        lastBurstAlertTime=0L;
        sessionRiskScore=0;
    }
    private ThreatAlert checkRuleA(AppEvent event){
        String source=event.getSource();
        String filename=extractFilename(source);
        if(!endsWithExecutableExtension(filename)){
            return null;
        }
        int score=clamp(RULE_A_SCORE);
        Severity severity=Severity.forScore(score);
        String explanation="Executable-type file created: "+filename+" – requires review";
        return new ThreatAlert(event.getTimestamp(),score,severity,"RULE_A",explanation);
    }
    private ThreatAlert checkRuleB(AppEvent event){
        String source=event.getSource();
        String matchedPattern=findMatchingPattern(source);
        if(matchedPattern==null){
            return null;
        }
        int score=clamp(RULE_B_SCORE);
        Severity severity=Severity.forScore(score);
        String explanation="Suspicious process detected matching pattern '"+matchedPattern+"': "+source;
        return new ThreatAlert(event.getTimestamp(),score,severity,"RULE_B",explanation);
    }
    private ThreatAlert checkRuleC(AppEvent event){
        long now=System.currentTimeMillis();
        recentBurstTimestamps.add(now);
        int i=0;
        while(i<recentBurstTimestamps.size()){
            if(now-recentBurstTimestamps.get(i)>BURST_WINDOW_MS){
                recentBurstTimestamps.remove(i);
            }else{
                i++;
            }
        }
        if(recentBurstTimestamps.size()<BURST_THRESHOLD){
            return null;
        }
        if(lastBurstAlertTime>0&&(now-lastBurstAlertTime)<BURST_COOLDOWN_MS){
            return null;
        }
        lastBurstAlertTime=now;
        int count=recentBurstTimestamps.size();
        recentBurstTimestamps.clear();
        int score=clamp(RULE_C_SCORE);
        Severity severity=Severity.forScore(score);
        String explanation="Rapid file change burst: "+count+" events in "+(BURST_WINDOW_MS/1000)+"s – possible ransomware activity";
        return new ThreatAlert(Instant.now(),score,severity,"RULE_C",explanation);
    }
    private int clamp(int score){
        if(score<0){return 0;}
        if(score>100){return 100;}
        return score;
    }
    private boolean endsWithExecutableExtension(String filename){
        if(filename==null||filename.isEmpty()){
            return false;
        }
        String lower=filename.toLowerCase();
        for(String ext:EXECUTABLE_EXTENSIONS){
            if(lower.endsWith(ext)){
                return true;
            }
        }
        return false;
    }
    private String findMatchingPattern(String source){
        if(source==null||source.isEmpty()){
            return null;
        }
        String lower=source.toLowerCase();
        for(String pattern:SUSPICIOUS_PATTERNS){
            if(lower.contains(pattern.toLowerCase())){
                return pattern;
            }
        }
        return null;
    }
    private String extractFilename(String path){
        if(path==null){
            return "";
        }
        int lastSlash=Math.max(path.lastIndexOf('/'),path.lastIndexOf('\\'));
        return path.substring(lastSlash+1);
    }
}
