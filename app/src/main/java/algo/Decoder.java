package algo;

import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.IOException;

public class Decoder {
    private MediaExtractor mMediaExtractor;
    public Decoder(String path) {
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String s;
        for (int i=0;i<mMediaExtractor.getTrackCount();i++) {
            MediaFormat format = mMediaExtractor.getTrackFormat(i);
            s = format.getString(MediaFormat.KEY_MIME);
            if (s.startsWith("video/")) {
                break;
            }
        }

//        MediaCodecList.findDecoderForFormat(s);
    }
}
