package sentinel.model;

public enum Severity {
    LOW(0,25),
    MEDIUM(26,50),
    HIGH(51,75),
    CRITICAL(76,100);
    private final int minScore;
    private final int maxScore;
    Severity(int minScore,int maxScore){
        this.minScore=minScore;
        this.maxScore=maxScore;
    }
    public int getMinScore(){
        return minScore;
    }
    public int getMaxScore(){
        return maxScore;
    }
    public static Severity forScore(int score){
        for(Severity s:values()){
            if(score>=s.minScore&&score<=s.maxScore){
                return s;
            }
        }
        return CRITICAL;
    }
}
