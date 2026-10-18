package sentinel.monitor;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import sentinel.model.AppEvent;

public class FileMonitor {
    private static final long POLL_TIMEOUT_MS=500L;
    private static final long DEDUP_WINDOW_MS=500L;

    public interface Callback {
        void onEvent(AppEvent event);
        void onError(String message);
    }

    private final Path directory;
    private final Callback callback;
    private volatile boolean running=false;
    private WatchService watchService;
    private WatchKey watchKey;
    private final Map<String,Long> lastFired=new HashMap<>();

    public FileMonitor(Path directory,Callback callback){
        this.directory=directory;
        this.callback=callback;
    }
    public void start(){
        running=true;
        Thread thread=new Thread(this::watchLoop,"sentinel-file-monitor");
        thread.setDaemon(true);
        thread.start();
    }
    public void stop(){
        running=false;
        closeWatchService();
    }
    private void watchLoop(){
        try{
            watchService=FileSystems.getDefault().newWatchService();
            watchKey=directory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
        }catch(IOException e){
            callback.onError("Cannot start file monitor: "+e.getMessage());
            running=false;
            return;
        }
        while(running){
            WatchKey key;
            try{
                key=watchService.poll(POLL_TIMEOUT_MS,TimeUnit.MILLISECONDS);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }catch(ClosedWatchServiceException e){
                break;
            }
            if(key==null){
                continue;
            }
            List<WatchEvent<?>> events=key.pollEvents();
            for(WatchEvent<?> event:events){
                processWatchEvent(event);
            }
            boolean valid=key.reset();
            if(!valid){
                callback.onError("Watched directory is no longer accessible. Monitoring stopped.");
                running=false;
                break;
            }
        }
        closeWatchService();
    }
    private void processWatchEvent(WatchEvent<?> event){
        WatchEvent.Kind<?> kind=event.kind();
        if(kind==StandardWatchEventKinds.OVERFLOW){
            callback.onError("WatchService OVERFLOW – some file events may have been missed.");
            return;
        }
        @SuppressWarnings("unchecked")
        WatchEvent<Path> pathEvent=(WatchEvent<Path>)event;
        Path relativePath=pathEvent.context();
        Path fullPath=directory.resolve(relativePath);
        String eventType;
        if(kind==StandardWatchEventKinds.ENTRY_CREATE){
            eventType="FILE_CREATED";
        }else if(kind==StandardWatchEventKinds.ENTRY_MODIFY){
            eventType="FILE_MODIFIED";
        }else if(kind==StandardWatchEventKinds.ENTRY_DELETE){
            eventType="FILE_DELETED";
        }else{
            return;
        }
        String dedupKey=fullPath.toString()+"|"+eventType;
        long now=System.currentTimeMillis();
        Long last=lastFired.get(dedupKey);
        if(last!=null&&(now-last)<DEDUP_WINDOW_MS){
            return;
        }
        lastFired.put(dedupKey,now);
        String description=eventType.replace("_"," ").toLowerCase().replace("file ","File ")+": "+fullPath.getFileName();
        AppEvent appEvent=new AppEvent(
                Instant.now(),
                eventType,
                description,
                fullPath.toString());
        callback.onEvent(appEvent);
    }
    private void closeWatchService(){
        if(watchKey!=null){
            watchKey.cancel();
        }
        if(watchService!=null){
            try{
                watchService.close();
            }catch(IOException e){
            }
        }
    }
}
