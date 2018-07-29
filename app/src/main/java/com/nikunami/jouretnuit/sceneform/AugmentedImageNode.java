
package com.nikunami.jouretnuit.sceneform;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.concurrent.CompletableFuture;

public class AugmentedImageNode extends AnchorNode {

    private static final String TAG = "AugmentedImageNode";

    private AugmentedImage image;
    private static CompletableFuture<ModelRenderable> modelFuture;
    private static CompletableFuture<ModelRenderable> gameJamModelFuture;
    private static CompletableFuture<ModelRenderable> beastModelFuture;
    private static CompletableFuture<ModelRenderable> bloodModelFuture;
    private static CompletableFuture<ModelRenderable> finModelFuture;
    private static CompletableFuture<ModelRenderable> noctiqueModelFuture;
    private MediaPlayer _ChimePlayer;

    public AugmentedImageNode(Context context, String filename, int index, MediaPlayer chimePlayer) {

        _ChimePlayer = chimePlayer;

        switch (index) {
            case 0: modelFuture = loadGameJamModel(context, filename);
                break;
            case 1: modelFuture = loadBeastModel(context, filename);
                break;
            case 2: modelFuture = loadBloodModel(context, filename);
                break;
            case 3: modelFuture = loadFinModel(context, filename);
                break;
            case 4: modelFuture = loadNoctiqueModel(context, filename);
                break;
        }

        // Upon construction, start loading the modelFuture
        //if (modelFuture == null) {
        //    modelFuture = ModelRenderable.builder().setRegistryId("modelFuture")
        //            .setSource(context, Uri.parse(filename))
        //            .build();
        //}
    }

    private CompletableFuture<ModelRenderable> loadBeastModel(Context context, String filename)
    {
        if (beastModelFuture == null) {
            beastModelFuture = ModelRenderable.builder().setRegistryId("modelFuture")
                    .setSource(context, Uri.parse(filename))
                    .build();
        }
        _ChimePlayer.start();
        return beastModelFuture;
    }

    private CompletableFuture<ModelRenderable> loadBloodModel(Context context, String filename)
    {
        if (bloodModelFuture == null) {
            bloodModelFuture = ModelRenderable.builder().setRegistryId("modelFuture")
                    .setSource(context, Uri.parse(filename))
                    .build();
        }
        _ChimePlayer.start();

        return bloodModelFuture;
    }

    private CompletableFuture<ModelRenderable> loadFinModel(Context context, String filename)
    {
        if (finModelFuture == null) {
            finModelFuture = ModelRenderable.builder().setRegistryId("modelFuture")
                    .setSource(context, Uri.parse(filename))
                    .build();
        }
        _ChimePlayer.start();

        return finModelFuture;
    }

    private CompletableFuture<ModelRenderable> loadGameJamModel(Context context, String filename)
    {
        if (gameJamModelFuture == null)
        {
            gameJamModelFuture = ModelRenderable.builder().setRegistryId("modelFuture")
                    .setSource(context, Uri.parse(filename))
                    .build();
        }
        _ChimePlayer.start();

        return gameJamModelFuture;
    }

    private CompletableFuture<ModelRenderable> loadNoctiqueModel(Context context, String filename)
    {
        if (noctiqueModelFuture == null)
        {
            noctiqueModelFuture = ModelRenderable.builder().setRegistryId("modelFuture")
                    .setSource(context, Uri.parse(filename))
                    .build();
        }
        _ChimePlayer.start();

        return noctiqueModelFuture;
    }


    /**
     * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
     * created based on an Anchor created from the image.
     *
     * @param image captured by your camera
     */
    public void setImage(AugmentedImage image) {
        this.image = image;

        if (!modelFuture.isDone()) {
            CompletableFuture.allOf(modelFuture).thenAccept((Void aVoid) -> {
                setImage(image);
            }).exceptionally(throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
            });
        }

        setAnchor(image.createAnchor(image.getCenterPose()));

        Node node = new Node();

        Pose pose = Pose.makeTranslation(0.0f, 0.0f, 0.25f);

        node.setParent(this);
        node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
        node.setLocalRotation(new Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()));
        node.setRenderable(modelFuture.getNow(null));

    }

    public AugmentedImage getImage() {
        return image;
    }
}
