package com.n0xx1.livedetect.camera;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.n0xx1.livedetect.MainActivity;
import com.n0xx1.livedetect.barcode.Barcode;
import com.n0xx1.livedetect.barcode.BarcodedEntity;
import com.n0xx1.livedetect.entitydetection.DetectedEntity;
import com.n0xx1.livedetect.entitysearch.Entity;
import com.n0xx1.livedetect.entitysearch.SearchEngine.SearchResultListener;
import com.n0xx1.livedetect.entitysearch.SearchedEntity;
import com.n0xx1.livedetect.settings.PreferenceUtils;
import com.n0xx1.livedetect.staticdetection.Label;
import com.n0xx1.livedetect.staticdetection.LabeledObject;
import com.n0xx1.livedetect.staticdetection.StaticDetectEngine.DetectResultListener;
import com.n0xx1.livedetect.staticdetection.StaticDetectRequest;
import com.n0xx1.livedetect.staticdetection.StaticDetectResponse;
import com.n0xx1.livedetect.staticdetection.Text;
import com.n0xx1.livedetect.staticdetection.TextedObject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;

/** View model for handling application workflow based on camera preview. */
public class WorkflowModel extends AndroidViewModel implements SearchResultListener, DetectResultListener {

    private static String TAG = "WorkflowModel";
    /**
     * State set of the application workflow.
     */
    public enum WorkflowState {
        NOT_STARTED,
        DETECTING,
        DETECTED,
        LIVE_CONFIRMING,
        LIVE_CONFIRMED,
        STATIC_CONFIRMING,
        STATIC_CONFIRMED,
        SEARCHING,
        LIVE_SEARCHED,
        STATIC_SEARCHED
    }

    public MainActivity mainActivity;
    public Resources resources;
    public LinkedList<String> ttsBuffer = new LinkedList<String>();


    public final MutableLiveData<WorkflowState> workflowState = new MutableLiveData<>();

    public final MutableLiveData<DetectedEntity> entityToDetect = new MutableLiveData<>();
    public final MutableLiveData<SearchedEntity> searchedEntity = new MutableLiveData<>();
//    public final MutableLiveData<LabeledObject> labeledObject = new MutableLiveData<>();

    public final MutableLiveData<Bitmap> staticDetectBitmap = new MutableLiveData<>();
    public final MutableLiveData<StaticDetectRequest> staticDetectRequest = new MutableLiveData<>();
    public final MutableLiveData<StaticDetectResponse> staticDetectResponse = new MutableLiveData<>();
    public final MutableLiveData<TreeMap<Integer, StaticDetectResponse>> staticDetectedMap = new MutableLiveData<>();

    public final MutableLiveData<TextedObject> textedObject = new MutableLiveData<>();

    public final MutableLiveData<FirebaseVisionBarcode> detectedBarcode = new MutableLiveData<>();
    public final MutableLiveData<Barcode> barcode = new MutableLiveData<>();
    public final MutableLiveData<BarcodedEntity> barcodedEntity = new MutableLiveData<>();

//    public final MutableLiveData<String> detectedText = new MutableLiveData<>();
//    public final MutableLiveData<Bitmap> detectedBitmap = new MutableLiveData<>();

    private final Set<Integer> entityIdsToSearch = new HashSet<>();

    private boolean isTtsAvailable = true;
    private boolean isCameraLive = false;
    @Nullable
    private DetectedEntity confirmedEntity;

    public WorkflowModel(Application application) {
        super(application);
    }

    @MainThread
    public void setWorkflowState(WorkflowState workflowState) {
        if (!workflowState.equals(WorkflowState.LIVE_CONFIRMED)
                && !workflowState.equals(WorkflowState.SEARCHING)
//                && !workflowState.equals(WorkflowState.LIVE_SEARCHED)
                && !workflowState.equals(WorkflowState.STATIC_SEARCHED)) {
            confirmedEntity = null;
        }
        this.workflowState.setValue(workflowState);
    }

    @MainThread
    public void confirmingEntity(DetectedEntity entity, float progress) {
        boolean isConfirmed = (Float.compare(progress, 1f) == 0);
        if (isConfirmed) {
            confirmedEntity = entity;
            if (PreferenceUtils.isAutoSearchEnabled(getContext())) {
                setWorkflowState(WorkflowState.SEARCHING);
                triggerSearch(entity);
            } else {
                setWorkflowState(WorkflowState.LIVE_CONFIRMED);
            }
        } else {
            setWorkflowState(WorkflowState.LIVE_CONFIRMING);
        }
    }

    @MainThread
    public void onSearchButtonClicked() {
        if (confirmedEntity == null) {
            return;
        }

        setWorkflowState(WorkflowState.SEARCHING);
        triggerSearch(confirmedEntity);
    }

    private void triggerSearch(DetectedEntity entity) {
        Integer entityId = checkNotNull(entity.getEntityId());
        if (entityIdsToSearch.contains(entityId)) {
            // Already in searching.
            return;
        }

        entityIdsToSearch.add(entityId);
        entityToDetect.setValue(entity);
    }

    public void markCameraLive() {
        isCameraLive = true;
        entityIdsToSearch.clear();
    }

    public void setTtsAvailable(boolean bool){
        isTtsAvailable = bool;
    }

    public boolean getTtsAvailable(){
        return isTtsAvailable;
    }

    public void queueBuffer(String message) {
        if(!ttsBuffer.contains(message)) {
            ttsBuffer.add(message);
            printBuffer();
            Log.i(TAG, "******NewTtsBuffer: "+message);
        }
    }

    public void printBuffer(){
        Log.i(TAG, "******>>"+ ttsBuffer.toString());
    }


    public boolean isTtsAvailable(){
        return isTtsAvailable;
    }

    public void markCameraFrozen() {
        isCameraLive = false;
    }

    public boolean isCameraLive() {
        return isCameraLive;
    }

    @Override
    public void onSearchCompleted(DetectedEntity entity, List<Entity> products) {
        if (!entity.equals(confirmedEntity)) {
            // Drops the search result from the entity that has lost focus.
            return;
        }

        entityIdsToSearch.remove(entity.getEntityId());
        setWorkflowState(WorkflowState.LIVE_SEARCHED);
        searchedEntity.setValue(
                new SearchedEntity(getContext().getResources(), entity, products));
    }

    @Override
    public void onDetectLabelCompleted(List<Label> labels, StaticDetectRequest request) {

        entityIdsToSearch.clear();
        setWorkflowState(WorkflowState.STATIC_SEARCHED);

        LabeledObject labeledObject = new LabeledObject(getContext().getResources(), labels, request);
        StaticDetectResponse response = new StaticDetectResponse(labeledObject, request, resources);
        staticDetectResponse.setValue(response);

    }

    @Override
    public void onDetectTextCompleted(List<Text> texts, Bitmap image, Bitmap image_rect) {
        setWorkflowState(WorkflowState.STATIC_SEARCHED);
        textedObject.setValue(
                new TextedObject(getContext().getResources(), texts, image, image_rect)
        );

    }


    private Context getContext() {
        return getApplication().getApplicationContext();
    }
}
