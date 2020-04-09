package com.n0xx1.livedetect;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.n0xx1.livedetect.barcode.BarcodeField;
import com.n0xx1.livedetect.barcode.BarcodeProcessor;
import com.n0xx1.livedetect.barcode.BarcodeResultFragment;
import com.n0xx1.livedetect.camera.CameraSource;
import com.n0xx1.livedetect.camera.CameraSourcePreview;
import com.n0xx1.livedetect.camera.FrameProcessor;
import com.n0xx1.livedetect.camera.GraphicOverlay;
import com.n0xx1.livedetect.camera.WorkflowModel;
import com.n0xx1.livedetect.camera.WorkflowModel.WorkflowState;
import com.n0xx1.livedetect.entitydetection.MultiEntityProcessor;
import com.n0xx1.livedetect.entitydetection.ProminentEntityProcessor;
import com.n0xx1.livedetect.productsearch.BottomSheetScrimView;
import com.n0xx1.livedetect.productsearch.Entity;
import com.n0xx1.livedetect.productsearch.EntityAdapter;
import com.n0xx1.livedetect.productsearch.SearchEngine;
import com.n0xx1.livedetect.productsearch.SearchedEntity;
import com.n0xx1.livedetect.settings.PreferenceUtils;
import com.n0xx1.livedetect.settings.SettingsActivity;
import com.n0xx1.livedetect.staticdetection.Label;
import com.n0xx1.livedetect.staticdetection.LabelAdapter;
import com.n0xx1.livedetect.staticdetection.StaticConfirmationController;
import com.n0xx1.livedetect.staticdetection.StaticEngine;
import com.n0xx1.livedetect.staticdetection.Text;
import com.n0xx1.livedetect.staticdetection.TextAdapter;
import com.n0xx1.livedetect.staticdetection.TextedEntity;
import com.n0xx1.livedetect.text2speech.Text2Speech;
import com.n0xx1.livedetect.textdetection.TextRecognitionProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int MULTI_MODE = 0;
    private static final int PROMI_MODE = 1;
    private static final int TEXT_MODE = 2;
    private static final int LABEL_MODE = 3;
    private static final int BARCODE_MODE = 4;
    private static int CURRENT_MODE;

    private MultiEntityProcessor multiEntityProcessor;
    private ProminentEntityProcessor prominentEntityProcessor;
    private TextRecognitionProcessor textRecognitionProcessor;
    private BarcodeProcessor barcodeProcessor;
    private StaticConfirmationController staticConfirmationController;
    private StaticEngine staticEngine;

    private FrameProcessor frameProcessor;
    private CameraSource cameraSource;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private View settingsButton;
    private View flashButton;
    private View textButton;
    private View barcodeButton;
    private Chip promptChip;
    private AnimatorSet promptChipAnimator;
    private ExtendedFloatingActionButton searchButton;
    private AnimatorSet searchButtonAnimator;
    private ProgressBar searchProgressBar;
    private WorkflowModel workflowModel;
    private WorkflowState currentWorkflowState;
    private SearchEngine searchEngine;
    private Text2Speech tts;
    private Chip bottomPromptChip;
    private RecyclerView previewCardCarousel;
    private ViewGroup dotViewContainer;
    private int dotViewSize;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private BottomSheetScrimView bottomSheetScrimView;
    private RecyclerView productRecyclerView;
    private TextView bottomSheetTitleView;
    private Bitmap entityThumbnailForBottomSheet;
    private Bitmap entityThumbnailForZoomView;
    private ImageView expandedImageView;
    private boolean slidingSheetUpFromHiddenState;

    private final TreeMap<Integer, SearchedEntity> searchedEntityMap = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        preview = findViewById(R.id.camera_preview);
        graphicOverlay = findViewById(R.id.camera_preview_graphic_overlay);
        graphicOverlay.setOnClickListener(this);
        cameraSource = new CameraSource(graphicOverlay);

        expandedImageView = findViewById(R.id.expanded_image);

        promptChip = findViewById(R.id.bottom_prompt_chip);
        promptChipAnimator =
                (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.bottom_prompt_chip_enter);
        promptChipAnimator.setTarget(promptChip);

        searchButton = findViewById(R.id.product_search_button);
        searchButton.setOnClickListener(this);
        searchButtonAnimator =
                (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.search_button_enter);
        searchButtonAnimator.setTarget(searchButton);
        searchProgressBar = findViewById(R.id.search_progress_bar);

        bottomPromptChip = findViewById(R.id.bottom_prompt_chip);

        previewCardCarousel = findViewById(R.id.card_recycler_view);
        previewCardCarousel.setHasFixedSize(true);
        previewCardCarousel.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        previewCardCarousel.addItemDecoration(new CardItemDecoration(getResources()));

        dotViewContainer = findViewById(R.id.dot_view_container);
        dotViewSize = getResources().getDimensionPixelOffset(R.dimen.static_image_dot_view_size);

        setUpBottomSheet();

        findViewById(R.id.close_button).setOnClickListener(this);
        flashButton = findViewById(R.id.flash_button);
        flashButton.setOnClickListener(this);
        settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(this);
        textButton = findViewById(R.id.text_button);
        textButton.setOnClickListener(this);
        barcodeButton = findViewById(R.id.barcode_button);
        barcodeButton.setOnClickListener(this);

        tts = new Text2Speech(getApplicationContext(), this);
        searchEngine = new SearchEngine(getApplicationContext(), this);

        setUpWorkflowModel();

        staticConfirmationController = new StaticConfirmationController(graphicOverlay, workflowModel, getApplicationContext());
        multiEntityProcessor = new MultiEntityProcessor(graphicOverlay, workflowModel, staticConfirmationController);
        prominentEntityProcessor = new ProminentEntityProcessor(graphicOverlay, workflowModel, staticConfirmationController);
        textRecognitionProcessor = new TextRecognitionProcessor(graphicOverlay, workflowModel, staticConfirmationController);
        barcodeProcessor = new BarcodeProcessor(graphicOverlay, workflowModel);

    }

    @Override
    protected void onResume() {
        super.onResume();

        workflowModel.markCameraFrozen();
        settingsButton.setEnabled(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        currentWorkflowState = WorkflowState.NOT_STARTED;

        setEntityMode();
        workflowModel.setWorkflowState(WorkflowState.DETECTING);

    }

    @Override
    protected void onPause() {
        super.onPause();
        currentWorkflowState = WorkflowState.NOT_STARTED;
        stopCameraPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
            cameraSource = null;
        }
        searchEngine.shutdown();
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.product_search_button) {
            searchButton.setEnabled(false);
            workflowModel.onSearchButtonClicked();

        } else if (id == R.id.bottom_sheet_scrim_view) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
//            bottomSheetScrimView.zoomImageFromThumb(expandedImageView, entityThumbnailForZoomView);
        } else if (id == R.id.close_button) {
            onBackPressed();

        } else if (id == R.id.flash_button) {
            if (flashButton.isSelected()) {
                flashButton.setSelected(false);
                cameraSource.updateFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            } else {
                flashButton.setSelected(true);
                cameraSource.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }

        } else if (id == R.id.settings_button) {
            // Sets as disabled to prevent the user from clicking on it too fast.
            settingsButton.setEnabled(false);
            startActivity(new Intent(this, SettingsActivity.class));

        } else if (id == R.id.text_button) {
            if (textButton.isSelected()) {
                textButton.setSelected(false);
                setEntityMode();
            } else {
                textButton.setSelected(true);
                barcodeButton.setSelected(false);
                setProcessor(TEXT_MODE);
                tts.speech("text mode");
                Toast.makeText(getApplicationContext(),
                        "text detection mode", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.barcode_button) {
            if (barcodeButton.isSelected() && !textButton.isSelected()) {
                barcodeButton.setSelected(false);
                setEntityMode();
            } else {
                barcodeButton.setSelected(true);
                textButton.setSelected(false);
                setProcessor(BARCODE_MODE);
                tts.speech("barcode mode");
                Toast.makeText(getApplicationContext(),
                        "barcode detection mode", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void startCameraPreview() {
        if (!workflowModel.isCameraLive() && cameraSource != null) {
            try {
                workflowModel.markCameraLive();
                preview.start(cameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Failed to start camera preview!", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    private void stopCameraPreview() {
        if (workflowModel.isCameraLive()) {
            workflowModel.markCameraFrozen();
            flashButton.setSelected(false);
            barcodeButton.setSelected(false);
            preview.stop();
        }
    }

    private void setUpBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
        bottomSheetBehavior.setBottomSheetCallback(
                new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        Log.d(TAG, "Bottom sheet new state: " + newState);
                        bottomSheetScrimView.setVisibility(
                                newState == BottomSheetBehavior.STATE_HIDDEN ? View.GONE : View.VISIBLE);
                        graphicOverlay.clear();

                        switch (newState) {
                            case BottomSheetBehavior.STATE_HIDDEN:
                                workflowModel.setWorkflowState(WorkflowState.DETECTING);
                                break;
                            case BottomSheetBehavior.STATE_COLLAPSED:
                            case BottomSheetBehavior.STATE_EXPANDED:
                            case BottomSheetBehavior.STATE_HALF_EXPANDED:
                                slidingSheetUpFromHiddenState = false;
                                break;
                            case BottomSheetBehavior.STATE_DRAGGING:
                            case BottomSheetBehavior.STATE_SETTLING:
                            default:
                                break;
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        SearchedEntity searchedEntity = workflowModel.searchedEntity.getValue();
                        TextedEntity textedEntity = workflowModel.textedEntity.getValue();

                        if ((searchedEntity == null && textedEntity == null) || Float.isNaN(slideOffset)) {
                            return;
                        }

                        int collapsedStateHeight =
                                Math.min(bottomSheetBehavior.getPeekHeight(), bottomSheet.getHeight());

                        if(searchedEntity!=null) {
                            if (slidingSheetUpFromHiddenState) {
                                RectF thumbnailSrcRect =
                                        graphicOverlay.translateRect(searchedEntity.getBoundingBox());
                                bottomSheetScrimView.updateWithThumbnailTranslateAndScale(
                                        entityThumbnailForBottomSheet,
                                        collapsedStateHeight,
                                        slideOffset,
                                        thumbnailSrcRect);

                            } else {

                            }
                        } else if (textedEntity!=null){
                            bottomSheetScrimView.updateWithThumbnailTranslate(
                                    entityThumbnailForBottomSheet, collapsedStateHeight, slideOffset, bottomSheet);
                        }
                    }
                });

        bottomSheetScrimView = findViewById(R.id.bottom_sheet_scrim_view);
        bottomSheetScrimView.setOnClickListener(this);

        bottomSheetTitleView = findViewById(R.id.bottom_sheet_title);
        productRecyclerView = findViewById(R.id.product_recycler_view);
        productRecyclerView.setHasFixedSize(true);
        productRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productRecyclerView.setAdapter(new EntityAdapter(ImmutableList.of()));
    }

    private void setUpWorkflowModel() {
        workflowModel = ViewModelProviders.of(this).get(WorkflowModel.class);

        // Observes the workflow state changes, if happens, update the overlay view indicators and
        // camera preview state.
        workflowModel.workflowState.observe(
                this,
                workflowState -> {
                    if (workflowState == null || Objects.equal(currentWorkflowState, workflowState)) {
                        return;
                    }

                    currentWorkflowState = workflowState;
                    Log.d(TAG, "Current workflow state: " + currentWorkflowState.name());

                    if (PreferenceUtils.isAutoSearchEnabled(this)) {
                        stateChangeInAutoSearchMode(workflowState);
                    } else {
                        stateChangeInManualSearchMode(workflowState);
                    }
                });

        // Observes changes on the entity to search, if happens, fire product search request.
        workflowModel.entityToSearch.observe(
                this, entity -> {
                    entityThumbnailForZoomView = entity.getBitmap();
                    searchEngine.search(this, entity, workflowModel);
                });

        // Observes changes on the entity that has search completed, if happens, show the bottom sheet
        // to present search result.
        workflowModel.searchedEntity.observe(
                this,
                searchedEntity -> {
                    if (searchedEntity != null) {
                        List<Entity> productList = searchedEntity.getEntityList();
                        entityThumbnailForBottomSheet = searchedEntity.getEntityThumbnail();
                        bottomSheetTitleView.setText(
                                getResources()
                                        .getQuantityString(
                                                R.plurals.bottom_sheet_title, productList.size(), productList.size()));
                        productRecyclerView.setAdapter(new EntityAdapter(productList));
                        slidingSheetUpFromHiddenState = true;
                        bottomSheetBehavior.setPeekHeight(preview.getHeight() / 2);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                });

        workflowModel.barcodedEntity.observe(
                this,
                barcodedEntity -> {

                    if (barcodedEntity != null) {
                        ArrayList<BarcodeField> barcodeFieldList = new ArrayList<>();
                        barcodeFieldList.add(new BarcodeField("Result", barcodedEntity.getName()));
                        BarcodeResultFragment.show(getSupportFragmentManager(), barcodeFieldList);
                    }
                });

        workflowModel.barcodedProducts.observe(
                this,
                barcodedProducts -> {
                    if (!barcodedProducts.getEntityList().isEmpty()) {

                    }
                }
        );

//        workflowModel.detectedText.observe(
//                this,
//                textBlocks -> {
//                    if (textBlocks != null) {
//                        bottomSheetScrimView.setVisibility(View.VISIBLE);
//                        ArrayList<TextField> textFieldList = new ArrayList<>();
////                        String text;
////                        for (int i = 0; i < textBlocks.size(); i++) {
////                            List<FirebaseVisionText.Line> lines = textBlocks.get(i).getLines();
////                            for (int j = 0; j < lines.size(); j++) {
////                                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
////                                for (int k = 0; k < elements.size(); k++) {
////                                    text = elements.get(k).getText();
//                        textFieldList.add(new TextField("Result", textBlocks));
////                                }
////                            }
////                        }
//                        TextResultFragment.show(getSupportFragmentManager(), textFieldList);
//                        for (TextField textField : textFieldList){
//                            tts.speech(textField.toString());
//                        }
//                    }
//                }
//        );

        // Observes changes on the entity to search, if happens, fire product search request.
        workflowModel.staticToDetect.observe(
                this, image -> {
                    staticEngine = new StaticEngine(getApplicationContext(), workflowModel, graphicOverlay);
                    if (CURRENT_MODE == TEXT_MODE)
                        staticEngine.detectText(image, workflowModel);
                    else if (CURRENT_MODE == PROMI_MODE || CURRENT_MODE == MULTI_MODE)
                        staticEngine.detectLabel(image, workflowModel);
//                    entityThumbnailForZoomView = textBimap;
                });

        workflowModel.textedEntity.observe(
                this,
                textedEntity -> {
                    if (textedEntity != null) {
                        List<Text> textList = textedEntity.getTextList();
                        entityThumbnailForBottomSheet = textedEntity.getTextThumbnail();
                        entityThumbnailForZoomView = textedEntity.getTextRectBitmap();
                        bottomSheetTitleView.setText(
                                getResources()
                                        .getQuantityString(
                                                R.plurals.bottom_sheet_title, textList.size(), textList.size()));
                        productRecyclerView.setAdapter(new TextAdapter(textList));
                        slidingSheetUpFromHiddenState = true;
                        bottomSheetBehavior.setPeekHeight(preview.getHeight() / 2);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }

                }
        );

        workflowModel.labeledEntity.observe(
                this,
                labeledEntity -> {
                    if (labeledEntity != null) {
                        List<Label> labelList = labeledEntity.getLabelList();
                        entityThumbnailForBottomSheet = labeledEntity.getLabelThumbnail();
//                        entityThumbnailForZoomView = labeledEntity.getLabelRectBitmap();
                        bottomSheetTitleView.setText(
                                getResources()
                                        .getQuantityString(
                                                R.plurals.bottom_sheet_title, labelList.size(), labelList.size()));
                        productRecyclerView.setAdapter(new LabelAdapter(labelList));
                        slidingSheetUpFromHiddenState = true;
                        bottomSheetBehavior.setPeekHeight(preview.getHeight() / 2);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }

                }
        );
    }

    private void stateChangeInAutoSearchMode(WorkflowState workflowState) {
        boolean wasPromptChipGone = (promptChip.getVisibility() == View.GONE);

        searchButton.setVisibility(View.GONE);
        searchProgressBar.setVisibility(View.GONE);
        switch (workflowState) {
            case DETECTING:
            case DETECTED:
            case CONFIRMING:
                promptChip.setVisibility(View.VISIBLE);
                promptChip.setText(
                        workflowState == WorkflowState.CONFIRMING
                                ? R.string.prompt_hold_camera_steady
                                : R.string.prompt_point_at_an_entity);
                startCameraPreview();
                break;
            case CONFIRMED:
                promptChip.setVisibility(View.VISIBLE);
                promptChip.setText(R.string.prompt_searching);
                stopCameraPreview();
                break;
            case SEARCHING:
                searchProgressBar.setVisibility(View.VISIBLE);
                promptChip.setVisibility(View.VISIBLE);
                promptChip.setText(R.string.prompt_searching);
                stopCameraPreview();
                break;
            case SEARCHED:
                promptChip.setVisibility(View.GONE);
                stopCameraPreview();
                break;
            default:
                promptChip.setVisibility(View.GONE);
                break;
        }

        boolean shouldPlayPromptChipEnteringAnimation =
                wasPromptChipGone && (promptChip.getVisibility() == View.VISIBLE);
        if (shouldPlayPromptChipEnteringAnimation && !promptChipAnimator.isRunning()) {
            promptChipAnimator.start();
        }
    }

    private void stateChangeInManualSearchMode(WorkflowState workflowState) {
        boolean wasPromptChipGone = (promptChip.getVisibility() == View.GONE);
        boolean wasSearchButtonGone = (searchButton.getVisibility() == View.GONE);

        searchProgressBar.setVisibility(View.GONE);
        switch (workflowState) {
            case DETECTING:
            case DETECTED:
            case CONFIRMING:
                promptChip.setVisibility(View.VISIBLE);
                promptChip.setText(R.string.prompt_point_at_an_entity);
                searchButton.setVisibility(View.GONE);
                startCameraPreview();
                break;
            case CONFIRMED:
                promptChip.setVisibility(View.GONE);
                searchButton.setVisibility(View.VISIBLE);
                searchButton.setEnabled(true);
                searchButton.setBackgroundColor(Color.WHITE);
                startCameraPreview();
                break;
            case SEARCHING:
                promptChip.setVisibility(View.GONE);
                searchButton.setVisibility(View.VISIBLE);
                searchButton.setEnabled(false);
                searchButton.setBackgroundColor(Color.GRAY);
                searchProgressBar.setVisibility(View.VISIBLE);
                stopCameraPreview();
                break;
            case SEARCHED:
                promptChip.setVisibility(View.GONE);
                searchButton.setVisibility(View.GONE);
                stopCameraPreview();
                break;
            default:
                promptChip.setVisibility(View.GONE);
                searchButton.setVisibility(View.GONE);
                break;
        }

        boolean shouldPlayPromptChipEnteringAnimation =
                wasPromptChipGone && (promptChip.getVisibility() == View.VISIBLE);
        if (shouldPlayPromptChipEnteringAnimation && !promptChipAnimator.isRunning()) {
            promptChipAnimator.start();
        }

        boolean shouldPlaySearchButtonEnteringAnimation =
                wasSearchButtonGone && (searchButton.getVisibility() == View.VISIBLE);
        if (shouldPlaySearchButtonEnteringAnimation && !searchButtonAnimator.isRunning()) {
            searchButtonAnimator.start();
        }
    }

    private void setEntityMode(){
        if (PreferenceUtils.isMultipleEntitysMode(this))
            setProcessor(MULTI_MODE);
        else
            setProcessor(PROMI_MODE);

        tts.speech("entity mode");
        Toast.makeText(getApplicationContext(),
                "entity detection mode", Toast.LENGTH_SHORT).show();
    }

    private void setProcessor(int i){

        if(frameProcessor!=null && !(CURRENT_MODE==BARCODE_MODE)){
            staticConfirmationController.disactivate();
        }

        CURRENT_MODE = i;

        switch(i) {
            case MULTI_MODE:
                staticConfirmationController.activate(LABEL_MODE);
                frameProcessor = multiEntityProcessor;
                break;
            case PROMI_MODE:
                staticConfirmationController.activate(LABEL_MODE);
                frameProcessor = prominentEntityProcessor;
                break;
            case TEXT_MODE:
                staticConfirmationController.activate(TEXT_MODE);
                frameProcessor = textRecognitionProcessor;
                break;
            case BARCODE_MODE:
                frameProcessor = barcodeProcessor;
                break;
        }

        cameraSource.setFrameProcessor(frameProcessor);

    }

    private static class CardItemDecoration extends RecyclerView.ItemDecoration {

        private final int cardSpacing;

        private CardItemDecoration(Resources resources) {
            cardSpacing = resources.getDimensionPixelOffset(R.dimen.preview_card_spacing);
        }

        @Override
        public void getItemOffsets(
                @NonNull Rect outRect,
                @NonNull View view,
                @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {
            int adapterPosition = parent.getChildAdapterPosition(view);
            outRect.left = adapterPosition == 0 ? cardSpacing * 2 : cardSpacing;
            if (parent.getAdapter() != null
                    && adapterPosition == parent.getAdapter().getItemCount() - 1) {
                outRect.right = cardSpacing;
            }
        }
    }
}
