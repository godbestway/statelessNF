package interfaces.stepControl;

/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:31
 * @Description:
 */
public interface ProcessControl {
    void executeStep(int step);
    void changeForwarding();
}
