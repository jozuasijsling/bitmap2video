# bitmap2video
![](bitmap2video.gif)

Generate video from a Bitmap, Canvas, or resource drawable in Android.

Create mp4 video from Bitmaps or anything you can draw to a hardware accelerated Canvas.  Pure, simple Android MediaCodec implementation.  Requires no third party libs or NDK.

Currently supports the MP4 container and both AVC/H264 and HEVC/H265. Easily extensible to other
 supported formats.  

Run the sample app or check out
and [MainActivity](app/src/main/java/jozua/sijsling/bitmap2video/MainActivity.kt)
for an example.

# Dependencies
Ths fork is not yet available. Get the [old version](https://github.com/israel-fl/bitmap2video).


# Initialize library
Simply create a `Muxer` object

```kotlin
val muxer = Muxer(this@MainActivity, "/files/video.mp4")
// and mux
muxer.mux(imageArray)
```

Use callbacks to listen for video completion:
```kotlin
muxer.setOnMuxingCompletedListener(object : MuxingCompletionListener {
    override fun onVideoSuccessful(file: File) {
        Log.d(TAG, "Video muxed - file path: ${file.absolutePath}")
    }

    override fun onVideoError(error: Throwable) {
        Log.e(TAG, "There was an error muxing the video")
    }
})

Thread {
    muxer.mux(imageArray, R.raw.bensound_happyrock)
}.start()
```

### Passing a custom configuration object 
```kotlin
val muxerConfig = MuxerConfig(this, 600, 600, 'video/avc', 3, 1F, 1500000)
val muxer = Muxer(this@MainActivity, muxerConfig!!)
// or
muxer.muxerConfig = muxerConfig
```

#### Supported configuration
- File object
- video width
- video height
- Mimetype
- Frames per image (how many seconds to display each image)
- Frames per second
- Bitrate
- FrameMuxer (only MP4 included currently)
- IFrame Interval

### Acceptable media types:
The library currently supports `Bitmap`, `Canvas`, and drawable resources (`R.drawable.image1`)

### Adding Audio
In order to add audio, pass in an audio track to the `mux` method.
```kotlin
muxer.mux(imageArray, R.raw.bensound_happyrock)
```

### Convenience utility functions
We provide a few functions to simplify a couple of tasks. These can be
found as static methods under `FileUtils`

##### Get a `File` object for your video
`getVideoFile(final Context context, final String fileName)`
