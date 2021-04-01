package zstateless;

import Server.OperationManager;
import commands.StopProgramm;
import interfaces.NetworkFunction;
import interfaces.stepControl.NextStepTask;
import interfaces.stepControl.ProcessCondition;
import interfaces.stepControl.ProcessControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import commands.TraceLoad;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MoveStatelessControl implements ProcessControl, ProcessCondition, Runnable {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final int firstNumInstances = 1;
    private final int secondNumInstances = 2;
    private final int numInstances = 2;
    private Map<String, NetworkFunction> runNFs;
    private OperationManager operationManager;
    public static long movestart;

    protected static Logger logger = LoggerFactory.getLogger(MoveStatelessControl.class);
    private TraceLoad traceLoad;
    private StopProgramm stopProgramm;
    protected short traceSwitchPort;
    private String traceHost;
    private short tracePort;
    private String traceFile;
    private int traceRate;
    private int traceNumPkts;
    private int operationDelay;
    private int stopDelay;
    private String stopHost;

    public MoveStatelessControl() {
        parseConfigFile();
    }

    public MoveStatelessControl(OperationManager operationManager){
        runNFs = new HashMap<String, NetworkFunction>();
        this.operationManager = operationManager;
        this.movestart = -1;
        parseConfigFile();
    }

    public void parseConfigFile(){
        Properties prop = new Properties();
        try {
            FileInputStream fileInputStream = new FileInputStream("/home/godbestway/IdeaProjects/practice1/src/main/java/traceload/config.properties");
            prop.load(fileInputStream);
            this.traceSwitchPort = Short.parseShort(prop.getProperty("TraceReplaySwitchPort"));
            this.traceHost = prop.getProperty("TraceReplayHost");
            this.traceFile = prop.getProperty("TraceReplayFile");
            this.traceRate = Short.parseShort(prop.getProperty("TraceReplayRate"));
            this.traceNumPkts  = Integer.parseInt(prop.getProperty("TraceReplayNumPkts"));
            this.operationDelay= Integer.parseInt(prop.getProperty("OperationDelay"));
            this.stopDelay= Integer.parseInt(prop.getProperty("StopDelay"));
            this.stopHost = prop.getProperty("StopHost");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * The NF will be added by operation manager, if we got enough number of NF, execute the move operation
     * @param nf
     */
    private synchronized void addNetworkFunction(NetworkFunction nf){
        if (this.runNFs.containsValue(nf))
        { return; }

        for (int i = 1; i <= numInstances; i++)
        {
            String nfID = "nf".concat(Integer.toString(i));
            if (!this.runNFs.containsKey(nfID))
            {
                this.runNFs.put(nfID, nf);
                break;
            }
        }

        if (firstNumInstances == this.runNFs.size())
        { this.executeStep(0); }

        if (secondNumInstances == this.runNFs.size())
        {
            changeForwarding();
            logger.info("second NF comes");

        }
    }

    /**
     * The NF will be added by operation manager
     * @param nf
     */
    public void NFConnected(NetworkFunction nf) {
        if(!this.runNFs.containsValue(nf)){
            this.addNetworkFunction(nf);
        }
    }

    public void startMove(){

        this.movestart = System.currentTimeMillis();

        this.stopProgramm = new StopProgramm("stopHost");
        stopProgramm.stopTrace(runNFs.get("nf1").getPid());


    }


    public void initialForwarding(){
        String[] cmd={"curl","-X", "POST","-d", "{\"switch\":\"00:00:00:00:00:00:00:01\",\"name\":\"flow-mod-1\"," +
                "\"in_port\":\"1\",\"active\":\"true\", \"actions\":\"output=2\"}","http://127.0.0.1:8080/wm/staticflowpusher/json"};
        System.out.println(execCurl(cmd));
    }

    public void deleteForwarding(){
        String[] cmd={"curl","-X", "DELETE","-d", "{\"name\":\"flow-mod-1\"}","http://127.0.0.1:8080/wm/staticflowpusher/json"};
        System.out.println(execCurl(cmd));
    }

    public void changeForwarding() {
        long movetime = System.currentTimeMillis() - this.movestart;

        logger.info("begin to change forward direction");
        String[] cmd={"curl","-X", "POST","-d", "{\"switch\":\"00:00:00:00:00:00:00:01\",\"name\":\"flow-mod-1\"," +
                "\"in_port\":\"1\",\"active\":\"true\", \"actions\":\"output=3\"}","http://127.0.0.1:8080/wm/staticflowpusher/json"};
        System.out.println(execCurl(cmd));
        logger.info("total move time"+movetime);
    }

    public static String execCurl(String[] cmds) {
        ProcessBuilder process = new ProcessBuilder(cmds);
        Process p;
        try {
            p = process.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            return builder.toString();

        } catch (IOException e) {
            System.out.print("error");
            e.printStackTrace();
        }
        return null;

    }

    public void executeStep(int step) {
        this.traceLoad = new TraceLoad(this.traceHost, this.traceRate , this.traceNumPkts);

        int startNextAfter;
        switch(step)
        {
            case 0:
                startNextAfter = 2;
                break;
            case 1:
                initialForwarding();
                boolean started = this.traceLoad.startTrace(this.traceFile);
                if (started)
                { logger.info("Started replaying trace"); }
                else
                { logger.error("Failed to start replaying trace"); }
                startNextAfter = this.operationDelay;
                break;
            case 2:
                logger.info("a simulation of move start");
                startMove();
                startNextAfter = 0;
                break;
            default:
                return;
        }
        this.scheduler.schedule(new NextStepTask(step+1, this), startNextAfter,
                TimeUnit.SECONDS);


    }


    public void run() {
        operationManager.addProcessCondition(this);
    }
}
