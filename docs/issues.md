# WebSSH Issue 处理清单

> 来源：GitHub 仓库 `fuchengjie/WebSSH` 的开放 issue（抓取于 2026-07-01）
> 仓库地址：https://github.com/fuchengjie/WebSSH

本文件记录每个开放 issue 的内容、作者回应，以及在本地仓库中的处理进度。每完成一项即在此更新。

---

## #3 会考虑增加文件传输功能吗

- **提交者**：chinaxushi
- **创建时间**：2023-01-11
- **正文**：会考虑增加文件传输功能吗
- **作者(fuchengjie)回应**：
  - 2023-02-04：会的
  - 2023-02-05：已经添加
  - chinaxushi 2023-02-08：这速度。。刚刚的
- **现状**：已由 `FileController` + `FileUtil` 实现 SFTP 上传，默认上传到远程 `/tmp` 目录。
- **处理记录**：
  - ✅ 已完成增强：上传支持自定义远程目录、新增文件列表/下载/删除，详见「2026-07-01 文件管理增强」记录。

---

## #4 ①iterm 自适应 ②文件列表（删除/下载/上传）

- **提交者**：Lotus6
- **创建时间**：2023-02-06
- **正文**：（无）
  1. 终端自适应（前端网页放大缩小会导致显示不了命令）
  2. 文件列表显示（删除、下载、上传）
- **作者(fuchengjie)回应**（2023-02-10）：我尝试了，主要是 xterm.js 框架本身不支持自适应全屏，因为我对其了解不多，所以在学习中。如果你有方法做到提个 PR 也是也好的
- **现状**：未解决。
- **处理记录**：
  - ✅ 第 ① 项（终端自适应）已完成。
  - ✅ 第 ② 项（文件列表/删除/下载/上传）已完成，与 #3 一并实现。

---

## #5 是否考虑添加一下，自适应全屏

- **提交者**：rosterme
- **创建时间**：2023-02-10
- **正文**：如题
- **作者(fuchengjie)回应**（2023-02-10）：同 #4，xterm.js 不支持自适应全屏，在学习中，欢迎 PR。
- **现状**：未解决。与 #4① 重复。
- **处理记录**：
  - ✅ 已完成（与 #4① 一并处理）。

---

## #6 支持公钥方式登录吗

- **提交者**：cocoandgaga
- **创建时间**：2023-03-08
- **正文**：如题
- **作者(fuchengjie)回应**：
  - 2023-03-17：还没开始做，有时间会做。
  - 2023-07-16：尝试了一下，因为我的架构决定了实际连接到远程服务器的是 Java 后端，考虑到上传你的私钥到服务器中不安全，所以我就不做这个了，但是您可以自己写进去，只要添加一行代码即可 `jSch.addIdentity("～/.ssh/id_rsa");`
- **现状**：作者明确出于安全考虑不做。代码中 `WebSSHServiceImpl.connectToSSH` 已存在 `addIdentity` 调用分支，但 `SSHConnectInfo` 未见 `privateKey` 字段（待核实编译）。
- **处理记录**：暂未处理（受作者安全立场约束，需先与用户确认是否推进）。

---

## #7 考虑限制部分命令不能执行吗

- **提交者**：761121532
- **创建时间**：2023-12-27
- **正文**：（无）
- **评论**：无
- **现状**：未解决。需求为命令黑名单（如禁 `rm -rf`）。
- **处理记录**：暂未处理。注意：命令是逐字符发送到 shell 的，黑名单只能在整行回车提交时拦截，绕过手段多，仅能作为辅助防护。

---

## 进度总览

| Issue | 本轮处理 | 状态 |
|-------|----------|------|
| #3 文件传输 | 是（增强） | ✅ 已完成 |
| #4① 终端自适应 | 是 | ✅ 已完成 |
| #4② 文件列表 | 是 | ✅ 已完成 |
| #5 自适应全屏 | 是（与 #4① 同） | ✅ 已完成 |
| #6 公钥登录 | 否 | 待确认 |
| #7 命令限制 | 否 | 待定 |

---

## 2026-07-01 改动详情（#4① / #5 终端自适应）

### 方案
原 `static/plugins/xterm/xterm.js` 为 xterm.js 3.x 老版本，无 FitAddon、无 `proposeGeometry`，且 `terminal.html` 中 `new FitAddon()` 会因 `FitAddon` 未定义而报错——这是自适应一直未实现的原因。经用户确认，升级到带官方自适应插件的版本：

- `xterm` 5.3.0（UMD，全局 `Terminal`）
- `xterm-addon-fit` 0.8.0（UMD，全局 `FitAddon`，提供 `fit()`）

均为传统 `<script>` 全局风格，与本项目一致。

### 前端
- `static/plugins/xterm/xterm.js`、`xterm.css` 升级为 5.3.0；新增 `xterm-addon-fit.js`（0.8.0）；删除失效的 `xterm.js.map`。
- `templates/fronted/terminal.html`：引入 `xterm-addon-fit.js`；删除无效的 `const fitAddon = new FitAddon();`。
- `static/assets/js/jssh.js`：
  - `Terminal` 创建后 `loadAddon(new FitAddon())`，`open` 后调用 `fitAddon.fit()` 做初始自适应；
  - 监听 `window.resize`（100ms 防抖）再次 `fit()`；
  - `terminal.onResize` 把新的 `cols/rows` 通过 WebSocket 上报后端；`sendResize` 仅在连接已建立（`readyState===1`）时发送，避免初始自适应误发；
  - `onConnect` 时把初始 `terminal.cols/rows` 随连接参数一起发送。

### 后端
- `ConstantPool`：新增操作码 `WEBSSH_OPERATE_RESIZE = "resize"`。
- `HostData`：新增 `cols`、`rows` 字段及 getter/setter。
- `WebSSHServiceImpl`：
  - `recvHandle` 新增 `resize` 分支：将 `cols/rows` 通过 `ChannelShell.setPtySize` 同步给远程伪终端（像素按字符 8×16 估算）；
  - `connectToSSH` 在通道连接后，按前端上报的初始 `cols/rows` 调用 `setPtySize`，保证连接建立时尺寸即正确；
  - 新增 `import com.jcraft.jsch.ChannelShell`。

### 顺带修复的预先存在编译错误（与本次 issue 无关，但阻断编译）
master 分支原本无法编译，已做最小修复（运行时行为不变）：
- `WebSSHServiceImpl.testConnect` 去掉多余的 `throws JSchException`（方法内部已 try-catch，本就不会抛出；接口未声明该异常）。
- `SSHConnectInfo` 补上 `privateKey` 字段及 getter/setter（`connectToSSH` 早已调用 `getPrivateKey()`，但字段缺失；补全后默认为 null，公钥分支仍不生效，与 #6 作者「不做公钥登录」的立场一致）。

### 验证
- `mvn -DskipTests compile` 通过（JDK 1.8.0_202 + Maven 3.9.16）。
- Playwright 自动化验证通过：终端渲染成功、窗口缩放触发 resize 消息上报（`cols:39/150`），页面无 JS 错误。截图见 `docs/verify_terminal_*.png`。
- 运行时验证（连真实 SSH 主机观察终端随窗口缩放自适应）需用户在实际环境中确认。

---

## 2026-07-01 改动详情（#3 / #4② 文件管理增强）

### 方案
在原 `FileUtil.upload` 基础上扩展为完整的 SFTP 文件管理：列表、下载、删除、上传，并支持自定义远程目录（默认仍兼容 `/tmp/`）。

### 后端
- `pom.xml`：JSch 从 `0.1.54` 升级到 `0.1.55`（`SftpATTRS.getMTime()` 等新 API 需要）。
- `FileUtil`：
  - 新增 `DEFAULT_REMOTE_PATH = "/tmp/"` 与 `normalizePath()`；
  - 新增 `openSftpChannel()` / `closeSftpChannel()` 统一创建/关闭 SFTP 会话；
  - 新增 `list()`：返回文件名、大小、修改时间、是否目录；
  - 新增 `delete()`：删除文件或空目录；
  - 新增 `download()`：按 `application/octet-stream` 输出到浏览器；
  - 改造 `upload()`：支持 `remotePath` 参数，并自动创建缺失目录；
  - 所有方法确保 finally 中关闭 session。
- `FileController`：
  - `/upload` GET：进入文件管理页，传入连接信息和 `remotePath`；
  - `/upload` POST：上传文件到指定目录，返回 JSON；
  - `/file/list` GET：列目录；
  - `/file/download` GET：下载文件；
  - `/file/delete` POST：删除文件。

### 前端
- `templates/fronted/upload.html` 重写为文件管理页：
  - 远程目录输入框 + 刷新列表；
  - 文件列表表格（文件名、大小、修改时间、类型、操作列）；
  - 下载/删除按钮；
  - 上传文件表单（上传到当前目录）。

### 验证
- `mvn -DskipTests compile` 通过。
- 运行时验证（连真实 SSH 主机测试列表/下载/删除/上传）需用户在实际环境中确认。
