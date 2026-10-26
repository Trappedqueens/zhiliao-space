"""Patch the supplied compiled Vue frontend; no original Vue source was supplied.

Run once after copying the original assets to src/main/resources/static.
Every replacement is checked so an incompatible frontend fails explicitly.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/static'


def patch(name, replacements):
    path = ROOT / name
    text = path.read_text(encoding='utf-8-sig')
    for before, after in replacements:
        if before not in text:
            if after in text:
                continue
            raise RuntimeError(f'Expected frontend fragment missing in {name}: {before[:80]}')
        text = text.replace(before, after)
    path.write_text(text, encoding='utf-8')


patch('index.html', [('<html lang="">', '<html lang="zh-CN">')])
patch('assets/api-CNdQDF4x.js', [
    ('Ke="http://localhost:8080"', 'Ke=location.port==="5173"?location.origin+"/api":location.origin'),
    ('AbortSignal.timeout(3e4)', 'AbortSignal.timeout(3e5)'),
    ('if(!a.ok)throw new Error(`HTTP error! status: ${a.status}`);', 'if(!a.ok){const err=await a.json().catch(()=>({}));return new Response(err.message||`请求失败：${a.status}`).body.getReader()}'),
    ('if(!t.ok)throw new Error(`HTTP error! status: ${t.status}`);return t.body.getReader()', 'if(!t.ok){const err=await t.json().catch(()=>({}));return new Response(err.message||`请求失败：${t.status}`).body.getReader()}return t.body.getReader()'),
    ('if(!t.ok)throw new Error(`API error: ${t.status}`);', 'if(!t.ok){const err=await t.json().catch(()=>({}));return new Response(err.message||`请求失败：${t.status}`).body.getReader()}'),
    ('throw console.error("API Error:",t),t', 'return console.error("API Error:",t),new Response("连接失败，请确认本地服务已启动后重试。").body.getReader()'),
])
patch('assets/AIChat-Ck9zItdp.js', [
    ('e.style.height=e.scrollHeight+"px"):e.style.height="50px"', 'e.style.height=e.scrollHeight+"px"):null'),
    ('if(!t.length)return;', 'if(!t.length)return;if(t.length>3){alert("每次最多上传3个附件");e.target.value="";return}'),
    ('x.decode(v)', 'x.decode(v,{stream:!0})'),
    ('R=async e=>{y.value=e;', 'R=async e=>{if(p.value)return;y.value=e;'),
    ('F=()=>{const e=', 'F=()=>{if(p.value)return;const e='),
    ('o.content="抱歉，发生了错误，请稍后重试。"', 'l.value[l.value.length-1]={...o,content:"连接失败，请检查本地服务后重试。"}'),
    ('输入消息，可上传图片、音频或视频...', '输入消息；最多3个附件，音频≤180秒、视频≤40秒（抽帧分析）...'),
])
patch('assets/CustomerService-BeLONoX9.js', [
    ('黑马程序员智能客服', '本地演示课程咨询 · 预约不会提交学校'),
    ('R.decode(l)', 'R.decode(l,{stream:!0})'),
    ('T=async s=>{f.value=s;', 'T=async s=>{if(u.value)return;f.value=s;'),
    ('y=async()=>{const s=', 'y=async()=>{if(u.value)return;const s='),
    ('r.content="抱歉，发生了错误，请稍后重试。"', 'o.value[o.value.length-1]={...r,content:"连接失败，请检查本地服务后重试。"}'),
    ('请输入您的问题...', '演示咨询；输入“预约试听”开始本地预约...'),
])
patch('assets/GameChat-CaiEv6e1.js', [
    ('j.decode(D)', 'j.decode(D,{stream:!0})'),
    ('H=async()=>{b.value', 'H=async()=>{if(m.value)return;b.value'),
    ('U=()=>{b.value', 'U=()=>{if(m.value)return;b.value'),
    ('v.content="抱歉，发生了错误，请稍后重试。"', 'n.value[n.value.length-1]={...v,content:"连接失败，请检查本地服务后重试。"}'),
])
patch('assets/ChatPDF-CW_8SwJM.js', [
    ('const l=URL.createObjectURL(x.file);', 'const l=x.file.previewUrl||URL.createObjectURL(x.file);'),
    ('const r=URL.createObjectURL(o);return', 'const r=o.previewUrl||URL.createObjectURL(o);return'),
    ('F.value=new File([C],h,{type:"application/pdf"})}catch', 'F.value=new File([C],h,{type:"application/pdf"}),F.value.previewUrl=`${V}/ai/pdf/view/${t}`}catch'),
    ('D.value=e.name,F.value=e;const v=', 'D.value=e.name,F.value=e,F.value.previewUrl=`${V}/ai/pdf/view/${l.value}`;const v='),
    ('V="http://localhost:8080"', 'V=location.port==="5173"?location.origin+"/api":location.origin'),
    ('if(!f.ok)throw new Error(`上传失败: ${f.status}`);', 'if(!f.ok){const err=await f.json().catch(()=>({}));throw new Error(err.message||`上传失败: ${f.status}`)}'),
    ('alert("文件上传失败，请重试")', 'alert(i.message||"文件上传失败，请重试")'),
    ('X=()=>{try{', 'X=()=>{if(w.value||o.value)return;try{'),
    ('O=async t=>{if(t){', 'O=async t=>{if(w.value||o.value)return;if(t){'),
    ('点击上传，或将PDF拖拽到此处', '上传文字型 PDF（≤20MB、200页；暂不支持扫描件）'),
])
patch('assets/Home-BQiH0-pw.js', [
    ('黑马 AI 应用中心', '智聊空间'),
    ('多模态对话机器人，支持图片、音频等', '本地文字、图片、语音与视频关键帧问答'),
    ('24小时在线的智能课程咨询师', '演示课程咨询与本地试听预约'),
    ('黑马智能客服', '课程咨询（演示）'),
    ('打造你的个人知识库，与知识库自由对话', '上传文字型 PDF，按文档片段与页码问答'),
])
patch('assets/index-yHA_Ioyc.js', [
    ('Heima AI Hub', '智聊空间'),
    ('{path:"/comfort-simulator",name:', '{path:"/comfort-simulator",redirect:"/game",name:'),
])
# The supplied filenames contain the old build hashes. Give patched modules new
# URLs as well, otherwise browsers can retain the original modules across reloads.
modules = [p for p in (ROOT / 'assets').glob('*.js') if '-local-v' not in p.name]
names = {p.name: p.stem + '-local-v3.js' for p in modules}
for source in modules:
    text = source.read_text(encoding='utf-8')
    for original, renamed in names.items():
        text = text.replace(original, renamed)
    source.with_name(names[source.name]).write_text(text, encoding='utf-8')
index = ROOT / 'index.html'
text = index.read_text(encoding='utf-8')
text = re.sub(r'-local-v\d+\.js', '-local-v3.js', text)
text = re.sub(r'<title>.*?</title>', '<title>智聊空间</title>', text)
text = text.replace('<link rel="icon" href="/favicon.ico">', '<link rel="icon" type="image/svg+xml" href="/favicon.svg">')
if '/assets/app-fixes.css' not in text:
    text = text.replace('</head>', '<link rel="stylesheet" href="/assets/app-fixes.css">\n  </head>')
for original, renamed in names.items():
    text = text.replace(original, renamed)
index.write_text(text, encoding='utf-8')
print('Frontend patches applied.')
