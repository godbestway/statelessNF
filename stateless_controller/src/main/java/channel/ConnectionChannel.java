package channel;

import Server.OperationManager;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyConnMessageProto;


/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:14
 * @Description:
 */
public class ConnectionChannel extends BaseChannel{
    protected static Logger logger = LoggerFactory.getLogger(ConnectionChannel.class);

    public ConnectionChannel(Channel channel, OperationManager operationManager) {
        super(channel, operationManager);
    }

    protected void processMessage(Object msg) {

        MyConnMessageProto.MyConnMessage myMessage = (MyConnMessageProto.MyConnMessage)msg;
        MyConnMessageProto.MyConnMessage.DataType dataType = myMessage.getDataType();
        if(dataType == MyConnMessageProto.MyConnMessage.DataType.SynType){
            MyConnMessageProto.ConnSyn syn = myMessage.getConnsyn();
            this.host = syn.getHost() ;
            this.pid = syn.getPid();
            operationManager.channelConnected(this);
        }

    }

    public void sendMessage(Object msg) {
        channel.writeAndFlush(msg);
    }
}
