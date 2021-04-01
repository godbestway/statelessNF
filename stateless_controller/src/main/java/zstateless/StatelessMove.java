package zstateless;

import Server.OperationManager;
import interfaces.stepControl.ProcessCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StatelessMove {
    protected static Logger logger = LoggerFactory.getLogger(StatelessMove.class);

    public static void main(String[] args) {
        OperationManager operationManager = new OperationManager();

        try {
            operationManager.run1();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ProcessCondition moveProcessControl = new MoveStatelessControl(operationManager);
        new Thread((Runnable) moveProcessControl).start();

        synchronized (operationManager){
            while(OperationManager.serverSet != 1){
                try {
                    operationManager.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            logger.info("netty server is set up");
        }

    }
}
