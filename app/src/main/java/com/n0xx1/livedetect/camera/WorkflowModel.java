package com.n0xx1.livedetect.camera;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.n0xx1.livedetect.objectdetection.DetectedObject;
import com.n0xx1.livedetect.productsearch.Product;
import com.n0xx1.livedetect.productsearch.SearchEngine.SearchResultListener;
import com.n0xx1.livedetect.productsearch.SearchedObject;
import com.n0xx1.livedetect.settings.PreferenceUtils;
import com.n0xx1.livedetect.staticdetection.Label;
import com.n0xx1.livedetect.staticdetection.LabeledObject;
import com.n0xx1.livedetect.staticdetection.StaticEngine.StaticResultListener;
import com.n0xx1.livedetect.staticdetection.Text;
import com.n0xx1.livedetect.staticdetection.TextedObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/** View model for handling application workflow based on camera preview. */
public class WorkflowModel extends AndroidViewModel implements SearchResultListener, StaticResultListener {

    /**
     * State set of the application workflow.
     */
    public enum WorkflowState {
        NOT_STARTED,
        DETECTING,
        DETECTED,
        CONFIRMING,
        CONFIRMED,
        SEARCHING,
        SEARCHED
    }

    public final MutableLiveData<WorkflowState> workflowState = new MutableLiveData<>();

    public final MutableLiveData<DetectedObject> objectToSearch = new MutableLiveData<>();
    public final MutableLiveData<SearchedObject> searchedObject = new MutableLiveData<>();
    public final MutableLiveData<LabeledObject> labeledObject = new MutableLiveData<>();

    public final MutableLiveData<Bitmap> staticToDetect = new MutableLiveData<>();
    public final MutableLiveData<TextedObject> textedObject = new MutableLiveData<>();

    public final MutableLiveData<FirebaseVisionBarcode> detectedBarcode = new MutableLiveData<>();

    public final MutableLiveData<String> detectedHtml = new MutableLiveData<>();
    public final MutableLiveData<String> detectedText = new MutableLiveData<>();


    public final MutableLiveData<Bitmap> detectedImage = new MutableLiveData<>();

    private final Set<Integer> objectIdsToSearch = new HashSet<>();

    private boolean isCameraLive = false;
    @Nullable
    private DetectedObject confirmedObject;

    public WorkflowModel(Application application) {
        super(application);
    }

    @MainThread
    public void setWorkflowState(WorkflowState workflowState) {
        if (!workflowState.equals(WorkflowState.CONFIRMED)
                && !workflowState.equals(WorkflowState.SEARCHING)
                && !workflowState.equals(WorkflowState.SEARCHED)) {
            confirmedObject = null;
        }
        this.workflowState.setValue(workflowState);
    }

    @MainThread
    public void confirmingObject(DetectedObject object, float progress) {
        boolean isConfirmed = (Float.compare(progress, 1f) == 0);
        if (isConfirmed) {
            confirmedObject = object;
            if (PreferenceUtils.isAutoSearchEnabled(getContext())) {
                setWorkflowState(WorkflowState.SEARCHING);
                triggerSearch(object);
            } else {
                setWorkflowState(WorkflowState.CONFIRMED);
            }
        } else {
            setWorkflowState(WorkflowState.CONFIRMING);
        }
    }

    @MainThread
    public void onSearchButtonClicked() {
        if (confirmedObject == null) {
            return;
        }

        setWorkflowState(WorkflowState.SEARCHING);
        triggerSearch(confirmedObject);
    }

    private void triggerSearch(DetectedObject object) {
        Integer objectId = checkNotNull(object.getObjectId());
        if (objectIdsToSearch.contains(objectId)) {
            // Already in searching.
            return;
        }

        objectIdsToSearch.add(objectId);
        objectToSearch.setValue(object);
    }

    public void markCameraLive() {
        isCameraLive = true;
        objectIdsToSearch.clear();
    }

    public void markCameraFrozen() {
        isCameraLive = false;
    }

    public boolean isCameraLive() {
        return isCameraLive;
    }

    @Override
    public void onSearchCompleted(DetectedObject object, List<Product> products) {
        if (!object.equals(confirmedObject)) {
            // Drops the search result from the object that has lost focus.
            return;
        }

        objectIdsToSearch.remove(object.getObjectId());
        setWorkflowState(WorkflowState.SEARCHED);
        searchedObject.setValue(
                new SearchedObject(getContext().getResources(), confirmedObject, products));
    }

    @Override
    public void onStaticLabelCompleted(List<Label> texts, Bitmap image, Bitmap image_rect) {
        setWorkflowState(WorkflowState.SEARCHED);
        labeledObject.setValue(
                new LabeledObject(getContext().getResources(), texts, image, image_rect)
        );

    }

    @Override
    public void onStaticTextCompleted(List<Text> texts, Bitmap image, Bitmap image_rect) {
        setWorkflowState(WorkflowState.SEARCHED);
        textedObject.setValue(
                new TextedObject(getContext().getResources(), texts, image, image_rect)
        );

    }


    private Context getContext() {
        return getApplication().getApplicationContext();
    }
}
