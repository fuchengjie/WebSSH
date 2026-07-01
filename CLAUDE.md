# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

WebSSH is a Spring Boot 2.3.0 web application that exposes an SSH terminal in the browser. A browser opens a WebSocket to the server; the server uses JSch to open a `shell` channel to a user-supplied SSH host and bridges bytes between the browser's xterm.js terminal and the remote shell. It also supports SFTP file upload and a pre-connect "test connection" check. Java 8 / Oracle JDK 8 (the Docker base image is `frolvlad/alpine-oraclejdk8`).

## Commands

Build / run / test (Maven, `pom.xml` at repo root):

- Run locally: `mvn spring-boot:run` (serves on port 80 — see `src/main/resources/application.yml`)
- Build the jar: `mvn clean package` → `target/WebSSH.jar` (finalName is `WebSSH`)
- Run all tests: `mvn test`
- Run a single test: `mvn -Dtest=Test#testConnect test` (tests are JUnit 4 under `src/test/java/fuchengjie/example/Test.java`)

The frontend is plain HTML/JS (no build step): edit files under `src/main/resources/` and reload. Thymeleaf cache is disabled in dev (`application.yml`), so template changes are picked up on restart.

## Architecture

The core flow spans four layers and is easiest to understand as a two-phase WebSocket session:

1. **Handshake** — `WebSocketInterceptor` (registered in `WebSSHWebSocketConfig` for path `/webssh`) generates a UUID and stuffs it into the WebSocket session attributes under `ConstantPool.USER_UUID_KEY`. This UUID is the identity for the whole session.
2. **Connect phase** — `WebSSHWebSocketHandler.afterConnectionEstablished` calls `WebSSHServiceImpl.initConnection`, which creates a `JSch` instance + `SSHConnectInfo` and stores them in a **static `ConcurrentHashMap` `sshMap`** keyed by UUID. The browser then sends its first JSON message with `operate: "connect"` carrying host/port/user/password. `recvHandle` reads it, and on a cached thread-pool (`Executors.newCachedThreadPool`) runs `connectToSSH`, which opens the JSch `Session` + `shell` `Channel`, stores the channel back on the `SSHConnectInfo`, then **blocks reading the channel's `InputStream` in a loop**, forwarding each chunk to the browser via `sendMessage`. That blocking read loop is the lifetime of the SSH connection.
3. **Command phase** — Each subsequent keystroke from xterm arrives as `operate: "command"`. `recvHandle` looks up the `SSHConnectInfo` by UUID and writes the command bytes to the channel's `OutputStream`. There is no per-message SSH round-trip — output flows back asynchronously through the loop from phase 2.
4. **Close** — `afterConnectionClosed` → `WebSSHServiceImpl.close` disconnects the channel and removes the entry from `sshMap`.

Other pieces:

- `RouterController` — `GET /` serves the login page (`templates/fronted/index.html`). `POST /connect` takes the `HostData` form, stashes it into the **static mutable `ConstantPool.SSH_DATA`**, and renders `templates/fronted/terminal.html`. `POST /testConnect` returns a `Map{res, msg}`. Note `testConnect` is declared to `throws JSchException` in the interface but the impl catches it internally and never throws.
- `FileController` — `GET/POST /upload` opens a fresh JSch `Session` + `sftp` channel per upload and `put`s the file to **`/tmp/`** on the remote host (path is hardcoded in `FileUtil.upload`).
- `IpUtil` — local IPv4 discovery (used elsewhere to advertise the server address to the browser instead of hardcoding `127.0.0.1`, per the README).
- Frontend: `static/assets/js/jssh.js` defines `WSSHClient`, which derives the WebSocket URL from `window.location.host` (`ws://host/webssh`) and JSON-encodes every message. `terminal.html` inlines the connection params via Thymeleaf (`th:inline="javascript"`) and boots xterm.

### Things to watch when changing this code

- `ConstantPool.SSH_DATA` is a **static, globally-shared, mutable** field set by `RouterController.connect`. It is not scoped per session — multiple concurrent users will clobber it. Treat this as a known design smell, not an invariant to preserve.
- `WebSSHServiceImpl.testConnect` and the test class hardcode the private key path `/Users/fu/.ssh/id_rsa` — these are developer-local artifacts, not project config.
- `.gitignore` excludes `*.properties`; runtime config lives in `application.yml` (YAML, not ignored).
- The connect read-loop runs on an unbounded cached thread pool; one long-lived thread per active SSH session. Don't move it to a bounded pool without considering session lifetime.
