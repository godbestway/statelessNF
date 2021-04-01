package commands;

import javafx.scene.paint.Stop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class StopProgramm {
    protected static Logger log = LoggerFactory.getLogger(StopProgramm.class);

    private static final String ACTION_STOP = "stop";

    private static final short REQUEST_SERVER_PORT = (short)8080;

    /** Address of the service */
    private InetSocketAddress serverAddr;

    /** Socket connection to the service */
    private Socket sock;

    public StopProgramm(String ip)
    {
        this.sock = new Socket();
        this.serverAddr = new InetSocketAddress(ip, REQUEST_SERVER_PORT);
    }


    private boolean issueCommand(String action, int pid)
    {
        if (!this.sock.isConnected())
        {
            try
            { this.sock.connect(this.serverAddr);}
            catch (IOException e)
            {
                log.error("Failed to connect to traceload sever");
                return false;
            }
        }

        try
        {
            PrintWriter out = new PrintWriter(this.sock.getOutputStream());
            out.println(String.format("%s %d", action, pid));
            out.flush();
        }
        catch (IOException e)
        {
            log.error(String.format("Failed to issue command '%s %d'", action, pid));
            return false;
        }

        return true;
    }

    public boolean stopTrace(int pid)
    { return this.issueCommand(ACTION_STOP, pid); }
}
