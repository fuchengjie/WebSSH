Since the original author hadn't updated the project in over two years and hadn't solved the issues and pr, I forked one myself and optimized the front-end interface.

# README.md
## zh_CN [简体中文](readme.md)

Currently added features are:
- Front-end login page: performs a connectivity test before entering the terminal
- Dark terminal UI: terminal takes the main area, collapsible file drawer on the left, responsive layout
- File manager: browse remote directories, upload (including drag-and-drop), download, delete files, and create directories; default directory is `/tmp/`, and you can switch to other directories manually
- Test connection function
- Public key login; due to jsch version issues,
  if your id_rsa starts with "-----BEGIN OPENSSH PRIVATE KEY-----", run the command `ssh-keygen -p -f <privateKeyFile> -m pem` to convert the format

Features
- Cross platform
- Browser-in
- Dark terminal-first modern interface
- Collapsible remote file manager drawer
- Drag-and-drop upload and file list operations
- Responsive layout

## Screenshots
Login page
![Login page](docs/screenshot-login.png)

Terminal and remote file manager (dark terminal-first style)
![Terminal and remote file manager](docs/screenshot-terminal.png)

Technical architecture diagram

```mermaid
flowchart TB
    subgraph Browser["Browser"]
        Login[index.html]
        Terminal[terminal.html
xterm.js]
    end

    Login -->|POST /connect| Router[RouterController]
    Router -->|Render| Terminal

    Terminal <-->|WebSocket /webssh| Handler[WebSSHWebSocketHandler]
    Handler -->|Read/Write| Service[WebSSHServiceImpl]

    subgraph SpringBoot["Spring Boot Backend"]
        Router
        Handler
        Service
        Interceptor[WebSocketInterceptor]
        FileCtrl[FileController]
        Pool[(sshMap
UUID -> SSHConnectInfo)]
    end

    Interceptor -->|Inject UUID at handshake| Handler
    Service -->|Read/Write| Pool
    FileCtrl -->|Independent Session| Jsch2[JSch]

    Terminal -->|POST /file/* /upload| FileCtrl

    Service -->|JSch Session + shell| SSH[(SSH Server)]
    Jsch2 -->|JSch Session + sftp| SSH
```
Problems solved:
1. The WebSocket connection between the browser and SpringBoot obtains the server address through the interface instead of being written to 127.0.0.1, so that at least multiple browsers can access the server on the same LAN. On the public network effect, I did not test, there are conditions can try.

<br><br><br><br><br><br><br><br><br><br>

