package com.nikunami.jouretnuit;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.Node;
import com.nikunami.jouretnuit.sceneform.AugmentedImageNode;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.nikunami.common.helpers.SnackbarHelper;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class MuseumActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private ArSceneView arSceneView;

    private boolean installRequested;

    private Session session;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();

    private boolean shouldConfigureSession = false;

    MediaPlayer musicPlayer;
    MediaPlayer chimePlayer;

    private boolean beastLoaded = false;
    private boolean bloodLoaded = false;
    private boolean finLoaded = false;
    private boolean noctiqueLoaded = false;
    private boolean nightMusicActive = true;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_museum);

        arSceneView = findViewById(R.id.surfaceview);

        installRequested = false;

        initializeSceneView();

        musicPlayer = MediaPlayer.create(this, getMusicSource(nightMusicActive));
        musicPlayer.setLooping(true);
        musicPlayer.setVolume(100,100);
        musicPlayer.start();

        chimePlayer = MediaPlayer.create(this, Uri.parse("android.resource://com.nikunami.jouretnuit/" + R.raw.chime));
        chimePlayer.setLooping(false);
        chimePlayer.setVolume(100,100);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (musicPlayer.isPlaying() == false)
        {
            musicPlayer.start();
        }

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                session = new Session(/* context = */ this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            shouldConfigureSession = true;
        }

        if (shouldConfigureSession) {
            configureSession();
            shouldConfigureSession = false;
            arSceneView.setupSession(session);
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
            arSceneView.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            messageSnackbarHelper.showError(this, "Camera not available. Please restart the app.");
            session = null;
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            arSceneView.pause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(
                    this, "Camera permissions are needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    private void initializeSceneView() {
        arSceneView.getScene().setOnUpdateListener((this::onUpdateFrame));
    }

    private void onUpdateFrame(FrameTime frameTime) {

        Frame frame = arSceneView.getArFrame();
        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {

                if (beastLoaded && bloodLoaded && noctiqueLoaded)
                {
                    nightMusicActive = false;

                    musicPlayer.stop();
                    musicPlayer.release();
                    musicPlayer = null;

                    musicPlayer = MediaPlayer.create(this, getMusicSource(nightMusicActive));
                    musicPlayer.setLooping(true);
                    musicPlayer.setVolume(100,100);
                    musicPlayer.start();
                }

                // Check camera image matches our reference image
                if (augmentedImage.getName().equals("gamejamlogo")){
                    AugmentedImageNode gameJamNode = new AugmentedImageNode(this, "Final-Controller.sfb", 0);
                    gameJamNode.setImage(augmentedImage);
                    arSceneView.getScene().addChild(gameJamNode);
                    chimePlayer.start();
                    // TODO - Move Chime Player back from AugmentedImageNode to here
                }
                if (augmentedImage.getName().equals("beast")){
                    AugmentedImageNode beastNode = new AugmentedImageNode(this, "Final-Beast.sfb", 1);
                    beastNode.setImage(augmentedImage);
                    arSceneView.getScene().addChild(beastNode);
                    chimePlayer.start();
                    beastLoaded = true;
                }
                if (augmentedImage.getName().equals("blood")){
                    AugmentedImageNode bloodNode = new AugmentedImageNode(this, "Final-Blood.sfb", 2);
                    bloodNode.setImage(augmentedImage);
                    arSceneView.getScene().addChild(bloodNode);
                    chimePlayer.start();
                    bloodLoaded = true;
                }
                /*if (augmentedImage.getName().equals("fin")){
                    AugmentedImageNode finNode = new AugmentedImageNode(this, "Rune-main-color.sfb", 3);
                    finNode.setImage(augmentedImage);
                    arSceneView.getScene().addChild(finNode);
                    finLoaded = true;
                }*/
                if (augmentedImage.getName().equals("noctique")){
                    AugmentedImageNode noctiqueNode = new AugmentedImageNode(this, "Final-Noctique-01.sfb", 4);
                    noctiqueNode.setImage(augmentedImage);
                    arSceneView.getScene().addChild(noctiqueNode);

                    chimePlayer.start();
                    noctiqueLoaded = true;
                }
                if (augmentedImage.getName().equals("reset")){
                    List<Node> SceneNodes = arSceneView.getScene().getChildren();

                    if (SceneNodes.size() > 1)
                    {
                        for (int i = SceneNodes.size() - 1; i > 1; i--) {
                            arSceneView.getScene().removeChild(SceneNodes.get(i));
                        }
                    }
                    resetChimeAndSwapMusic();
                }
            }
        }
    }

    private void resetChimeAndSwapMusic() {
        chimePlayer.release();
        chimePlayer = null;
        chimePlayer = MediaPlayer.create(this, Uri.parse("android.resource://com.nikunami.jouretnuit/" + R.raw.resetchime));
        chimePlayer.setLooping(false);
        chimePlayer.setVolume(100,100);
        chimePlayer.start();

        chimePlayer.release();
        chimePlayer = null;
        chimePlayer = MediaPlayer.create(this, Uri.parse("android.resource://com.nikunami.jouretnuit/" + R.raw.chime));
        chimePlayer.setLooping(false);
        chimePlayer.setVolume(100,100);

        beastLoaded = false;
        bloodLoaded = false;
        noctiqueLoaded = false;
        nightMusicActive = true;

        musicPlayer.stop();
        musicPlayer.release();
        musicPlayer = null;
        musicPlayer = MediaPlayer.create(this, getMusicSource(nightMusicActive));
        musicPlayer.start();
    }

    private Uri getMusicSource(boolean nightActive)
    {
        Uri sourceUri = null;

        if (nightActive)
        {
            sourceUri = Uri.parse("android.resource://com.nikunami.jouretnuit/" + R.raw.soundscapeloop);
        }
        else {
            sourceUri = Uri.parse("android.resource://com.nikunami.jouretnuit/" + R.raw.starless);
        }

        return sourceUri;
    }

    private void configureSession() {
        Config config = new Config(session);
        if (!setupAugmentedImageDb(config)) {
            messageSnackbarHelper.showError(this, "Could not setup augmented image database");
        }
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        session.configure(config);
    }

    private boolean setupAugmentedImageDb(Config config) {
        AugmentedImageDatabase augmentedImageDatabase;

        Bitmap gameJamBitmap = loadGameJamLogoAugmentedImage();
        if (gameJamBitmap == null) {
            return false;
        }

        Bitmap beastBitmap = loadBeastAugmentedImage();
        if (beastBitmap == null)
        {
            return false;
        }

        Bitmap bloodBitmap = loadBloodAugmentedImage();
        if (bloodBitmap == null)
        {
            return false;
        }

//        Bitmap finBitmap = loadFinAugmentedImage();
//        if (finBitmap == null){
//            return false;
//        }

        Bitmap noctiqueBitmap = loadNoctiqueAugmentedImage();
        if (noctiqueBitmap == null)
        {
            return false;
        }

        Bitmap resetBitmap = loadResetAugmentedImage();
        if (resetBitmap == null)
        {
            return false;
        }

        augmentedImageDatabase = new AugmentedImageDatabase(session);
        augmentedImageDatabase.addImage("gamejamlogo", gameJamBitmap);
        augmentedImageDatabase.addImage("beast", beastBitmap);
        augmentedImageDatabase.addImage("blood", bloodBitmap);
        //augmentedImageDatabase.addImage("fin", finBitmap);
        augmentedImageDatabase.addImage("noctique", noctiqueBitmap);
        augmentedImageDatabase.addImage("reset", resetBitmap);


        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }

    private Bitmap loadBeastAugmentedImage() {
        try (InputStream is = getAssets().open("TrackingImages/Beast.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
        }
        return null;
    }

    private Bitmap loadBloodAugmentedImage() {
        try (InputStream is = getAssets().open("TrackingImages/blood-picture.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
        }
        return null;
    }

//    private Bitmap loadFinAugmentedImage() {
//        try (InputStream is = getAssets().open("TrackingImages/Fin.jpg")) {
//            return BitmapFactory.decodeStream(is);
//        } catch (IOException e) {
//            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
//        }
//        return null;
//    }

    private Bitmap loadGameJamLogoAugmentedImage() {
        try (InputStream is = getAssets().open("gamejamlogo.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
        }
        return null;
    }

    private Bitmap loadNoctiqueAugmentedImage() {
        try (InputStream is = getAssets().open("TrackingImages/Noctique.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
        }
        return null;
    }

    private Bitmap loadResetAugmentedImage() {
        try (InputStream is = getAssets().open("TrackingImages/reset.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
        }
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        musicPlayer.stop();
        musicPlayer.release();
    }

    @Override
    public void onStop() {
        super.onStop();
        musicPlayer.stop();
    }

}
