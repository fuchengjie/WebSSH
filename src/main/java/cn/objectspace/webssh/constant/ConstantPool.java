package cn.objectspace.webssh.constant;

import cn.objectspace.webssh.pojo.WebSSHData;

/**
* @Description: 常量池
* @Author: NoCortY
* @Date: 2020/3/8
*/
public class ConstantPool {
    /**
     * 随机生成uuid的key名
     */
    public static final String USER_UUID_KEY = "user_uuid";
    /**
     * 用户连接的信息
     */
    public static WebSSHData SSH_DATA = null;
    /**
     * 发送指令：连接
     */
    public static final String WEBSSH_OPERATE_CONNECT = "connect";
    /**
     * 发送指令：命令
     */
    public static final String WEBSSH_OPERATE_COMMAND = "command";
}
