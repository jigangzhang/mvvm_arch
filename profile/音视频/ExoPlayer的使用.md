
## ExoPlayer的使用

[参考](https://www.cnblogs.com/renhui/p/10822692.html)
[缓存](https://blog.csdn.net/john_chedan/article/details/80692483)
    缓存使用：
    ```
        CacheDataSource.Factory factory = new CacheDataSource.Factory();
        File dir = new File(getExternalCacheDir(), CacheManager.VIDEO_CACHE);
        cache = new SimpleCache(dir, new NoOpCacheEvictor(), new ExoDatabaseProvider(this));
        factory.setCache(cache);
        DefaultDataSourceFactory upstreamFactory = new DefaultDataSourceFactory(this);
        factory.setUpstreamDataSourceFactory(upstreamFactory);
        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(path));
        mPlayer.setMediaSource(mediaSource);
        mPlayer.setPlayWhenReady(true);
    ```
    