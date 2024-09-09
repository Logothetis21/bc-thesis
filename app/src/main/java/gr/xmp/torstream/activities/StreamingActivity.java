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
import android.widget.MediaController;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;


import org.libtorrent4j.*;
import org.libtorrent4j.alerts.*;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.io.File;
import java.io.IOException;
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
import gr.xmp.torstream.VideoServer;
import gr.xmp.torstream.databinding.ActivityStreamingBinding;


public class StreamingActivity extends AppCompatActivity {
    private static final String TAG = "STREAM";
    private SessionManager session;

    private SurfaceView mVideoSurface = null;
    private LibVLC mLibVLC = null;
    private IVLCVout vlcVout;
    private MediaPlayer mMediaPlayer = null;
    private MediaController controller;
    private Media m;
    private File video_file;
    private static int file_first_piece_index_final;
    int port = 8083;
    private Set<Integer> set = new HashSet<>(); // Set that holds in Queque the Torrent Piecies. Always 5.
    private static int PieceIndexFlow;

    // We init every piece with IGNORE. Afterwards we init first,last 5 piecies.
    // We get + -------------------- + (File)
    // Afterwards we set ++-------------- +
    // +++----------------------+
    // and so on.
    private static Priority init_val_for_priorites = Priority.IGNORE;


    private VideoServer server = null; // Server that serves the video file
    private String vPath; // Video File Path
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
        fileIndx = getIntent().getIntExtra("fileIdx",0);
        magnet = getIntent().getStringExtra("magnet");
        binding.goBack.setOnClickListener(v -> {
            new Thread(() ->{ session.stopDht();
                session.stop();}).start();
            finish();});

        new Thread(() -> {try {torrent_download();} catch (InterruptedException e) {throw new RuntimeException(e);}
        }).start();
    }


    private void torrent_handle_current(){
        int e = PieceIndexFlow;
        PieceIndexFlow = e;
        int loop = 5; // Next Piecies to init
        for(int l=e; l<last_piece_index_of_video;  l++){
            if(loop == 0) break;
            if(!torrentHandle.havePiece(l)){
                set.add(l); loop--;
                torrentHandle.piecePriority(l , Priority.TOP_PRIORITY);
            }
        }
        print_set();
    }

    /** Sets THREE Priotity to Next 5 Piecies ahead of DEFAULT PRIORITY PIECIES **/
    /** 5[TOP_PRIOT] 5[DEFAULT_PRIO] 5[THREE_PRIO] **/
    private void torrent_handle_THREE_priority_after_DEFAULT(){
        int loop = 5;
        Log.d(TAG, "torrent_handle_THREE_priority_after_DEFAULT");
        for(int i=PieceIndexFlow; i<last_piece_index_of_video; i++) {
            if (!torrentHandle.havePiece(i) &&
                    torrentHandle.piecePriority(i) != Priority.TOP_PRIORITY
                    && torrentHandle.piecePriority(i) != Priority.DEFAULT) {
                if (loop == 0) break;
                torrentHandle.piecePriority(i, Priority.THREE);
                loop--;
            }
        }
    }

    /** Sets Default Priotity to Next 5 Piecies ahead of Set That are Incomplete **/
    private void torrent_handle_default_priority_after_top(){
        int loop = 5;
        Log.d(TAG, "torrent_handle_default_priority_after_top");
        for(int i=PieceIndexFlow; i<last_piece_index_of_video; i++) {
            if (!torrentHandle.havePiece(i) && torrentHandle.piecePriority(i) != Priority.TOP_PRIORITY) {
                if (loop == 0) break;
                torrentHandle.piecePriority(i, Priority.DEFAULT);
                loop--;
            }
        }
    }

    /** Sets Top Priority to incomplete piecies and updates set<T> datatype **/
    private void torrent_handle_next(){
        int e = PieceIndexFlow+6;
        PieceIndexFlow = e;
        int loop = 5; // Next Piecies to init
        for(int l=e; l<last_piece_index_of_video;  l++){
            if(loop == 0) break;
            if(!torrentHandle.havePiece(l)){
                set.add(l); loop--;
                torrentHandle.piecePriority(l , Priority.TOP_PRIORITY);
            }
        }
        torrent_handle_default_priority_after_top();
        torrent_handle_THREE_priority_after_DEFAULT();
        print_set();
    }
    private void print_set(){
        Log.d(TAG,"SET - QUEQUE (TOP_PRIORITY)");
        for(Integer s : set) Log.d(TAG,s + " Piece Priority: " + torrentHandle.piecePriority(s));
        Log.d(TAG,"NEXT DEFAULT PRIORITY PIECIES");
        int n = Collections.max(set) + 1;
        for(int i=n; i<n+5; i++) Log.d(TAG,i + " Piece Priority: " + torrentHandle.piecePriority(i));
        n+=5;
        Log.d(TAG,"NEXT (THREE) PRIORITY PIECIES");
        for(int i=n; i<n+5; i++) Log.d(TAG,i + " Piece Priority: " + torrentHandle.piecePriority(i));
    }
    private boolean set_is_complete(){
        if(set.isEmpty()) return false;
        for(Integer s : set) if(!torrentHandle.havePiece(s)) return false;
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

        findViewById(R.id.video_surface).setOnClickListener(v -> {
            controller.show(5000);
        });

    }
    void torrent_download() throws InterruptedException {
        String link = magnet;

        session = new SessionManager();
        AlertListener l = new TorrentAlert(); session.addListener(l); if (session.isRunning() != true) session.start();
        waitForNodesInDHT(session);
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
        vPath = torrentHandle.torrentFile().files().filePath(fileIndx , "torrents");
        video_file = new File(getFilesDir() + "/" + vPath);

        config_piece_priorities();
        config_first_and_last_piece_priorities();
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
        final CountDownLatch signal = new CountDownLatch(1);
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override public void run() {
                long nodes = s.stats().dhtNodes();
                if (nodes >= 10) {
                    System.out.println("DHT contains " + nodes + " nodes");
                    signal.countDown();
                    timer.cancel();
                }
            }
        }, 0, 1000);

        System.out.println("Waiting for nodes in DHT (10 seconds)...");
        boolean r = signal.await(10, TimeUnit.SECONDS);
        if (!r) {
            System.out.println("DHT bootstrap timeout");
            System.exit(0);
        }
    }
    private static boolean original_piecies_ready(){
        int s = file_first_piece_index_final;
        int e = last_piece_index_of_video;
        boolean l = torrentHandle.havePiece(e)
                && torrentHandle.havePiece(e-1)
                && torrentHandle.havePiece(e-2)
                && torrentHandle.havePiece(e-3)
                && torrentHandle.havePiece(e-4);
        boolean f = torrentHandle.havePiece(s)
                && torrentHandle.havePiece(s+1)
                && torrentHandle.havePiece(s+2)
                && torrentHandle.havePiece(s+3)
                && torrentHandle.havePiece(s+4);
        return l && f;
    }
    private void setMediaVLC(){
        controller.setAnchorView(findViewById(R.id.video_surface));
        controller.getChildAt(0).setBackgroundColor(getColor(R.color.transparent));
        try {
            // uses file:// or http?
            String http = "http://localhost:" + port;
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
            startLogging();
        } catch (Exception e) {
            Log.e(TAG, "Error creating Media: " + e.getMessage(), e);
        }
    }

    private int seek_pos = -1;
    private Handler handler = new Handler();
    private Runnable updateTimeTask = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer != null && vlcVout != null && mMediaPlayer.isPlaying()) {
                int pos =  playerInterface.getCurrentPosition();
                int duration = playerInterface.getDuration();
                if(duration == 0) return;
                long bytes_per_second = video_file.length() / duration;
                long byte_pos =  (pos * bytes_per_second);
                int piece_index = (int) (byte_pos / torrentHandle.torrentFile().pieceLength());
                // i Believe here piece_index is always playable but! we always gonna check the next.
                if(!torrentHandle.havePiece(piece_index+1)){
                    Log.d(TAG , "Stopping Playback. Reason: Index-> " + (piece_index+1) + " is not ready.");
                    playerInterface.pause();
                    // We stop downloading the other piecies an we focus on this.
                    clear_set_and_piecies_priorities();
                    PieceIndexFlow = piece_index+1;
                    torrent_handle_current();
                }else{
                    Log.d(TAG , "We continue the playback next piece -> " + (piece_index+1) + " is ready.");
                    // We continue downloading the set piecies as normal
                    //if(!playerInterface.isPlaying()) playerInterface.start();
                }
            }else if(mMediaPlayer != null && vlcVout != null && !mMediaPlayer.isPlaying()){
                if(torrentHandle.havePiece(PieceIndexFlow)) {
                    if (seek_pos != -1) {
                        mMediaPlayer.setPosition((float) seek_pos / playerInterface.getDuration());
                        playerInterface.start();
                        seek_pos = -1;
                    }else{
                        playerInterface.start();
                    }
                }
            }

            handler.postDelayed(this, 1000);
        }
    };
    public void startLogging() {handler.post(updateTimeTask); }
    public void stopLogging() {handler.removeCallbacks(updateTimeTask);}

    private void clear_set_and_piecies_priorities(){
        Iterator<Integer> iterator = set.iterator();
        while(iterator.hasNext()) {
            Integer s = iterator.next();
            if(torrentHandle.piecePriority(s) == Priority.TOP_PRIORITY)
                torrentHandle.piecePriority(s , Priority.IGNORE);
        } set.clear();
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
        }

        public void seekTo(int pos) {
            if (vlcVout == null) {
                Log.e(TAG, "VLCVout is not initialized");
                return;
            }
            long bytes_per_second = video_file.length() / getDuration();
            long byte_pos = pos * bytes_per_second;
            int piece_index = (int) (byte_pos / torrentHandle.torrentFile().pieceLength());
            Log.d(TAG, "Seek Piece Index: " + piece_index );
            seek_pos = pos;
            if(torrentHandle.havePiece(piece_index)) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setPosition((float) pos / getDuration(), false);
                    if(!mMediaPlayer.isPlaying()) mMediaPlayer.play();
                }
            }else{
                /** Piece Unavailable **/
                /*FIXME SELECT SEEK INDEX, CLEAR PRIORITIES OF SET,
                   SET TOP_PRIORITY TO SEEK PIECE INDEX AND MAKE MEDIA WAIT UNTIL PIECE IS AVAILABLE
                **/

                //mMediaPlayer.setPosition((float) pos / getDuration(), false);
                //pause();
            }
        }

        public void start() {
            if(vlcVout != null)
                mMediaPlayer.play();
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

                    if(set_is_complete()){
                        clear_set_and_piecies_priorities();
                        torrent_handle_next();
                    }

                    if(original_piecies_ready()) {
                        if(server == null) {
                            // Run Once
                            /** Testing::Comment **/
                            torrent_handle_next();
                            try {
                                server = new VideoServer(port , getFilesDir() + "/" + vPath);
                                server.start();
                            } catch (IOException e) {throw new RuntimeException(e);}
                            setMediaVLC();
                        }
                    }


                    break;
                case TORRENT_FINISHED: ((TorrentFinishedAlert) alert).handle().pause();   break;
                case METADATA_RECEIVED: Log.d(TAG, "metadata received"); break;
                default: break;
            }
        }
    }


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
        stopLogging();
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