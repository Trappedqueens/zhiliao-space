"""Local media extraction. Stdout is one JSON object; never invokes a shell."""
import argparse
import base64
import io
import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL = Path(os.environ.get('APP_WHISPER_MODEL_DIR', ROOT / 'data' / 'models' / 'whisper-base'))


def whisper(download=False):
    from faster_whisper import WhisperModel
    if download:
        from huggingface_hub import snapshot_download
        snapshot_download('Systran/faster-whisper-base', local_dir=str(MODEL))
    return WhisperModel(str(MODEL), device='cpu', compute_type='int8', local_files_only=True)


def jpeg(image):
    image = image.convert('RGB')
    image.thumbnail((1024, 1024))
    output = io.BytesIO()
    image.save(output, format='JPEG', quality=85)
    return base64.b64encode(output.getvalue()).decode('ascii')


def extract(path, kind):
    import av
    from PIL import Image, ImageOps
    result = {'images': [], 'text': '', 'duration': 0, 'sampleTimes': []}
    if kind == 'image':
        Image.MAX_IMAGE_PIXELS = 25_000_000
        with Image.open(path) as image:
            result['images'] = [jpeg(ImageOps.exif_transpose(image))]
        return result
    with av.open(str(path)) as container:
        duration = (container.duration or 0) / av.time_base
        if duration <= 0:
            raise ValueError('无法确定媒体时长，请转换为常见音视频格式。')
        limit = 40 if kind == 'video' else 180
        if duration > limit + .5:
            raise ValueError(f'媒体时长超过{limit}秒。')
        result['duration'] = round(duration, 2)
        has_audio = bool(container.streams.audio)
        if kind == 'video':
            if not container.streams.video:
                raise ValueError('文件没有视频画面。')
            # Decode at most four evenly spaced frames. Video is sampled, not exhaustively watched.
            stream = container.streams.video[0]
            for point in [duration * x / 4 for x in range(4)]:
                container.seek(int(point / stream.time_base), stream=stream)
                for frame in container.decode(video=0):
                    if frame.time is None or frame.time >= point:
                        result['images'].append(jpeg(frame.to_image()))
                        result['sampleTimes'].append(round(frame.time or point, 2))
                        break
    if has_audio:
        segments, _ = whisper().transcribe(str(path), beam_size=3, vad_filter=True)
        result['text'] = '\n'.join(f'[{s.start:.1f}s] {s.text.strip()}' for s in segments)
    elif kind == 'audio':
        raise ValueError('文件没有音轨。')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--download', action='store_true')
    parser.add_argument('--kind', choices=['image', 'audio', 'video'])
    parser.add_argument('--file')
    args = parser.parse_args()
    try:
        if args.download:
            whisper(True)
            print(json.dumps({'ready': True}))
        else:
            print(json.dumps(extract(Path(args.file), args.kind), ensure_ascii=True))
    except Exception as error:
        print(json.dumps({'error': str(error)}, ensure_ascii=True))
        sys.exit(1)
