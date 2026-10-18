package sentinel.monitor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import sentinel.model.AppEvent;

public class ProcessMonitor {
    private static final int POLL_PERIOD_SECONDS=2;

    public interface Callback {
        void onEvent(AppEvent event);
        void onError(String message);
    }

    private final Callback callback;
    private final Set<Long> previousPids=new HashSet<>();
    private boolean baselineTaken=false;
    private ScheduledExecutorService executor;

    public ProcessMonitor(Callback callback){
        this.callback=callback;
    }
    public void start(){
        baselineTaken=false;
        previousPids.clear();
        executor=Executors.newSingleThreadScheduledExecutor(r->{
            Thread t=new Thread(r,"sentinel-process-monitor");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::pollProcesses,0,POLL_PERIOD_SECONDS,TimeUnit.SECONDS);
    }
    public void stop(){
        if(executor!=null){
            executor.shutdownNow();
            executor=null;
        }
    }
    private void pollProcesses(){
        try{
            Set<Long> currentPids=new HashSet<>();
            ProcessHandle.allProcesses().forEach(ph->currentPids.add(ph.pid()));
            if(!baselineTaken){
                previousPids.addAll(currentPids);
                baselineTaken=true;
                return;
            }
            for(long pid:currentPids){
                if(!previousPids.contains(pid)){
                    emitProcessEvent(pid);
                }
            }
            previousPids.clear();
            previousPids.addAll(currentPids);
        }catch(Exception e){
            callback.onError("ProcessMonitor error: "+e.getMessage());
        }
    }
    private void emitProcessEvent(long pid){
        Optional<ProcessHandle> handleOpt=ProcessHandle.of(pid);
        if(handleOpt.isEmpty()){
            return;
        }
        ProcessHandle handle=handleOpt.get();
        ProcessHandle.Info info=handle.info();
        String command=info.command().orElse("unknown");
        String source=command.equals("unknown")?"pid:"+pid:command;
        String description="New process started: PID "+pid+", command: "+command;
        AppEvent event=new AppEvent(
                Instant.now(),
                "PROCESS_STARTED",
                description,
                source);
        callback.onEvent(event);
    }
}
