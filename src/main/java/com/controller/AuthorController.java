package com.controller;

import com.generate.common.deploy.IConfig;
import com.generate.common.deploy.MongoConfigControl;
import com.generate.common.exception.CommonException;
import com.generate.common.exception.ParamsInvalidException;
import com.generate.listener.MsgListener;
import com.generate.model.MongoOptions;
import com.generate.model.MsgNode;
import com.generate.mongo.DataSourceLinkerFetch;
import com.generate.utils.CommentUtilSource;
import com.generate.utils.CommonUtils;
import com.generate.utils.Const;
import com.gui.ConfigGui;
import com.gui.MainGui;
import com.jfoenix.controls.JFXButton;
import com.mongodb.DB;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.emojione.EmojiOneView;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.weathericons.WeatherIcon;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthorController implements Initializable {

    private static Logger logger = LoggerFactory.getLogger(AuthorController.class);

    @FXML
    public TextField hostField;
    @FXML
    public TextField portField;
    @FXML
    public TextField userField;
    @FXML
    public PasswordField pwdField;
    @FXML
    public TextField dataNameField;
    @FXML
    public ComboBox versionField;
    @FXML
    public JFXButton testButton;
    @FXML
    public TextArea logTextArea;
    @FXML
    public JFXButton clearButton;
    @FXML
    public TextField saveNameField;
    @FXML
    private JFXButton submit;

    public void submitConfig() {
        paramsCheck();
        //连接数据库
        DataSourceLinkerFetch linkerFetch = testMongoDB();
        //监控链接获取器是否已经完成连接
        DB db = monitorLinkerIsFinish(linkerFetch);
        if(db == null){
            return;
        }
        //将配置保存至ini配置文件中
        //这儿暂时使用ini配置文件来记录数据库信息(暂时没找到更好的办法) 如果有类似sqllite轻量级数据库再做切换
        saveMongoLinkerConfig();
        ConfigGui.closeWindow();
    }

    private void saveMongoLinkerConfig() {

        MongoOptions configNode = installMongoOption();

        IConfig configHelper = new MongoConfigControl();
        ((MongoConfigControl) configHelper).setConfig(configNode.getSaveName(),configNode);
        try {
            configHelper.addConfig();
        } catch (Exception e) {
            CommentUtilSource.alertMessage(Const._TIPS,"初始化数据源出现异常！！！", Alert.AlertType.ERROR);
            throw new CommonException("【保存mongo配置失败 e:】"+e.getMessage()+"");
        }
    }

    /**
     * 异步执行监听
     */
    private DB monitorLinkerIsFinish(DataSourceLinkerFetch linkerFetch) {
        for (;;){
            if(linkerFetch.isFinish()){
                return linkerFetch.getDb();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void linkTest(){
        MsgListener.processMsg(null,"测试开始");
        testMongoDB();
    }

    private DataSourceLinkerFetch testMongoDB(){
        //由于swing组件的阻塞执行,为了让消息动态刷新到界面上,所以开启新的线程来执行我们的逻辑避免堵塞
        DataSourceLinkerFetch linkerFetch = new DataSourceLinkerFetch(installMongoOption());
        linkerFetch.start();
        return linkerFetch;
    }

    private MongoOptions installMongoOption() {

        MongoOptions options = new MongoOptions();

        try {
            options.setHost(hostField.getText());
            options.setPort(Integer.parseInt(portField.getText()));
            options.setPassword(pwdField.getText());
            options.setUser(userField.getText());
            options.setDataName(dataNameField.getText());
            options.setSaveName(saveNameField.getText());
            if(CommonUtils.isEmpty(saveNameField.getText())){
                options.setSaveName(hostField.getText());
            }
        }catch (Exception e){
            MsgListener.processMsg(null,"参数缺失");
            throw e;
        }
        return options;
    }

    public void clearInput(){
        clearAllFieldText();
    }

    private void clearAllFieldText() {
        hostField.clear();
        portField.clear();
        userField.clear();
        pwdField.clear();
        dataNameField.clear();
    }

    private void paramsCheck() throws ParamsInvalidException{
        if(CommonUtils.isEmpty(hostField.getText(),portField.getText(),dataNameField.getText())){
            MsgListener.processMsg(null,"输入参数不完整");
            throw new ParamsInvalidException();
        }

        if(!CommonUtils.isHttpUrl(hostField.getText())){
            MsgListener.processMsg(null,"主机地址格式错误");
            throw new ParamsInvalidException();
        }

        if(!CommonUtils.isDigit(portField.getText()) || Integer.parseInt(portField.getText()) > 65536){
            MsgListener.processMsg(null,"端口只能为1~65536整数");
            throw new ParamsInvalidException();
        }
    }

    private synchronized void listenerProcessMsg(){
        if(MsgListener.isIsListener()){
            return;
        }
        logger.info("【Message Process Thread Start Success......】");
        new Thread(()->{
            for (;;){
                try {
                    MsgNode node = MsgListener.popMsgNode();
                    if(node == null || node.getExpire_time() < System.currentTimeMillis()){
                        continue;
                    }
                    consoleLog(node.getMsg());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    System.out.println("【消息监听：】" + e.getMessage());
                }
            }
        }).start();
        MsgListener.setIsListener(true);
    }

    private void consoleLog(String msg){
        new Thread(()-> logTextArea.appendText(msg + "\n")).start();
    }

    private void initComponentCss() {
        testButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.LINK,"15px"));
        submit.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.SAVE,"15px"));
        clearButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.RECYCLE,"15px"));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listenerProcessMsg();
        initComponentCss();
    }
}
