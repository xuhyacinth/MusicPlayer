# FLAC 回归样本

这里的两个文件均为人工生成的测试信号，不包含歌曲内容。

- 采样率：48000 Hz；双声道；4800 帧（0.1 秒）。
- 第 n 帧左声道的 16 位采样值为 `(n % 257 - 128) * 200`，右声道取相反数。
- 16 位样本按有符号小端 PCM 编码；24 位样本的左声道为上述数值左移 8 位再加上 `n % 251`，右声道取相反数，按小端 PCM 编码。低八位包含非零数据，用于检测是否误降为 16 位。
- 使用 FFmpeg 将原始 PCM 无损编码为对应位深的 FLAC：

```text
ffmpeg -f s16le -ar 48000 -ac 2 -i stereo-16.pcm -map_metadata -1 -c:a flac stereo-48000-16bit.flac
ffmpeg -f s24le -ar 48000 -ac 2 -i stereo-24.pcm -map_metadata -1 -c:a flac stereo-48000-24bit.flac
```

测试直接读取已生成的 FLAC，不依赖本机安装 FFmpeg 或可用音频设备。
