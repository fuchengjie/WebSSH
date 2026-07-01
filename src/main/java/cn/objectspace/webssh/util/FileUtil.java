package cn.objectspace.webssh.util;

import cn.objectspace.webssh.pojo.HostData;
import com.jcraft.jsch.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/*
 * date: 2023-02-05
 * author: fuchengjie
 *
 * */
public class FileUtil {

    /**
     * 默认远程目录：保持与原 upload 行为一致
     */
    public static final String DEFAULT_REMOTE_PATH = "/tmp/";

    /**
     * 规范化远程目录路径，确保以 / 结尾
     */
    public static String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return DEFAULT_REMOTE_PATH;
        }
        path = path.trim();
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return path;
    }

    /**
     * 拼接远程路径时只接受单个文件名，避免前端传入 ../ 或斜杠导致越过当前目录操作。
     */
    private static String joinRemotePath(String remotePath, String filename) {
        return normalizePath(remotePath) + safeName(filename);
    }

    /**
     * SFTP 删除、下载、创建目录都只允许处理当前目录下的直接子项。
     */
    private static String safeName(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        filename = filename.trim();
        if (".".equals(filename) || "..".equals(filename) || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("非法文件名: " + filename);
        }
        return filename;
    }

    /**
     * 获取 SFTP 会话（调用方必须负责关闭 session）
     */
    public static ChannelSftp openSftpChannel(HostData hostData) throws JSchException, SftpException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(hostData.getUsername(), hostData.getHost(), hostData.getPort() == null ? 22 : hostData.getPort());
        session.setPassword(hostData.getPassword());
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();

        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        // 把 session 暂存在 sftpChannel 上，便于后续关闭
        sftpChannel.setFilenameEncoding("UTF-8");
        return sftpChannel;
    }

    /**
     * 关闭 SFTP 会话
     */
    public static void closeSftpChannel(ChannelSftp sftpChannel) {
        if (sftpChannel != null) {
            try {
                Session session = sftpChannel.getSession();
                sftpChannel.disconnect();
                if (session != null) {
                    session.disconnect();
                }
            } catch (JSchException e) {
                // 忽略
            }
        }
    }

    /**
     * 上传文件到指定远程目录
     */
    public static void upload(HostData hostData, MultipartFile multipartFile, String remotePath) throws JSchException, SftpException, IOException {
        ChannelSftp sftpChannel = openSftpChannel(hostData);
        try {
            String targetDir = normalizePath(remotePath);
            ensureDirectory(sftpChannel, targetDir);

            String filename = getOriginalFilename(multipartFile);
            try (InputStream inputStream = multipartFile.getInputStream()) {
                sftpChannel.put(inputStream, joinRemotePath(targetDir, filename));
            }
        } finally {
            closeSftpChannel(sftpChannel);
        }
    }

    /**
     * 列出远程目录下的文件/文件夹
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> list(HostData hostData, String remotePath) throws JSchException, SftpException {
        ChannelSftp sftpChannel = openSftpChannel(hostData);
        try {
            String targetDir = normalizePath(remotePath);

            Vector<ChannelSftp.LsEntry> entries = sftpChannel.ls(targetDir);
            List<Map<String, Object>> result = new ArrayList<>();
            for (ChannelSftp.LsEntry entry : entries) {
                String filename = entry.getFilename();
                if (".".equals(filename) || "..".equals(filename)) {
                    continue;
                }
                SftpATTRS attrs = entry.getAttrs();
                Map<String, Object> item = new HashMap<>();
                item.put("filename", filename);
                item.put("size", attrs.getSize());
                item.put("modifyTime", attrs.getMTime() * 1000L);
                item.put("directory", attrs.isDir());
                result.add(item);
            }
            // 目录排在文件前面，同类按名称排序，方便在前端浏览远程文件。
            result.sort(new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> first, Map<String, Object> second) {
                    boolean firstDir = Boolean.TRUE.equals(first.get("directory"));
                    boolean secondDir = Boolean.TRUE.equals(second.get("directory"));
                    if (firstDir != secondDir) {
                        return firstDir ? -1 : 1;
                    }
                    return String.valueOf(first.get("filename")).compareToIgnoreCase(String.valueOf(second.get("filename")));
                }
            });
            return result;
        } finally {
            closeSftpChannel(sftpChannel);
        }
    }

    /**
     * 在远程目录下创建子目录
     */
    public static void mkdir(HostData hostData, String remotePath, String dirname) throws JSchException, SftpException {
        ChannelSftp sftpChannel = openSftpChannel(hostData);
        try {
            String targetDir = normalizePath(remotePath);
            ensureDirectory(sftpChannel, targetDir);
            sftpChannel.mkdir(joinRemotePath(targetDir, dirname));
        } finally {
            closeSftpChannel(sftpChannel);
        }
    }

    /**
     * 删除远程文件或空目录
     */
    public static void delete(HostData hostData, String remotePath, String filename) throws JSchException, SftpException {
        ChannelSftp sftpChannel = openSftpChannel(hostData);
        try {
            String targetDir = normalizePath(remotePath);
            String fullPath = joinRemotePath(targetDir, filename);
            SftpATTRS attrs = sftpChannel.stat(fullPath);
            if (attrs.isDir()) {
                sftpChannel.rmdir(fullPath);
            } else {
                sftpChannel.rm(fullPath);
            }
        } finally {
            closeSftpChannel(sftpChannel);
        }
    }

    /**
     * 下载远程文件到浏览器
     */
    public static void download(HostData hostData, String remotePath, String filename, HttpServletResponse response) throws JSchException, SftpException, IOException {
        ChannelSftp sftpChannel = openSftpChannel(hostData);
        InputStream in = null;
        try {
            String targetDir = normalizePath(remotePath);
            String safeFilename = safeName(filename);
            String fullPath = joinRemotePath(targetDir, safeFilename);

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(safeFilename, "UTF-8"));

            in = sftpChannel.get(fullPath);
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
            closeSftpChannel(sftpChannel);
        }
    }

    /**
     * 确保远程目录存在（不存在则逐级创建）
     */
    private static void ensureDirectory(ChannelSftp sftpChannel, String dir) throws SftpException {
        try {
            sftpChannel.cd(dir);
            return;
        } catch (SftpException e) {
            // 目录不存在，尝试创建
        }
        String[] parts = dir.split("/");
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            current.append("/").append(part);
            try {
                sftpChannel.cd(current.toString());
            } catch (SftpException e) {
                sftpChannel.mkdir(current.toString());
            }
        }
    }

    public static File multipartFileToFile(MultipartFile multiFile) {
        // 获取文件名
        String fileName = getOriginalFilename(multiFile);
        // 获取文件后缀
        int suffixIndex = fileName.lastIndexOf(".");
        String prefix = suffixIndex > 0 ? fileName.substring(0, suffixIndex) : fileName;
        String postfix = suffixIndex > 0 ? fileName.substring(suffixIndex) : ".tmp";
        if (prefix.length() < 3) {
            prefix = (prefix + "___").substring(0, 3);
        }

        File file = null;
        try {
            file = File.createTempFile(prefix, postfix);
            multiFile.transferTo(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    private static String getOriginalFilename(MultipartFile multiFile) {
        String fileName = multiFile.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            return "upload";
        }
        return new File(fileName).getName();
    }
}
