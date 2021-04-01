package channel;

import Server.OperationManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:15
 * @Description:
 */
public class ConnectionChannelHandler  extends BaseChannelHandler{
    protected static Logger logger = LoggerFactory.getLogger(ConnectionChannelHandler.class);

    public ConnectionChannelHandler(OperationManager operationManager) {
        super(operationManager);
    }


    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel incoming=ctx.channel();
        ConnectionChannel connChannel = new ConnectionChannel(incoming, this.operationManager);
        //新建立连接时触发的动作
        Attribute<BaseChannel> attr = ctx.attr(AttributeMapConstant.NETTY_CHANNEL_KEY);
        attr.setIfAbsent(connChannel);
        //System.out.println("客户端："+incoming.remoteAddress()+"已连接上Connection来");
        logger.info("client："+incoming.remoteAddress()+"is connected");

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        //出现异常的时候执行的动作（打印并关闭通道）
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
    {
        //连接断开时触发的动作
        Channel incoming=ctx.channel();
        //System.out.println("客户端："+incoming.remoteAddress()+"已断开");
        logger.info("client："+incoming.remoteAddress()+"is disconnected");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        //通道处于活动状态触发的动作，该方法只会在通道建立时调用一次
        Channel incoming=ctx.channel();
        //System.out.println("客户端："+incoming.remoteAddress()+"在线");
        logger.info("client："+incoming.remoteAddress()+"is online");


    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        //通道处于非活动状态触发的动作，该方法只会在通道失效时调用一次
        Channel incoming=ctx.channel();
        //System.out.println("客户端："+incoming.remoteAddress()+"掉线");
        logger.info("client："+incoming.remoteAddress()+"off-line");
    }

}
