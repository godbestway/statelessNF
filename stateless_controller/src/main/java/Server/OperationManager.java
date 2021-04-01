package Server;

import channel.BaseChannel;
import channel.ConnectionChannel;
import interfaces.NetworkFunction;
import interfaces.stepControl.ProcessCondition;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:16
 * @Description:
 */
public class OperationManager {
    private ConcurrentHashMap<String, NetworkFunction> nfs;
    private ProcessCondition processCondition;
    protected static Logger logger = LoggerFactory.getLogger(OperationManager.class);


    public int port1;
    public int port2;
    public  static  int serverSet;


    public OperationManager(){

        nfs = new ConcurrentHashMap<String, NetworkFunction>();
        port1 = 18080;
        port2 = 18081;
    }




    public void run1() throws Exception
    {
        //负责接收客户端的连接
        EventLoopGroup connBossGroup=new NioEventLoopGroup();

        //负责处理消息I/O
        EventLoopGroup connWorkerGroup=new NioEventLoopGroup();
        try {
            ServerBootstrap first = new ServerBootstrap();//用于启动NIO服务
            first.group(connBossGroup,connWorkerGroup)
                    .channel(NioServerSocketChannel.class) //通过工厂方法设计模式实例化一个channel
                    //.localAddress(new InetSocketAddress(port1))//设置监听端口
                    .option(ChannelOption.SO_BACKLOG,128)//最大保持连接数128，option主要是针对boss线程组
                    .childOption(ChannelOption.SO_KEEPALIVE,true)//启用心跳保活机制，childOption主要是针对worker线程组
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    .childHandler(new InitializerServerConn(this));

            bind(first, port1);



        } finally {

        }

    }


    public void bind(ServerBootstrap serverBootstrap, int port) {
        ChannelFuture channelFuture = serverBootstrap.bind(port);
        channelFuture.addListener(new ServerListener(port, this));

    }



    public NetworkFunction obtainNetworkFunction(String host, int pid){
        //System.out.println("find a NF");
        logger.info("a new NF is found");
        NetworkFunction nf;
        synchronized (this.nfs){
            String id = NetworkFunction.constructId(host , pid );
            System.out.println(id);
            if(nfs.containsKey(id)){
                logger.info("contains this id");
                nf = nfs.get(id);
            }else{
                nf = new NetworkFunction(host , pid);
                logger.info("not contains this id");
                nfs.put(id , nf);
                //System.out.println("set a NF successful");
                logger.info("set a NF successful");
            }
        }
        return nf;
    }

    /**
     * use syn message to check whether two channels are connected
     * if the coming NF is fully connected(receive the syn messages from the conn and action channel )
     * tell the process, a new NF is added
     * @param channel
     */
    public void channelConnected(BaseChannel channel){
        //System.out.println("channel try to connect");
        logger.info("channel try to connect");
        NetworkFunction nf = obtainNetworkFunction(channel.getHost(),  channel.getPid());
        boolean connected = false;
        synchronized (nf){
            channel.setNetworkFunction(nf);
            if(channel instanceof ConnectionChannel){
                //System.out.println("try to set a connection channel");
                logger.info("try to set a connection channel");
                nf.setConnectionChannel((ConnectionChannel) channel);
                connected = true;
            }
        }
        if(connected){
            //System.out.println(NetworkFunction.constructId(nf.getHost(), nf.getPid())+"has already fully connected");
            logger.info(NetworkFunction.constructId(nf.getHost(), nf.getPid())+"has already fully connected");
        }

        if(connected){
            processCondition.NFConnected(nf);
        }

    }

    //Prcoess will use this function to add itself
    public void addProcessCondition(ProcessCondition processCondition){
        this.processCondition = processCondition;
        //System.out.println("a new condition is added");
        logger.info("a new condition is added");
    }


}
