## FFmpeg

    是一套可以用来记录、处理数字音频、视频，并将其转换为流的开源框架
    提供了录制、转换以及流化音视频的完整解决方案
    Fast Forward MPEG（视频编码标准）

#### 编译

    ffprobe，用于探测媒体文件的格式以及详细信息
    ffplay，是一个播放媒体文件的工具
    ffmpeg，强大的媒体文件转换工具，还可以进行处理和编辑

#### 流程

    1、引用头文件：avformat.h swscale.h swresample.h pixdesc.h
    2、注册协议、格式与编码器：
        avformat_network_init();  //网络协议使用
        av_register_all();
    3、打开媒体文件源，并设置超时回调：
        AVFormatContext *formatCtx = avformat_alloc_context();
        AVIOInterruptCB int_cb = (interrupt_callback, (__bridge void *)(self));
        formatCtx->interrupt_callback = int_cb;
        acformat_open_input(formatCtx, path, NULL, NULL);
        avformat_find_stream_info(foramtCtx, NULL);
    4、寻找各个流，并且打开对应的解码器：
        for(int i=0; i < formatCtx->nb_streams; i++) {
            AVStream* stream = formatCtx->streams[i];
            if(AVMEDIA_TYPE_VIDEO == stream->codec->codec_type){
                videoStreamIndex = i;
            } else if(AVMEDIA_TYPE_AUDIO == stream->codec->codec_type){
               audioStreamIndex = i;
            }
        }
        AVCodecContext* audioCodecCtx = audioStream->codec;
        AVCodec *codec = avcodec_find_decoder(audioCodecCtx ->codec_id);
        int openCodecErrCode = avcodec_open2(codecCtx, codec, NULL);
        AVCodecContext* videoCodecCtx = videoStream->codec;
        AVCodec *codec = avcodec_find_decoder(videoCodecCtx ->codec_id);
        int openCodecErrCode = avcodec_open2(videoCodecCtx, codec, NULL);
    5、初始化解码后数据的结构体：
        SwrContext *swrContext = swr_alloc_set_opts(NULL, outputChannel, ...);
        swr_init(swrContext);
        audioFrame = avcodec_alloc_frame();

        AVPicture picture;
        avpicture_alloc(&picture, PIX_FMT_YUV420P, videoCodecCtx->width, vcc->height);
        SwsContext *swsContext = sws_getCachedContext(swsContext, vcc->width, vcc->height, ...);
        videoFrame = avcodec_alloc_frame();
    6、读取流内容并且解码：
        AVPacket packet;
        int getFrame = 0;
        av_read_frame(formatContext, &packet);
        int streamIndex = packet.stream_index;
        if(streamIndex == videoStreamIndex){
            avcodec_decode_video2(videoCodecCtx, videoFrame, &getFrame, &packet);
            if(getFrame)
                self->handleVideoFrame();  //自己处理，保存文件或者转码等
        } else if(streamIndex == audioStreamIndex){
            avcodec_decode_audio2(audioCodecCtx, audioFrame, &getFrame, &packet);
            if(getFrame)
                self->handleAudioFrame();  //自己处理，保存文件或者转码等
        }
    7、处理解码后的裸数据：
        PCM、YUV，见 P65
        av_samples_get_buffer_size(NULL, channels, ...);
        swr_convert(_swrContext, outbuf, ...);

        copyFrameData(videoFrame->data[0], videoFrame->linesize[0], videoCodecCtx->width, ..height);
        sws_scale(_swsContext, ...);
    8、关闭所有资源：
        free(swrBuffer);
        swr_free(&swrContext);
        av_free(audioFrame);
        avcodec_close(audioCodecCtx);
        sws_freeContext(swsContext);
        avpicture_free(&picture);
        av_free(videoFrame);
        avcodec_close(videoCodecCtx);
        avformat_close_input(&formatCtx);

#### 源码结构

    模块介绍
    libavformat：AVFormatContext，进行格式的封装与解封装
    libavcodec：AVPacket与AVFrame
    通用API：
        av_register_all，编解码器、封装格式、协议等
        av_find_codec，寻找编解码器
        avcodec_open2，打开编解码器
        avcodec_close，关闭对应的编解码器
    解码时用到的函数：
        avformat_open_input，根据文件路径判断文件格式，决定使用哪个Demuxer
        avformat_find_stream_info，把所有Stream的MetaData信息填充好，内部会查找对应的解码器，打开对应的解码器，利用Demuxer中的read_packet读取数据进行解码，分析流信息，可以控制读取信息的长度
        av_read_frame，该方法读取出来的数据是AVPacket，音频流，一个AVPacket可能包含多个AVFrame，视频流，一个AVPacket只包含一个AVFrame
        avcodec_decode，解码视频，解码音频
        avformat_close_input，释放对应的资源，首先调用Demuxer中的read_close，然后释放AVFormatContext，最后关闭文件或网络连接
    编码时用到的函数：
        avformat_alloc_output_context2，分配一个AVFormatContext，将找出来的格式赋值给AVFormatContext类型的oformat
        av_err2str，把整数类型的错误码转换为字符串
        avio_open2，构造出URLContext，分配出AVIOContext，赋值给AVFormatContext
        后续就是解码的逆过程
        avformat_new_stream
        avformat_write_header
        手动封装好AVFrame，作为avcodec_encode_video的输入，编码为AVPacket
        av_write_frame，输出AVPacket到媒体文件中
        av_write_trailer，与write_header成对出现
