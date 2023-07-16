package fuchengjie.example;

import cn.objectspace.webssh.pojo.HostData;
import cn.objectspace.webssh.service.WebSSHService;
import cn.objectspace.webssh.service.impl.WebSSHServiceImpl;
import com.jcraft.jsch.JSchException;
import org.apache.catalina.Host;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.File;

@SpringBootTest
public class Test {
    @Resource
    WebSSHService webSSHService;

    @org.junit.Test
    public void testConnect() throws JSchException {
        File file = new File("/Users/fu/.ssh/id_rsa");
        boolean b = file.canRead();
        boolean f = file.exists();
        webSSHService = new WebSSHServiceImpl();
        HostData hostData = new HostData();
        hostData.setHost("139.9.242.37");
        hostData.setPort(22222);
        hostData.setUsername("fcj");
        webSSHService.testConnect(hostData);

        System.out.printf("");
    }

    @org.junit.Test
    public void testPublicKey(){

    }
}
