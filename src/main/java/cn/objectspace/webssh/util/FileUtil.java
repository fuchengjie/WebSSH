package cn.objectspace.webssh.util;

import cn.objectspace.webssh.pojo.HostData;
import com.jcraft.jsch.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;

/*
 * date: 2023-02-05
 * author: fuchengjie
 *
 * */
public class FileUtil {
    public static void upload(HostData hostData, MultipartFile multipartFile) throws JSchException, SftpException, IOException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(hostData.getUsername(), hostData.getHost());
        session.setPassword(hostData.getPassword());
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();

        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();

        File file = multipartFileToFile(multipartFile);
        if (Objects.isNull(file)) {
            return;
        }
        InputStream fis = null;
        try {
            fis = Files.newInputStream(file.toPath());
            sftpChannel.put(fis, "/tmp/" + multipartFile.getOriginalFilename());
        } finally {
            if (!Objects.isNull(fis)) {
                fis.close();
            }
        }

    }


    public static File multipartFileToFile(MultipartFile multiFile) {
        // 获取文件名
        String fileName = multiFile.getOriginalFilename();
        // 获取文件后缀
        assert fileName != null;
        String postfix = fileName.substring(fileName.lastIndexOf("."));
        postfix = (Objects.isNull(postfix) ? "" : postfix);

        File file = null;
        try {
            file = File.createTempFile(fileName, postfix);
            multiFile.transferTo(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }
}
