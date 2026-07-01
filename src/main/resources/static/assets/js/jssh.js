// 在前端打开模拟终端

function openTerminal(operate, host, port, username, password, id) {
    return internalFunc({
        operate: operate, // 指令，默认为connect
        host: host,// IP
        port: port,// 端口号
        username: username,// 用户名
        password: password // 密码
    });

    function internalFunc(options) {
        // 新建客户端的socket
        const client = new WSSHClient();
        const terminal = new Terminal({
            cols: 80,
            rows: 24,
            cursorBlink: true, // 光标闪烁
            cursorStyle: "block", // 光标样式  null | 'block' | 'underline' | 'bar'
            scrollback: 800, // 回滚
            tabStopWidth: 8 // 制表宽度
        });

        // 自适应插件：根据容器尺寸自动计算并应用 cols/rows
        // 兼容 addon-fit 的 UMD 导出：新版把模块对象赋给全局，真正的类在 .FitAddon 上
        const FitAddonModule = (typeof FitAddon === 'function') ? FitAddon : (FitAddon && FitAddon.FitAddon);
        const fitAddon = new FitAddonModule();
        terminal.loadAddon(fitAddon);

        // 简易防抖
        function debounce(fn, wait) {
            let timer = null;
            return function () {
                clearTimeout(timer);
                timer = setTimeout(fn, wait);
            };
        }

        // 终端尺寸变化时，把新的 cols/rows 同步给远程伪终端
        terminal.onResize(function (size) {
            client.sendResize(size.cols, size.rows);
        });

        terminal.onData(function (data) {
            // 键盘输入时的回调函数
            client.sendClientData(data);
        });

        // 打开终端到指定容器
        terminal.open(document.getElementById(id));

        // 在页面上显示连接中...
        terminal.write('Connecting...\r\n');

        // 先按容器尺寸自适应一次，得到初始 cols/rows
        fitAddon.fit();

        // 窗口缩放时防抖自适应
        window.addEventListener('resize', debounce(function () {
            fitAddon.fit();
        }, 100));

        // 执行连接操作
        client.connect({
            onError: function (error) {
                // 连接失败
                terminal.write('Error: ' + error + '\r\n');
            }, onConnect: function () {
                // 连接成功，发送连接参数（携带初始终端尺寸）
                options.cols = terminal.cols;
                options.rows = terminal.rows;
                client.sendInitData(options);
            }, onClose: function () {
                // 连接关闭
                terminal.write("\r\nconnection has closed...");
            }, onData: function (data) {
                // 收到数据时
                terminal.write(data);
            }
        });

        return terminal;
    }
}



// websocket前端代码，负责发送字符串和处理结果
class WSSHClient {
    // 获取springboot服务器地址
    getHost() {
        const wssProtocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
        return wssProtocol + window.location.host + '/webssh'; // js可以直接获取服务器的地址
    }

    connect(options) {
        const host = this.getHost();

        // 浏览器不支持websock就直接退出
        if (window.WebSocket) {
            this._connection = new WebSocket(host);
        } else {
            options.onError('WebSocket Not Supported');
            return;
        }

        this._connection.onopen = function () {
            options.onConnect();
        };

        this._connection.onmessage = function (evt) {
            const data = evt.data.toString();
            //data = base64.decode(data);
            options.onData(data);
        };

        this._connection.onclose = function () {
            options.onClose();
        };
    }

    // 发送json序列化数据
    send(data) {
        this._connection.send(JSON.stringify(data));
    }

    // 连接参数
    sendInitData(options) {
        this._connection.send(JSON.stringify(options));
    }

    // 发送指令
    sendClientData(data) {
        this._connection.send(JSON.stringify({
            "operate": "command", "command": data
        }))
    }

    // 发送终端尺寸变化（连接建立后才发送，避免初始自适应时误发）
    sendResize(cols, rows) {
        if (this._connection && this._connection.readyState === 1) {
            this._connection.send(JSON.stringify({
                "operate": "resize", "cols": cols, "rows": rows
            }))
        }
    }
}
