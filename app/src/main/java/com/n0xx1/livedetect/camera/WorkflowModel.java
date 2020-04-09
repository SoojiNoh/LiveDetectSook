package com.n0xx1.livedetect.camera;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.n0xx1.livedetect.barcode.BarcodedEntity;
import com.n0xx1.livedetect.barcode.BarcodedProducts;
import com.n0xx1.livedetect.entitydetection.DetectedEntity;
import com.n0xx1.livedetect.productsearch.Entity;
import com.n0xx1.livedetect.productsearch.SearchEngine.SearchResultListener;
import com.n0xx1.livedetect.productsearch.SearchedEntity;
import com.n0xx1.livedetect.settings.PreferenceUtils;
import com.n0xx1.livedetect.staticdetection.Label;
import com.n0xx1.livedetect.staticdetection.LabeledEntity;
import com.n0xx1.livedetect.staticdetection.StaticEngine.StaticResultListener;
import com.n0xx1.livedetect.staticdetection.Text;
import com.n0xx1.livedetect.staticdetection.TextedEntity;

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

    public final MutableLiveData<DetectedEntity> entityToSearch = new MutableLiveData<>();
    public final MutableLiveData<SearchedEntity> searchedEntity = new MutableLiveData<>();
    public final MutableLiveData<LabeledEntity> labeledEntity = new MutableLiveData<>();

    public final MutableLiveData<Bitmap> staticToDetect = new MutableLiveData<>();
    public final MutableLiveData<TextedEntity> textedEntity = new MutableLiveData<>();

    public final MutableLiveData<FirebaseVisionBarcode> detectedBarcode = new MutableLiveData<>();

    public final MutableLiveData<BarcodedEntity> barcodedEntity = new MutableLiveData<>();
    public final MutableLiveData<BarcodedProducts> barcodedProducts = new MutableLiveData<>();
    public final MutableLiveData<String> detectedText = new MutableLiveData<>();


    public final MutableLiveData<Bitmap> detectedImage = new MutableLiveData<>();

    private final Set<Integer> entityIdsToSearch = new HashSet<>();

    private boolean isCameraLive = false;
    @Nullable
    private DetectedEntity confirmedEntity;

    public WorkflowModel(Application application) {
        super(application);
    }

    @MainThread
    public void setWorkflowState(WorkflowState workflowState) {
        if (!workflowState.equals(WorkflowState.CONFIRMED)
                && !workflowState.equals(WorkflowState.SEARCHING)
                && !workflowState.equals(WorkflowState.SEARCHED)) {
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
                setWorkflowState(WorkflowState.CONFIRMED);
            }
        } else {
            setWorkflowState(WorkflowState.CONFIRMING);
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
        entityToSearch.setValue(entity);
    }

    public void markCameraLive() {
        isCameraLive = true;
        entityIdsToSearch.clear();
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
        setWorkflowState(WorkflowState.SEARCHED);
        searchedEntity.setValue(
                new SearchedEntity(getContext().getResources(), confirmedEntity, products));
    }

    @Override
    public void onStaticLabelCompleted(List<Label> texts, Bitmap image, Bitmap image_rect) {
        setWorkflowState(WorkflowState.SEARCHED);
        labeledEntity.setValue(
                new LabeledEntity(getContext().getResources(), texts, image, image_rect)
        );

    }

    @Override
    public void onStaticTextCompleted(List<Text> texts, Bitmap image, Bitmap image_rect) {
        setWorkflowState(WorkflowState.SEARCHED);
        textedEntity.setValue(
                new TextedEntity(getContext().getResources(), texts, image, image_rect)
        );

    }


    private Context getContext() {
        return getApplication().getApplicationContext();
    }
}
