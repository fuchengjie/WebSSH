package cn.objectspace.webssh.service.impl;

import cn.objectspace.webssh.constant.ConstantPool;
import cn.objectspace.webssh.pojo.SSHConnectInfo;
import cn.objectspace.webssh.pojo.HostData;
import cn.objectspace.webssh.service.WebSSHService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Description: WebSSH业务逻辑实现
 * @Author: NoCortY
 * @Date: 2020/3/8
 */
@Service
public class WebSSHServiceImpl implements WebSSHService {
    //存放ssh连接信息的map
    private static Map<String, Object> sshMap = new ConcurrentHashMap<>();

    private Logger logger = LoggerFactory.getLogger(WebSSHServiceImpl.class);
    //线程池
    private ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * @Description: 初始化连接
     * @Param: [session]
     * @return: void
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    @Override
    public void initConnection(WebSocketSession session) {
        JSch jSch = new JSch();
        SSHConnectInfo sshConnectInfo = new SSHConnectInfo();
        sshConnectInfo.setjSch(jSch);
        sshConnectInfo.setWebSocketSession(session);
        String uuid = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));
        //将这个ssh连接信息放入map中
        sshMap.put(uuid, sshConnectInfo);
    }

    /**
     * @Description: 处理客户端发送的数据
     * @Param: [buffer, session]
     * @return: void
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    @Override
    public void recvHandle(String buffer, WebSocketSession session) {
        ObjectMapper objectMapper = new ObjectMapper();
        HostData webSSHData = null;
        try {
            webSSHData = objectMapper.readValue(buffer, HostData.class);
        } catch (IOException e) {
            logger.error("Json转换异常");
            logger.error("异常信息:{}", e.getMessage());
            return;
        }
        String userId = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));
        if (ConstantPool.WEBSSH_OPERATE_CONNECT.equals(webSSHData.getOperate())) {
            //找到刚才存储的ssh连接对象
            SSHConnectInfo sshConnectInfo = (SSHConnectInfo) sshMap.get(userId);
            //启动线程异步处理
            HostData finalWebSSHData = webSSHData;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean res = connectToSSH(sshConnectInfo, finalWebSSHData, session);
                        logger.info("webssh连接状态" + res);
                    } catch (JSchException | IOException e) {
                        logger.error("webssh连接异常");
                        logger.error("异常信息:{}", e.getMessage());
                        close(session);
                    }
                }
            });
        } else if (ConstantPool.WEBSSH_OPERATE_COMMAND.equals(webSSHData.getOperate())) {
            String command = webSSHData.getCommand();
            SSHConnectInfo sshConnectInfo = (SSHConnectInfo) sshMap.get(userId);
            if (sshConnectInfo != null) {
                try {
                    transToSSH(sshConnectInfo.getChannel(), command);
                } catch (IOException e) {
                    logger.error("webssh连接异常");
                    logger.error("异常信息:{}", e.getMessage());
                    close(session);
                }
            }
        } else if (ConstantPool.WEBSSH_OPERATE_RESIZE.equals(webSSHData.getOperate())) {
            // 终端尺寸变化，同步给远程伪终端
            SSHConnectInfo sshConnectInfo = (SSHConnectInfo) sshMap.get(userId);
            if (sshConnectInfo != null && sshConnectInfo.getChannel() instanceof ChannelShell) {
                ChannelShell channelShell = (ChannelShell) sshConnectInfo.getChannel();
                Integer cols = webSSHData.getCols();
                Integer rows = webSSHData.getRows();
                if (cols != null && rows != null && cols > 0 && rows > 0) {
                    // 像素宽高按字符 8x16 估算
                    channelShell.setPtySize(cols, rows, cols * 8, rows * 16);
                }
            }
        } else {
            logger.error("不支持的操作");
            close(session);
        }
    }

    @Override
    public void sendMessage(WebSocketSession session, byte[] buffer) throws IOException {
        session.sendMessage(new TextMessage(buffer));
    }

    @Override
    public void close(WebSocketSession session) {
        String userId = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));
        SSHConnectInfo sshConnectInfo = (SSHConnectInfo) sshMap.get(userId);
        if (sshConnectInfo != null) {
            //断开连接
            if (sshConnectInfo.getChannel() != null) sshConnectInfo.getChannel().disconnect();
            //清理本次会话的临时私钥文件
            deletePrivateKeyFile(sshConnectInfo.getPrivateKey());
            //map中移除
            sshMap.remove(userId);
        }
    }

    @Override
    public Map<String, String> testConnect(HostData data) {
        String username = data.getUsername();
        String password = data.getPassword();
        String host = data.getHost();
        int port = data.getPort();

        // 创建JSch对象
        JSch jSch = new JSch();
        Session jSchSession = null;
        Map<String, String> map = new HashMap<>();
        map.put("res", "");
        map.put("msg", "");
        boolean res = false;
        String tempKeyFile = null;

        try {
            // 若前端提交了私钥内容，写入临时文件并加载
            tempKeyFile = writePrivateKeyToTempFile(data.getPrivateKey());
            if (tempKeyFile != null) {
                if (password != null && !password.isEmpty()) {
                    jSch.addIdentity(tempKeyFile, password);
                } else {
                    jSch.addIdentity(tempKeyFile);
                }
            }

            // 根据主机账号、ip、端口获取一个Session对象
            jSchSession = jSch.getSession(username, host, port);

            // 存放主机密码（私钥登录时也可作为私钥口令）
            jSchSession.setPassword(password);

            Properties config = new Properties();

            // 去掉首次连接确认
            config.put("StrictHostKeyChecking", "no");

            jSchSession.setConfig(config);

            // 超时连接时间为3秒
            jSchSession.setTimeout(3000);

            // 进行连接
            jSchSession.connect();

            // 获取连接结果
            res = jSchSession.isConnected();
            map.put("res", String.valueOf(res));
        } catch (JSchException e) {
            logger.warn(e.getMessage());
            map.put("msg", e.getMessage());
        } catch (IOException e) {
            logger.warn("私钥写入临时文件失败", e);
            map.put("msg", "私钥处理失败：" + e.getMessage());
        } finally {
            // 清理测试时产生的临时私钥文件
            deletePrivateKeyFile(tempKeyFile);
            // 关闭jschSesson流
            if (jSchSession != null && jSchSession.isConnected()) {
                jSchSession.disconnect();
            }
            if (res) {
                logger.info("测试SSH连接: " + host + " 连接成功");
            } else {
                logger.error("测试SSH连接: " + host + " 连接失败");
            }

            // 返回到前端的数据
            return map;
        }
    }

    /**
     * @Description: 使用jsch连接终端
     * @Param: [cloudSSH, webSSHData, webSocketSession]
     * @return: void
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    private boolean connectToSSH(SSHConnectInfo sshConnectInfo, HostData webSSHData, WebSocketSession webSocketSession) throws JSchException, IOException {
        Session session = null;
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        //获取jsch的会话
        session = sshConnectInfo.getjSch().getSession(webSSHData.getUsername(), webSSHData.getHost(), webSSHData.getPort());
        session.setConfig(config);

        // 若前端提交了私钥内容，写入本次会话的临时文件并加载；密码字段同时作为私钥口令
        String privateKeyContent = webSSHData.getPrivateKey();
        String tempKeyFile = writePrivateKeyToTempFile(privateKeyContent);
        if (tempKeyFile != null) {
            sshConnectInfo.setPrivateKey(tempKeyFile);
            String passphrase = webSSHData.getPassword();
            if (passphrase != null && !passphrase.isEmpty()) {
                sshConnectInfo.getjSch().addIdentity(tempKeyFile, passphrase);
            } else {
                sshConnectInfo.getjSch().addIdentity(tempKeyFile);
            }
        }

        //设置密码
        session.setPassword(webSSHData.getPassword());
        //连接  超时时间30s
        session.connect(30000);

        //开启shell通道
        Channel channel = session.openChannel("shell");

        //通道连接 超时时间3s
        channel.connect(3000);

        // 设置初始终端尺寸（若前端已上报）
        Integer cols = webSSHData.getCols();
        Integer rows = webSSHData.getRows();
        if (channel instanceof ChannelShell && cols != null && rows != null && cols > 0 && rows > 0) {
            ((ChannelShell) channel).setPtySize(cols, rows, cols * 8, rows * 16);
        }

        // 获取连接状态
        boolean res = channel.isConnected();

        //设置channel
        sshConnectInfo.setChannel(channel);

        //转发消息
        transToSSH(channel, "\r");

        //读取终端返回的信息流
        InputStream inputStream = channel.getInputStream();
        try {
            //循环读取
            byte[] buffer = new byte[1024];
            int i = 0;
            //如果没有数据来，线程会一直阻塞在这个地方等待数据。
            while ((i = inputStream.read(buffer)) != -1) {
                sendMessage(webSocketSession, Arrays.copyOfRange(buffer, 0, i));
            }

        } finally {
            //断开连接后关闭会话
            session.disconnect();
            channel.disconnect();
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return res;
    }

    /**
     * 把私钥内容写入本次会话的临时文件，返回临时文件绝对路径；无私钥时返回 null。
     */
    private String writePrivateKeyToTempFile(String privateKey) throws IOException {
        if (privateKey == null || privateKey.trim().isEmpty()) {
            return null;
        }
        Path temp;
        try {
            // POSIX 系统在创建时即限定为仅属主可读写，避免临时私钥被同机其他用户读取
            FileAttribute<Set<PosixFilePermission>> attr =
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
            temp = Files.createTempFile("webssh-pk-", ".key", attr);
        } catch (UnsupportedOperationException e) {
            // Windows 等非 POSIX 文件系统不支持 POSIX 权限，退回默认临时文件
            temp = Files.createTempFile("webssh-pk-", ".key");
        }
        File tempFile = temp.toFile();
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(privateKey.trim());
        }
        return tempFile.getAbsolutePath();
    }

    /**
     * 删除临时私钥文件
     */
    private void deletePrivateKeyFile(String path) {
        if (path != null && !path.isEmpty()) {
            try {
                new File(path).delete();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * @Description: 将消息转发到终端
     * @Param: [channel, data]
     * @return: void
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    private void transToSSH(Channel channel, String command) throws IOException {
        if (channel != null) {
            OutputStream outputStream = channel.getOutputStream();
            outputStream.write(command.getBytes());
            outputStream.flush();
        }
    }
}
