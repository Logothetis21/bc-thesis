package gr.xmp.torstream.activities;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.MediaController;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatSeekBar;
import com.bumptech.glide.Glide;
import com.google.android.material.textview.MaterialTextView;
import org.libtorrent4j.*;
import org.libtorrent4j.alerts.*;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;
import java.io.File;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import gr.xmp.torstream.R;
import gr.xmp.torstream.databinding.ActivityStreamingBinding;

public class StreamingActivity extends AppCompatActivity {
    private static final String TAG = "STREAM";
    private SessionManager session;
    private SurfaceView mVideoSurface = null;
    private LibVLC mLibVLC = null;
    private IVLCVout vlcVout;
    private MediaPlayer mMediaPlayer = null;
    private MediaController controller;
    private File video_file;
    private static int file_first_piece_index_final;
    private Set<Integer> set = new HashSet<>();
    private static int PieceIndexFlow;
    private static Priority init_val_for_priorites = Priority.IGNORE;
    private static boolean VIDEO_PAUSED_MANUAL = false;


    private String vPath;
    private static int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    private ArrayList<String> VLC_OPTIONS = new ArrayList<String>() {
        {
            add("--no-drop-late-frames");
            add("--no-skip-frames");
            add("-vvv");
        }
    };
    static String magnet;
    private static int fileIndx;
    private static TorrentHandle torrentHandle;
    private static int first_piece_index_of_video;
    private static int last_piece_index_of_video;
    private Animation blink_animation = new AlphaAnimation(1, 0);


    ActivityStreamingBinding binding;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        binding = ActivityStreamingBinding.inflate(getLayoutInflater());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(binding.getRoot());
        config_vlc();
        config_blinking_img_animation();
        fileIndx = getIntent().getIntExtra("fileIdx",0);
        magnet = getIntent().getStringExtra("magnet");
        Thread main_thread = new Thread(() -> {
            torrent_download();
        }); main_thread.start();

        binding.goBack.setOnClickListener(v -> {
            if(mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
                mMediaPlayer.stop();
            }
            stopLogging();
            torrent_piece_handler.removeCallbacks(torrent_video_thread);
            if(session != null)
                if(session.isDhtRunning())
                    new Thread(() -> {session.stopDht(); session.stop();}).start();
            if(main_thread.isAlive()) main_thread.interrupt();
            finish();
        });

        Glide.with(this).load(getIntent().getStringExtra("logo_url")).into(binding.logo);
        binding.logo.startAnimation(blink_animation);


    }
    private void config_blinking_img_animation(){
        blink_animation = new AlphaAnimation(1, 0); //to change visibility from visible to invisible
        blink_animation.setDuration(1500); //1 second duration for each animation cycle
        blink_animation.setInterpolator(new LinearInterpolator());
        blink_animation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
        blink_animation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
    }


    private void torrent_handle_current(int current){
        for(int l=current; l<last_piece_index_of_video;  l++){
            if(set.size() == 5) break;
            if(!torrentHandle.havePiece(l)){
                set.add(l);
                torrentHandle.piecePriority(l , Priority.TOP_PRIORITY);
            }
        }
        print_set();
    }
    private void torrent_handle_next(){
        for(int l=first_piece_index_of_video; l<last_piece_index_of_video;  l++){
            if(set.size() == 5) break;
            if(!torrentHandle.havePiece(l)){
                set.add(l);
                torrentHandle.piecePriority(l , Priority.TOP_PRIORITY);
            }
        }

        int max_set = Collections.max(set)+1;
        for(int i=max_set; i<max_set+5; i++) {
            if (i > last_piece_index_of_video) break;
            torrentHandle.piecePriority(i, Priority.DEFAULT);
        }


        print_set();
    }

    private void print_set(){
        Iterator<Integer> iterator = set.iterator();
        Log.d(TAG,"SET - TOP PRIORITY");
        while(iterator.hasNext()) {
            Integer s = iterator.next();
            Log.d(TAG,s + " " + (torrentHandle.havePiece(s) ? "Available":"Unavailable") +" " + torrentHandle.piecePriority(s));
        }
        int max_set = Collections.max(set)+1;
        for(int s=max_set; s<max_set+5; s++) {
            if (s > last_piece_index_of_video) break;
            Log.d(TAG, s + " " + (torrentHandle.havePiece(s) ? "Available" : "Unavailable") + " " + torrentHandle.piecePriority(s));
        }

        Log.d(TAG,"Download Speed: " + DownloadSpeed(session.stats().downloadRate()));
    }
    public static String DownloadSpeed(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }

    private boolean set_is_complete(){
        if(set.isEmpty()) return false;
        Iterator<Integer> iterator = set.iterator();
        while(iterator.hasNext()) {
            Integer s = iterator.next();
            if(!torrentHandle.havePiece(s)) return false;
        }
        return true;
    }
    private void config_vlc(){
        mLibVLC = new LibVLC(this, VLC_OPTIONS);

        mMediaPlayer = new MediaPlayer(mLibVLC);

        mVideoSurface = findViewById(R.id.video_surface);
        mVideoSurface.setSystemUiVisibility(flags);
        vlcVout = mMediaPlayer.getVLCVout();
        vlcVout.setVideoView(mVideoSurface);

        vlcVout.attachViews();
        controller = new MediaController(this);
        controller.setMediaPlayer(playerInterface);
        controller.setSystemUiVisibility(flags);
        controller.setAnchorView(findViewById(R.id.video_surface));
        controller.getChildAt(0).setBackgroundColor(getColor(R.color.transparent));
        LinearLayout bar = (LinearLayout) ((LinearLayout) controller.getChildAt(0)).getChildAt(1);
        bar.setPadding(26,10,26,10);
        AppCompatSeekBar seekBar = (AppCompatSeekBar) bar.getChildAt(1);

        bar.removeViewAt(1);
        bar.addView(seekBar, 2);
        ((MaterialTextView) bar.getChildAt(0)).setTextSize(20f);
        ((MaterialTextView) bar.getChildAt(1)).setTextSize(20f);


        findViewById(R.id.video_surface).setOnClickListener(v -> {
            if(binding.logo.getVisibility() == View.VISIBLE) return;
            controller.show(5000);
        });

    }
    void torrent_download() {
        String link = magnet;

        session = new SessionManager();
        if(session.isRunning()) session.stop();

        AlertListener l = new TorrentAlert(); session.addListener(l); if (session.isRunning() != true) session.start();
        try {
            waitForNodesInDHT(session);
        } catch (InterruptedException e) {
            return;
        }
        File output_folder =  new File(getFilesDir() , "/torrents");
        byte[] data = session.fetchMagnet(link, 10, output_folder);
        TorrentInfo ti = TorrentInfo.bdecode(data);
        Priority[] priorities = Priority.array(Priority.IGNORE, ti.numFiles());
        priorities[fileIndx] = Priority.TOP_PRIORITY;

        first_piece_index_of_video = ti.files().pieceIndexAtFile(fileIndx);
        file_first_piece_index_final = first_piece_index_of_video;
        last_piece_index_of_video = ti.files().lastPieceIndexAtFile(fileIndx);
        PieceIndexFlow = first_piece_index_of_video;

        Log.d(TAG, "file index: " + fileIndx);
        Log.d(TAG, "Piece Length Total: " + ti.numPieces());
        Log.d(TAG, "piece index at file: " + ti.files().pieceIndexAtFile(fileIndx));
        Log.d(TAG, "last piece index at file: " + ti.files().lastPieceIndexAtFile(fileIndx));
        Log.d(TAG, "file path: " + ti.files().filePath(fileIndx , "torrents"));

        session.download(ti, output_folder, null, priorities, null, TorrentFlags.DEFAULT_DONT_DOWNLOAD);
        torrentHandle = session.find(ti.infoHash());
        if(torrentHandle.isValid()) {
            vPath = torrentHandle.torrentFile().files().filePath(fileIndx, "torrents");
            video_file = new File(getFilesDir() + "/" + vPath);
        }

        config_piece_priorities();
        config_first_and_last_piece_priorities();


        torrent_piece_handler.post(torrent_video_thread);
    }
    static void config_first_and_last_piece_priorities(){
        // Init 6 first and last piecies with TOP Priority. + ---------- +
        for(int k=0; k<=5; k++) torrentHandle.piecePriority(first_piece_index_of_video + k, Priority.TOP_PRIORITY);
        for(int k=last_piece_index_of_video; k>=last_piece_index_of_video-5; k--) torrentHandle.piecePriority(k , Priority.TOP_PRIORITY);
    }
    static void config_piece_priorities(){
        for(int i=first_piece_index_of_video; i<last_piece_index_of_video; i++) torrentHandle.piecePriority(i , init_val_for_priorites);
    }


    private static void waitForNodesInDHT(final SessionManager s) throws InterruptedException {
        if(s.isDhtRunning()){Log.d(TAG,"DHT IS STILL RUNNING...");
        }

        final CountDownLatch signal = new CountDownLatch(1);
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override public void run() {
                long nodes = s.stats().dhtNodes();
                if (nodes >= 10) {
                    signal.countDown();
                    timer.cancel();
                }
            }
        }, 0, 1000);


        boolean r = signal.await(10, TimeUnit.SECONDS);
        if (!r) {
            Log.d(TAG,"DHT CONNECTION FAILED");
            waitForNodesInDHT(s);
        }
    }
    private static boolean original_piecies_ready(){
        int s = file_first_piece_index_final;
        int e = last_piece_index_of_video;
        boolean l = false,f = false;
        if(torrentHandle.isValid()) {
            l = torrentHandle.havePiece(e)
                    && torrentHandle.havePiece(e - 1)
                    && torrentHandle.havePiece(e - 2)
                    && torrentHandle.havePiece(e - 3)
                    && torrentHandle.havePiece(e - 4);
            f = torrentHandle.havePiece(s)
                    && torrentHandle.havePiece(s + 1)
                    && torrentHandle.havePiece(s + 2)
                    && torrentHandle.havePiece(s + 3)
                    && torrentHandle.havePiece(s + 4);
        }
        return l && f;
    }

    private void setMediaVLC(){
        try {
            // uses file:// or http?
            String file = "file://" + getFilesDir() + "/" + vPath;

            Media m = new Media(mLibVLC, Uri.parse(file));
            m.setHWDecoderEnabled(true, true);
            m.addOption(":network-caching=500");
            m.addOption(":clock-jitter=0");
            m.addOption(":clock-synchro=0");
            m.addOption(":no-dr");
            m.addOption(":drop-late-frames");
            m.addOption(":skip-frames");
            m.addOption(":live-caching=500");
            mMediaPlayer.setMedia(m);

            m.release();
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int screenHeight = metrics.heightPixels;
            int screenWidth = metrics.widthPixels;
            mMediaPlayer.getVLCVout().setWindowSize(screenWidth, screenHeight);
            mMediaPlayer.setAspectRatio("16:9");
            mMediaPlayer.play();
            binding.logo.getAnimation().cancel();
            binding.logo.setVisibility(View.GONE);

            startLogging();
        } catch (Exception e) {
            Log.e(TAG, "Error creating Media: " + e.getMessage(), e);
        }
    }

    private Handler video_handler = new Handler();
    private Runnable updateTimeTask = new Runnable() {
        @Override public void run() {
            if (mMediaPlayer != null && vlcVout != null) {
                int pos =  playerInterface.getCurrentPosition();
                int duration = playerInterface.getDuration();
                if(duration == 0) {video_handler.postDelayed(this, 1000); return;}
                long bytes_per_second = video_file.length() / duration;
                long byte_pos =  (pos * bytes_per_second);
                int piece_index = (int) (byte_pos / torrentHandle.torrentFile().pieceLength());


                // i Believe here piece_index is always playable but! we always gonna check the next.
                if (!torrentHandle.havePiece(piece_index + 1)) {
                    Log.d(TAG, "(1) Stopping Playback. Index -> " + (piece_index + 1) + " is not ready.");
                    mMediaPlayer.pause();
                    if(binding.logo.getAnimation() == null)  binding.logo.startAnimation(blink_animation);
                    if(binding.logo.getVisibility() == View.GONE) binding.logo.setVisibility(View.VISIBLE);
                    print_set();
                } else {
                    Log.d(TAG, "We continue the playback next piece -> " + (piece_index + 1) + " is ready.");

                    if(VIDEO_PAUSED_MANUAL == true && mMediaPlayer.isPlaying()) {mMediaPlayer.pause(); VIDEO_PAUSED_MANUAL = false;}
                    else if(VIDEO_PAUSED_MANUAL == false && !mMediaPlayer.isPlaying()) mMediaPlayer.play();

                    if(binding.logo.getAnimation() != null){
                        binding.logo.getAnimation().cancel();
                        binding.logo.setVisibility(View.GONE);
                    }
                }

            }

            video_handler.postDelayed(this, 1000);
        }
    };
    public void startLogging() {video_handler.post(updateTimeTask); }
    public void stopLogging() {video_handler.removeCallbacks(updateTimeTask);}

    private void clear_set(){
         set.clear();
    }

    private MediaController.MediaPlayerControl playerInterface = new MediaController.MediaPlayerControl() {
        public int getBufferPercentage() {return 0;}
        public int getCurrentPosition() {
            if(vlcVout != null) {
                float pos = mMediaPlayer.getPosition();
                return (int) (pos * getDuration());
            }
            return 0;
        }

        public int getDuration() {
            return  vlcVout != null ? (int)mMediaPlayer.getLength() : 0;
        }
        public boolean isPlaying() {
            return vlcVout != null ? mMediaPlayer.isPlaying() : false;
        }
        public void pause() {
            if(vlcVout != null) mMediaPlayer.pause();
            VIDEO_PAUSED_MANUAL = true;
            Log.d(TAG,"VIDEO PAUSED!");
        }

        public void seekTo(int pos) {
            if (vlcVout == null) {
                Log.e(TAG, "VLCVout is not initialized");
                return;
            }
            long bytes_per_second = video_file.length() / getDuration();
            long byte_pos = pos * bytes_per_second;
            int piece_index = (int) (byte_pos / torrentHandle.torrentFile().pieceLength());
            Log.d(TAG, "Seek Piece Index: " + piece_index + " Status: " + (torrentHandle.havePiece(piece_index) ? "Ready" : "Not Ready"));
            if(torrentHandle.havePiece(piece_index)) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setPosition((float) pos / getDuration(), false);
                    if(!mMediaPlayer.isPlaying()) mMediaPlayer.play();
                }
            }

        }

        public void start() {
            if(vlcVout != null) mMediaPlayer.play();
            VIDEO_PAUSED_MANUAL = false;

        }
        public boolean canPause() {
            return true;
        }
        public boolean canSeekBackward() {
            return true;
        }
        public boolean canSeekForward() {
            return true;
        }
        @Override public int getAudioSessionId() {
            return 0;
        }
    };
    private class TorrentAlert implements AlertListener{

        @Override public int[] types() {return null;}
        @Override public void alert(Alert<?> alert) {
            switch (alert.type()) {
                case ADD_TORRENT: ((AddTorrentAlert) alert).handle().resume(); break;
                case PIECE_FINISHED:
                    int index = (int) (((PieceFinishedAlert) alert).pieceIndex());
                    Log.d(TAG , "Piece: " + index + " completed");
                    break;
                case TORRENT_FINISHED: ((TorrentFinishedAlert) alert).handle().pause();   break;
                case METADATA_RECEIVED: Log.d(TAG, "metadata received"); break;
                default: break;
            }
        }
    }

    private boolean media_is_set = false;
    private Handler torrent_piece_handler = new Handler();
    private Runnable torrent_video_thread = new Runnable() {
        @Override public void run() {
            Log.d("SPEED","Download Speed: " + DownloadSpeed(session.stats().downloadRate()) + " DHT(" + session.stats().dhtNodes() +")");


            if(set_is_complete()) {
                clear_set();
                torrent_handle_next();
            }

            if(original_piecies_ready()) {
                if(media_is_set == false) {
                    media_is_set = true;
                    torrent_handle_next();
                    setMediaVLC();
                }
            }
            torrent_piece_handler.postDelayed(this, 1000);
        }
    };

    private void release_libvlc(){
        if(mLibVLC == null) return;
        mMediaPlayer.stop();
        mMediaPlayer.getVLCVout().detachViews();
        mVideoSurface = null;
        mLibVLC.release();
        mLibVLC = null;
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        release_libvlc();
        if(vlcVout != null){
            vlcVout.detachViews();
            vlcVout = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mLibVLC != null) {
            mLibVLC.release();
            mLibVLC = null;
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if (mLibVLC == null) mLibVLC = new LibVLC(this, VLC_OPTIONS);
        if (mMediaPlayer == null)  mMediaPlayer = new MediaPlayer(mLibVLC);
    }
    @Override protected void onStop() {
        super.onStop();
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.getVLCVout().detachViews();
        }
    }
}