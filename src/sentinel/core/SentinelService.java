package sentinel.core;

import java.nio.file.Path;
import java.util.List;
import sentinel.model.AppEvent;
import sentinel.model.ThreatAlert;
import sentinel.monitor.FileMonitor;
import sentinel.monitor.ProcessMonitor;

public class SentinelService {
    public interface Listener {
        void onEvent(AppEvent event);
        void onThreat(ThreatAlert alert,AppEvent sourceEvent);
        void onError(String message);
        void onStatusChange(String status);
    }
    private final EventStorage storage;
    private final ThreatDetector detector;
    private Listener listener;
    private final Object lock=new Object();
    private boolean running=false;
    private FileMonitor fileMonitor;
    private ProcessMonitor processMonitor;

    public SentinelService(EventStorage storage){
        this.storage=storage;
        this.detector=new ThreatDetector();
    }
    public void setListener(Listener listener){
        this.listener=listener;
    }
    public void start(Path directory){
        synchronized(lock){
            if(running){
                throw new IllegalStateException("SentinelService is already running.");
            }
            detector.reset();
            FileMonitor.Callback fileCallback=new FileMonitor.Callback(){
                @Override
                public void onEvent(AppEvent event){
                    handleEvent(event);
                }
                @Override
                public void onError(String message){
                    dispatchError(message);
                }
            };
            ProcessMonitor.Callback procCallback=new ProcessMonitor.Callback(){
                @Override
                public void onEvent(AppEvent event){
                    handleEvent(event);
                }
                @Override
                public void onError(String message){
                    dispatchError(message);
                }
            };
            fileMonitor=new FileMonitor(directory,fileCallback);
            processMonitor=new ProcessMonitor(procCallback);
            fileMonitor.start();
            processMonitor.start();
            running=true;
        }
        dispatchStatusChange("Running");
    }
    public void stop(){
        synchronized(lock){
            if(!running){
                return;
            }
            if(fileMonitor!=null){
                fileMonitor.stop();
                fileMonitor=null;
            }
            if(processMonitor!=null){
                processMonitor.stop();
                processMonitor=null;
            }
            running=false;
        }
        dispatchStatusChange("Stopped");
    }
    public boolean isRunning(){
        synchronized(lock){
            return running;
        }
    }
    public int getSessionRiskScore(){
        return detector.getSessionRiskScore();
    }
    private void handleEvent(AppEvent event){
        storage.writeEvent(event);
        dispatchEvent(event);
        List<ThreatAlert> alerts=detector.evaluate(event);
        for(ThreatAlert alert:alerts){
            storage.writeThreat(alert,event);
            dispatchThreat(alert,event);
        }
    }
    private void dispatchEvent(AppEvent event){
        if(listener!=null){
            listener.onEvent(event);
        }
    }
    private void dispatchThreat(ThreatAlert alert,AppEvent source){
        if(listener!=null){
            listener.onThreat(alert,source);
        }
    }
    private void dispatchError(String message){
        if(listener!=null){
            listener.onError(message);
        }
    }
    private void dispatchStatusChange(String status){
        if(listener!=null){
            listener.onStatusChange(status);
        }
    }
}
