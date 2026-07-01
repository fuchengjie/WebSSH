// 验证 #4/#5 终端自适应：打开终端页，检查 xterm 渲染、FitAddon 可用、resize 上报
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });

  const errors = [];
  const wsSent = [];
  const wsFrames = [];

  page.on('console', msg => {
    if (msg.type() === 'error') errors.push('CONSOLE ERROR: ' + msg.text());
    if (msg.type() === 'log') console.log('[页面log]', msg.text());
  });
  page.on('pageerror', err => errors.push('PAGE ERROR: ' + err.message));

  // 拦截 testConnect 请求，伪造为连接成功，使登录表单能跳转到终端页
  // （本机无真实 SSH 服务，业务代码 testConnect 失败会阻止跳转；此处仅验证前端自适应，不改业务代码）
  await page.route('**/testConnect', route => {
    route.fulfill({ status: 200, contentType: 'application/json',
      body: JSON.stringify({ res: 'true', msg: '' }) });
  });

  // 拦截页面发出的 WebSocket 消息，记录 resize 上报
  await page.routeWebSocket('**/webssh', ws => {
    ws.onMessage(msg => {
      wsFrames.push(msg);
      try {
        const obj = JSON.parse(msg);
        wsSent.push(obj);
        if (obj.operate) console.log('[WS→]', JSON.stringify(obj));
      } catch (e) {
        console.log('[WS→ raw]', msg);
      }
    });
  });

  // 直接访问终端页（绕过登录页，通过 /connect 的 thymeleaf 渲染需要 POST）
  // terminal.html 依赖 data.host 等模板变量，直接 GET 会拿不到值。
  // 改用 POST /connect 提交表单获取终端页。
  console.log('--- 1) 通过登录页提交进入终端页 ---');
  await page.goto('http://localhost/', { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  await page.fill('#host', '127.0.0.1');
  await page.fill('#port', '22');
  await page.fill('#username', 'test');
  await page.fill('#password', 'test');
  await page.click('#btn');
  await page.waitForTimeout(3000);

  // 检查是否进入终端页
  const url = page.url();
  console.log('当前URL:', url);
  const hasTerminal = await page.evaluate(() => !!document.querySelector('.xterm'));
  console.log('终端DOM是否存在:', hasTerminal);

  // 关键验证：FitAddon 全局可用 + terminal 实例 + resize 上报
  console.log('\n--- 2) 验证 FitAddon 与自适应 ---');
  const probe = await page.evaluate(() => {
    const out = {};
    out.fitAddonDefined = typeof FitAddon !== 'undefined';
    out.terminalDefined = typeof Terminal !== 'undefined';
    out.xtermRows = document.querySelectorAll('.xterm-rows').length;
    // 找到 terminal 实例（openTerminal 返回值存在 window 上吗？没有。改从 DOM 推断）
    const termEl = document.querySelector('.xterm');
    out.termElExists = !!termEl;
    // 触发 window resize 模拟缩放
    return out;
  });
  console.log('FitAddon 全局可用:', probe.fitAddonDefined);
  console.log('Terminal 全局可用:', probe.terminalDefined);
  console.log('终端元素存在:', probe.termElExists);
  console.log('xterm-rows 行数:', probe.xtermRows);

  // 截图：初始尺寸
  await page.screenshot({ path: 'docs/verify_terminal_initial.png', fullPage: false });
  console.log('已截图: docs/verify_terminal_initial.png');

  // 模拟窗口缩放（窄）
  console.log('\n--- 3) 模拟窗口缩放为窄屏 600x400 ---');
  await page.setViewportSize({ width: 600, height: 400 });
  await page.waitForTimeout(500);
  await page.screenshot({ path: 'docs/verify_terminal_narrow.png', fullPage: false });
  const narrowCols = await page.evaluate(() => {
    // 从 xterm-rows 实际渲染的列数无法直接取，但 terminal 实例未挂全局。
    // 用 resize 上报的 WS 消息判断。
    return null;
  });

  // 模拟窗口缩放（宽）
  console.log('--- 4) 模拟窗口缩放为宽屏 1600x1000 ---');
  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.waitForTimeout(500);
  await page.screenshot({ path: 'docs/verify_terminal_wide.png', fullPage: false });

  // 统计 WS 上报的 resize 消息
  const resizeMsgs = wsSent.filter(o => o.operate === 'resize');
  console.log('\n--- 5) WebSocket resize 上报统计 ---');
  console.log('总 WS 发送消息数:', wsSent.length);
  console.log('其中 resize 消息数:', resizeMsgs.length);
  if (resizeMsgs.length) {
    console.log('前3条 resize:', JSON.stringify(resizeMsgs.slice(0, 3)));
  }

  console.log('\n--- 6) 页面错误统计 ---');
  console.log('错误数:', errors.length);
  errors.slice(0, 10).forEach(e => console.log('  ', e));

  await browser.close();

  // 汇总结论
  console.log('\n========== 验证结论 ==========');
  const ok = probe.fitAddonDefined && probe.terminalDefined && probe.termElExists && resizeMsgs.length > 0 && errors.length === 0;
  console.log('FitAddon可用:', probe.fitAddonDefined, '| Terminal可用:', probe.terminalDefined, '| 终端渲染:', probe.termElExists);
  console.log('resize上报:', resizeMsgs.length > 0 ? '✅有' : '❌无');
  console.log('页面错误:', errors.length === 0 ? '✅无' : '❌有');
  console.log('整体:', ok ? '✅ 通过' : '❌ 未通过');
  process.exit(ok ? 0 : 1);
})();
