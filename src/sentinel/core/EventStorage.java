package sentinel.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import sentinel.model.AppEvent;
import sentinel.model.ThreatAlert;

public class EventStorage {
    private static final String CSV_HEADER="timestamp,recordType,eventType,description,source,riskScore,severity";
    private static final int COLUMN_COUNT=7;
    private final String csvPath;
    private final Consumer<String> errorCallback;
    private boolean writable=true;

    public EventStorage(String csvPath,Consumer<String> errorCallback){
        this.csvPath=csvPath;
        this.errorCallback=errorCallback;
        ensureFileExists();
    }
    public void writeEvent(AppEvent event){
        if(!writable){
            return;
        }
        String[] row={
            event.getTimestamp().toString(),
            "EVENT",
            event.getEventType(),
            event.getDescription(),
            event.getSource(),
            "",
            ""
        };
        appendRow(row);
    }
    public void writeThreat(ThreatAlert alert,AppEvent sourceEvent){
        if(!writable){
            return;
        }
        String[] row={
            alert.getTimestamp().toString(),
            "THREAT",
            sourceEvent.getEventType(),
            alert.getExplanation(),
            sourceEvent.getSource(),
            String.valueOf(alert.getScore()),
            alert.getSeverity().name()
        };
        appendRow(row);
    }
    public List<String[]> readAll(){
        List<String[]> result=new ArrayList<>();
        File file=new File(csvPath);
        if(!file.exists()){
            return result;
        }
        try(BufferedReader reader=new BufferedReader(new FileReader(file,java.nio.charset.StandardCharsets.UTF_8))){
            String line;
            boolean firstLine=true;
            int lineNumber=0;
            while((line=reader.readLine())!=null){
                lineNumber++;
                if(firstLine){
                    firstLine=false;
                    continue;
                }
                if(line.trim().isEmpty()){
                    continue;
                }
                String[] fields=parseCsvLine(line);
                if(fields==null||fields.length!=COLUMN_COUNT){
                    System.err.println("[EventStorage] Skipping malformed line "+lineNumber+": "+line);
                    continue;
                }
                result.add(fields);
            }
        }catch(IOException e){
            System.err.println("[EventStorage] Could not read CSV: "+e.getMessage());
        }
        return result;
    }
    private void ensureFileExists(){
        File file=new File(csvPath);
        File parent=file.getParentFile();
        if(parent!=null&&!parent.exists()){
            parent.mkdirs();
        }
        if(!file.exists()){
            try(BufferedWriter writer=new BufferedWriter(new FileWriter(file,java.nio.charset.StandardCharsets.UTF_8,false))){
                writer.write(CSV_HEADER);
                writer.newLine();
            }catch(IOException e){
                writable=false;
                errorCallback.accept("Cannot create CSV file: "+e.getMessage());
            }
        }
    }
    private void appendRow(String[] fields){
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<fields.length;i++){
            if(i>0){
                sb.append(',');
            }
            sb.append(quoteCsv(fields[i]));
        }
        File file=new File(csvPath);
        try(BufferedWriter writer=new BufferedWriter(new FileWriter(file,java.nio.charset.StandardCharsets.UTF_8,true))){
            writer.write(sb.toString());
            writer.newLine();
        }catch(IOException e){
            writable=false;
            errorCallback.accept("Cannot write to CSV: "+e.getMessage());
        }
    }
    private String quoteCsv(String field){
        if(field==null){
            return "";
        }
        boolean needsQuotes=field.contains(",")||field.contains("\"")||field.contains("\n")||field.contains("\r");
        if(!needsQuotes){
            return field;
        }
        String escaped=field.replace("\"","\"\"");
        return "\""+escaped+"\"";
    }
    String[] parseCsvLine(String line){
        List<String> fields=new ArrayList<>();
        int i=0;
        int len=line.length();
        if(len==0){
            return new String[0];
        }
        while(i<=len){
            if(i==len){
                fields.add("");
                break;
            }
            if(line.charAt(i)=='"'){
                i++;
                StringBuilder sb=new StringBuilder();
                boolean closed=false;
                while(i<len){
                    char c=line.charAt(i);
                    if(c=='"'){
                        if(i+1<len&&line.charAt(i+1)=='"'){
                            sb.append('"');
                            i+=2;
                        }else{
                            i++;
                            closed=true;
                            break;
                        }
                    }else{
                        sb.append(c);
                        i++;
                    }
                }
                if(!closed){
                    return null;
                }
                fields.add(sb.toString());
                if(i<len){
                    if(line.charAt(i)==','){
                        i++;
                    }else{
                        return null;
                    }
                }else{
                    break;
                }
            }else{
                int start=i;
                while(i<len&&line.charAt(i)!=','){
                    i++;
                }
                fields.add(line.substring(start,i));
                if(i<len){
                    i++;
                }else{
                    break;
                }
            }
        }
        return fields.toArray(new String[0]);
    }
}
