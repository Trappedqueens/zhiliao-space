"""End-to-end checks against the local deployment using synthetic test data."""
import argparse
import json
import re
import time
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / 'target/fixtures'
BASE = 'http://localhost:5173/api/ai'


def get(path, **params):
    url = BASE + path + ('?' + urllib.parse.urlencode(params) if params else '')
    with urllib.request.urlopen(url, timeout=300) as response:
        return response.read().decode('utf-8')


def upload(path, fields, files):
    boundary = 'local-' + uuid.uuid4().hex
    body = bytearray()
    for key, value in fields.items():
        body.extend(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode())
    for key, name, mime, data in files:
        body.extend(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"; filename="{name}"\r\nContent-Type: {mime}\r\n\r\n'.encode())
        body.extend(data)
        body.extend(b'\r\n')
    body.extend(f'--{boundary}--\r\n'.encode())
    request = urllib.request.Request(BASE + path, data=bytes(body), headers={'Content-Type': f'multipart/form-data; boundary={boundary}'})
    with urllib.request.urlopen(request, timeout=300) as response:
        return response.read().decode('utf-8')


def sample_pdf():
    # Minimal synthetic one-page PDF fixture, containing no user data.
    stream = b'BT /F1 14 Tf 50 700 Td (Project codename is BLUEBIRD-42. Launch day is Friday.) Tj ET'
    objects = [b'<< /Type /Catalog /Pages 2 0 R >>', b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
               b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
               b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
               b'<< /Length ' + str(len(stream)).encode() + b' >>\nstream\n' + stream + b'\nendstream']
    result = bytearray(b'%PDF-1.4\n')
    offsets = [0]
    for index, obj in enumerate(objects, 1):
        offsets.append(len(result))
        result.extend(f'{index} 0 obj\n'.encode() + obj + b'\nendobj\n')
    xref = len(result)
    result.extend(f'xref\n0 {len(offsets)}\n0000000000 65535 f \n'.encode())
    for offset in offsets[1:]:
        result.extend(f'{offset:010d} 00000 n \n'.encode())
    result.extend(f'trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF'.encode())
    return bytes(result)


def fixtures():
    from PIL import Image, ImageDraw
    import av
    FIXTURES.mkdir(parents=True, exist_ok=True)
    image = Image.new('RGB', (512, 320), 'red')
    draw = ImageDraw.Draw(image)
    draw.ellipse((150, 55, 360, 265), fill='blue')
    image.save(FIXTURES / 'colors.png')
    with av.open(str(FIXTURES / 'colors.mp4'), mode='w') as container:
        stream = container.add_stream('mpeg4', rate=10)
        stream.width = 512
        stream.height = 320
        stream.pix_fmt = 'yuv420p'
        for _ in range(20):
            frame = av.VideoFrame.from_image(image)
            for packet in stream.encode(frame):
                container.mux(packet)
        for packet in stream.encode():
            container.mux(packet)
    (FIXTURES / 'handbook.pdf').write_bytes(sample_pdf())


def healthy(text):
    assert text.strip() and '处理失败：' not in text and '连接失败' not in text, text
    return text


def run(include_media):
    run_id = 'smoke-' + uuid.uuid4().hex[:10]
    outputs = {}
    outputs['chat'] = healthy(get('/chat', chatId=run_id, prompt='请记住本次对话暗号是 BLUEBIRD-42，回复收到。'))
    outputs['memory'] = healthy(get('/chat', chatId=run_id, prompt='本次对话暗号是什么？只回复暗号。'))
    assert 'BLUEBIRD-42' in outputs['memory'], outputs['memory']
    assert len(json.loads(get('/history/chat/' + run_id))) == 4
    assert json.loads(get('/history/service/' + run_id)) == []
    outputs['game'] = healthy(get('/game', chatId=run_id, prompt='开始游戏，女友生气原因：忘记了纪念日'))
    assert '原谅值：20' in outputs['game']
    outputs['gameTurn'] = healthy(get('/game', chatId=run_id, prompt='对不起，我理解你的感受，下次我会提前安排，一起庆祝。'))
    assert '原谅值：45' in outputs['gameTurn'], outputs['gameTurn']
    for prompt in ['预约试听', 'Java', '线上', '测试用户', '13800000000', '确认预约']:
        outputs['booking'] = healthy(get('/service', chatId=run_id, prompt=prompt))
    assert '预约编号：DEMO-' in outputs['booking'] and '未向真实学校提交' in outputs['booking']
    outputs['pdfUpload'] = upload('/pdf/upload/' + run_id, {}, [('file', 'handbook.pdf', 'application/pdf', sample_pdf())])
    with urllib.request.urlopen(BASE + '/pdf/file/' + run_id) as response:
        assert response.read().startswith(b'%PDF-')
    outputs['pdf'] = healthy(get('/pdf/chat', chatId=run_id, prompt='What is the project codename?'))
    assert 'BLUEBIRD-42' in outputs['pdf'] and '参考页：1' in outputs['pdf'], outputs['pdf']
    if include_media:
        for name, mime, prompt in [('colors.png', 'image/png', '请描述图中背景和圆形的颜色。'),
                                   ('speech.wav', 'audio/wav', '请总结这段语音说的会议时间。'),
                                   ('colors.mp4', 'video/mp4', '请描述视频里背景和圆形的颜色。')]:
            path = FIXTURES / name
            outputs[name] = healthy(upload('/chat?chatId=' + run_id + '-' + name.split('.')[0],
                                         {'prompt': prompt}, [('files', name, mime, path.read_bytes())]))
            if name != 'speech.wav':
                assert '红' in outputs[name] and '蓝' in outputs[name], outputs[name]
        outputs['media'] = 'passed'
    (ROOT / 'target/smoke-results.json').write_text(json.dumps({'runId': run_id, 'outputs': outputs}, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({'runId': run_id, 'checks': list(outputs), 'passed': True}, ensure_ascii=False), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--fixtures', action='store_true')
    parser.add_argument('--media', action='store_true')
    args = parser.parse_args()
    if args.fixtures:
        fixtures()
    else:
        run(args.media)
