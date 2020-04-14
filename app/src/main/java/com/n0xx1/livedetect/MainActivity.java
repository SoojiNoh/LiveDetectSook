package com.n0xx1.livedetect;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.n0xx1.livedetect.barcode.Barcode;
import com.n0xx1.livedetect.barcode.BarcodeField;
import com.n0xx1.livedetect.barcode.BarcodeProcessor;
import com.n0xx1.livedetect.barcode.BarcodeResultFragment;
import com.n0xx1.livedetect.barcode.BarcodedEntity;
import com.n0xx1.livedetect.barcode.BarcodedProductAdapter;
import com.n0xx1.livedetect.barcode.ProductCrawlEngine;
import com.n0xx1.livedetect.camera.CameraSource;
import com.n0xx1.livedetect.camera.CameraSourcePreview;
import com.n0xx1.livedetect.camera.FrameProcessor;
import com.n0xx1.livedetect.camera.GraphicOverlay;
import com.n0xx1.livedetect.camera.WorkflowModel;
import com.n0xx1.livedetect.camera.WorkflowModel.WorkflowState;
import com.n0xx1.livedetect.entitydetection.MultiEntityProcessor;
import com.n0xx1.livedetect.entitydetection.ProminentEntityProcessor;
import com.n0xx1.livedetect.entitysearch.BottomSheetScrimView;
import com.n0xx1.livedetect.entitysearch.Entity;
import com.n0xx1.livedetect.entitysearch.EntityAdapter;
import com.n0xx1.livedetect.entitysearch.SearchEngine;
import com.n0xx1.livedetect.entitysearch.SearchedEntity;
import com.n0xx1.livedetect.settings.PreferenceUtils;
import com.n0xx1.livedetect.settings.SettingsActivity;
import com.n0xx1.livedetect.staticdetection.Label;
import com.n0xx1.livedetect.staticdetection.LabelAdapter;
import com.n0xx1.livedetect.staticdetection.PreviewCardAdapter;
import com.n0xx1.livedetect.staticdetection.StaticConfirmationController;
import com.n0xx1.livedetect.staticdetection.StaticDetectEngine;
import com.n0xx1.livedetect.staticdetection.StaticDetectProcessor;
import com.n0xx1.livedetect.staticdetection.StaticDetectResponse;
import com.n0xx1.livedetect.staticdetection.StaticObjectDotView;
import com.n0xx1.livedetect.staticdetection.Text;
import com.n0xx1.livedetect.staticdetection.TextAdapter;
import com.n0xx1.livedetect.staticdetection.TextedObject;
import com.n0xx1.livedetect.text2speech.Text2Speech;
import com.n0xx1.livedetect.textdetection.TextRecognitionProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PreviewCardAdapter.CardItemListener{


    private static final String TAG = "MainActivity";

    public static final int MULTI_MODE = 0;
    public static final int PROMI_MODE = 1;
    public static final int TEXT_MODE = 2;
    public static final int LABEL_MODE = 3;
    public static final int BARCODE_MODE = 4;
    public static int CURRENT_MODE;

    private MultiEntityProcessor multiEntityProcessor;
    private StaticDetectProcessor staticDetectProcessor;
    private ProminentEntityProcessor prominentEntityProcessor;
    private TextRecognitionProcessor textRecognitionProcessor;
    private BarcodeProcessor barcodeProcessor;
    private StaticConfirmationController staticConfirmationController;
    private StaticDetectEngine staticDetectEngine;

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
    public ProductCrawlEngine productCrawlEngine;

    private View loadingView;
    private Chip bottomPromptChip;
    private ImageView inputImageView;
    private RecyclerView previewCardCarousel;
    private ViewGroup dotViewContainer;
    private int dotViewSize;
    private int currentSelectedObjectIndex = 0;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private BottomSheetScrimView bottomSheetScrimView;
    private RecyclerView recyclerView;
    private TextView bottomSheetTitleView;
    private Bitmap entityThumbnailForBottomSheet;
    private Bitmap entityThumbnailForZoomView;
    private ImageView expandedImageView;
    private boolean slidingSheetUpFromHiddenState;

    private Barcode barcode;
    private BarcodedEntity BarcodedProductsForBottomSheet;

    private TreeMap<Integer, StaticDetectResponse> detectedResultMap = new TreeMap<>();
    private StaticDetectResponse detectedObjectForBottomSheet;

    public Text2Speech tts;

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
        loadingView = findViewById(R.id.loading_view);
        loadingView.setOnClickListener(this);

        bottomPromptChip = findViewById(R.id.bottom_prompt_chip);

        inputImageView = findViewById(R.id.input_image_view);
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

        bottomSheetScrimView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && CURRENT_MODE != BARCODE_MODE){
                    RectF thumbnailRect = bottomSheetScrimView.getThumbnailRect();

                float touchX = event.getX();
                float touchY = event.getY();

//                for(Rect rect : rectangles){
                    if(thumbnailRect.contains(touchX, touchY)){
                        bottomSheetScrimView.zoomInImageFromThumb(expandedImageView, entityThumbnailForZoomView);
                    } else {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    }
//                }
                }
                return true;
            }
        });

        setUpWorkflowModel();
        workflowModel.mainActivity = this;
        workflowModel.resources = graphicOverlay.getResources();

        staticDetectProcessor = new StaticDetectProcessor(graphicOverlay, workflowModel);
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
//        stopCameraPreview();
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
//        } else if (id == R.id.bottom_sheet_scrim_view) {
//            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

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
            }
        }

        Log.i(TAG, "****onClicked: "+getResources().getResourceName(view.getId()));
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
//            preview.stop();
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
                        TextedObject textedObject = workflowModel.textedObject.getValue();

                        if ((searchedEntity == null && textedObject == null) || Float.isNaN(slideOffset)) {
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
                        } else if (textedObject!=null){
                            bottomSheetScrimView.updateWithThumbnailTranslate(
                                    entityThumbnailForBottomSheet, collapsedStateHeight, slideOffset, bottomSheet);
                        }
                    }
                });

        bottomSheetScrimView = findViewById(R.id.bottom_sheet_scrim_view);
        bottomSheetScrimView.setOnClickListener(this);

        bottomSheetTitleView = findViewById(R.id.bottom_sheet_title);
        recyclerView = findViewById(R.id.product_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new EntityAdapter(ImmutableList.of()));
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
        workflowModel.entityToDetect.observe(
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
                        recyclerView.setAdapter(new EntityAdapter(productList));
                        slidingSheetUpFromHiddenState = true;
                        bottomSheetBehavior.setPeekHeight(preview.getHeight() / 2);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                        String result="";
                            for(int i=0 ; i<productList.size(); i++){
                                if (i!=0) result+="혹은 ";
                                result+=productList.get(i).getTitle();
                            }
                                tts.speech(result+"");
//                                    +((Entity)productList.get(1)).getTitle()+""+((Entity)productList.get(2)).getTitle());
                    }
                });

        workflowModel.barcode.observe(
                this,
                    barcode -> {
                    if (barcode != null) {
                        this.barcode = barcode;
                        ArrayList<BarcodeField> barcodeFieldList = new ArrayList<>();
                        if (barcode.getName() != null) {
                            barcodeFieldList.add(new BarcodeField("상품명", barcode.getName()));
                            tts.speech("상품을 찾았습니다. 상품명은"+barcode.getName()+"입니다.");
                            barcodeFieldList.add(new BarcodeField("상품설명", barcode.getDescription()+""));
                        }
                        else{
                            barcodeFieldList.add(new BarcodeField("상품명", "찾을 수 없음."));
                            tts.speech("상품을 찾을 수 없습니다.");
                        }
                        productCrawlEngine = new ProductCrawlEngine(workflowModel, barcode);
                        BarcodeResultFragment.show(getSupportFragmentManager(), barcodeFieldList, productCrawlEngine);
                    }

                });

        workflowModel.barcodedEntity.observe(
                this,
                barcodedEntity -> {
                    if (!barcodedEntity.getBarcodedProducts().isEmpty()) {
                        BarcodeResultFragment.dismiss(getSupportFragmentManager());
//                        List<BarcodedProduct> productList = barcodedProducts;
//                        entityThumbnailForBottomSheet = barcodedEntity.getEntityThumbnail();
                        bottomSheetTitleView.setText(
                                getResources()
                                        .getQuantityString(
                                                R.plurals.bottom_sheet_title, barcodedEntity.getBarcodedProducts().size(), barcodedEntity.getBarcodedProducts().size()));
                        tts.speech("검색결과가 나왔습니다.");
                        recyclerView.setAdapter(new BarcodedProductAdapter(barcodedEntity.getBarcodedProducts()));
                        slidingSheetUpFromHiddenState = true;
                        bottomSheetBehavior.setPeekHeight(preview.getHeight() / 2);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        workflowModel.setWorkflowState(WorkflowState.DETECTED);
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
        workflowModel.staticDetectBitmap.observe(
                this, bitmap -> {
                    inputImageView.setImageDrawable(null);
                    previewCardCarousel.setAdapter(new PreviewCardAdapter(ImmutableList.of(), this));
                    previewCardCarousel.clearOnScrollListeners();
                    dotViewContainer.removeAllViews();
                    currentSelectedObjectIndex = 0;

                    inputImageView.setImageBitmap(bitmap);
                    staticDetectProcessor.detectEntityRegion(bitmap);
                }
        );

        // Observes changes on the entity to search, if happens, fire product search request.
        workflowModel.staticDetectRequest.observe(
                this, request -> {
                    staticDetectProcessor.requestStaticDetect(request);
//                    entityThumbnailForZoomView = textBimap;
                });


        workflowModel.staticDetectResponse.observe(this,
                response -> {
                    if (response.getLabeledObject()!=null){
                        staticDetectProcessor.onStaticDetectCompleted(response.getRequest(), response);
                    } else {
                        promptChip.setText(R.string.static_image_prompt_detected_no_results);
                        tts.speech(getResources().getString(R.string.static_image_prompt_detected_no_results)+"");
                    }
                });

        workflowModel.staticDetectedMap.observe(this,
                detectedResultMap -> {
                    this.detectedResultMap = detectedResultMap;
                    StaticDetectCardView();
                });

        workflowModel.textedObject.observe(
                this,
                textedObject -> {
                    if (textedObject != null) {
                        List<Text> textList = textedObject.getTextList();
                        entityThumbnailForBottomSheet = textedObject.getTextThumbnail();
                        entityThumbnailForZoomView = textedObject.getTextRectBitmap();
                        bottomSheetTitleView.setText(
                                getResources()
                                        .getQuantityString(
                                                R.plurals.bottom_sheet_title, textList.size(), textList.size()));
                        recyclerView.setAdapter(new TextAdapter(textList));
                        slidingSheetUpFromHiddenState = true;
                        bottomSheetBehavior.setPeekHeight(preview.getHeight() / 2);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }

                }
        );


//        workflowModel.labeledObject.observe(
//                this,
//                labeledObject -> {
//                    if (labeledObject != null) {
//                        List<Label> labelList = labeledObject.getLabelList();
////                        onStaticDetectCompleted(staticDetectRequests.get(), labelList);
//
////                        entityThumbnailForBottomSheet = labeledObject.getLabelThumbnail();
//////                        entityThumbnailForZoomView = labeledObject.getLabelRectBitmap();
////                        bottomSheetTitleView.setText(
////                                getResources()
////                                        .getQuantityString(
////                                                R.plurals.bottom_sheet_title, labelList.size(), labelList.size()));
////                        recyclerView.setAdapter(new LabelAdapter(labelList));
////                        slidingSheetUpFromHiddenState = true;
////                        bottomSheetBehavior.setPeekHeight(preview.getHeight() / 2);
////                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
//                    }
//
//                }
//        );
    }

    private void stateChangeInAutoSearchMode(WorkflowState workflowState) {
        boolean wasPromptChipGone = (promptChip.getVisibility() == View.GONE);

        searchButton.setVisibility(View.GONE);
        searchProgressBar.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);

        switch (workflowState) {
            case DETECTING:
            case DETECTED:
            case LIVE_CONFIRMING:
                promptChip.setVisibility(View.VISIBLE);
                promptChip.setText(
                        workflowState == WorkflowState.LIVE_CONFIRMING
                                ? R.string.prompt_hold_camera_steady
                                : R.string.prompt_point_at_an_entity);
//                tts.speech(getResources().getString(R.string.prompt_hold_camera_steady)+"");
                startCameraPreview();
                break;
            case LIVE_CONFIRMED:
                promptChip.setVisibility(View.VISIBLE);
                promptChip.setText(R.string.prompt_searching);
                tts.speech(getResources().getString(R.string.prompt_searching)+"");
//                stopCameraPreview();
                break;
            case STATIC_CONFIRMING:
                promptChip.setVisibility(View.VISIBLE);
                startCameraPreview();
                break;
            case STATIC_CONFIRMED:
                promptChip.setVisibility(View.VISIBLE);
                promptChip.setText(R.string.prompt_searching);
                tts.speech(getResources().getString(R.string.prompt_searching)+"");
                stopCameraPreview();
                break;
            case SEARCHING:
                searchProgressBar.setVisibility(View.VISIBLE);
                promptChip.setVisibility(View.VISIBLE);
                promptChip.setText(R.string.prompt_searching);
                tts.speech(getResources().getString(R.string.prompt_searching)+"");
                stopCameraPreview();
                break;
            case LIVE_SEARCHED:
                promptChip.setVisibility(View.GONE);
//                stopCameraPreview();
                break;
            case STATIC_SEARCHED:
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
            case LIVE_CONFIRMING:
                promptChip.setVisibility(View.VISIBLE);
                promptChip.setText(R.string.prompt_point_at_an_entity);
                tts.speech(getResources().getString(R.string.prompt_point_at_an_entity)+"");
                searchButton.setVisibility(View.GONE);
                startCameraPreview();
                break;
            case LIVE_CONFIRMED:
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
            case LIVE_SEARCHED:
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

        tts.speech("object mode");
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

    public WorkflowModel getWorkflowModel(){
        return  workflowModel;
    }



    private void StaticDetectCardView(){
        showBottomPromptChip(getString(R.string.static_image_prompt_detected_results));

        previewCardCarousel.setAdapter(
                new PreviewCardAdapter(ImmutableList.copyOf(detectedResultMap.values()), this));

        previewCardCarousel.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                        Log.d(TAG, "New card scroll state: " + newState);
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                                View childView = recyclerView.getChildAt(i);
                                if (childView.getX() >= 0) {
                                    int cardIndex = recyclerView.getChildAdapterPosition(childView);
                                    if (cardIndex != currentSelectedObjectIndex) {
                                        selectNewObject(cardIndex);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                });

        for (StaticDetectResponse response : detectedResultMap.values()) {
            StaticObjectDotView dotView = createDotView(response);
            dotView.setOnClickListener(
                    v -> {
                        if (response.getRequest().getRequestIndex() == currentSelectedObjectIndex) {
                            showCardResults(response);
                        } else {
                            selectNewObject(response.getRequest().getRequestIndex());
                            showCardResults(response);
                            previewCardCarousel.smoothScrollToPosition(response.getRequest().getRequestIndex());
                        }
                    });

            dotViewContainer.addView(dotView);
            AnimatorSet animatorSet =
                    ((AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.static_image_dot_enter));
            animatorSet.setTarget(dotView);
            animatorSet.start();
        }


    }



    private void showCardResults(StaticDetectResponse response) {
        detectedObjectForBottomSheet = response;
        List<Label> labelList = response.getLabeledObject().getLabelList();
        bottomSheetTitleView.setText(
                getResources()
                        .getQuantityString(
                                R.plurals.bottom_sheet_title, labelList.size(), labelList.size()));
        recyclerView.setAdapter(new LabelAdapter(labelList));
        bottomSheetBehavior.setPeekHeight(((View) inputImageView.getParent()).getHeight() / 2);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }


    @Override
    public void onPreviewCardClicked(StaticDetectResponse response) {
        showCardResults(response);
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

    private StaticObjectDotView createDotView(StaticDetectResponse response) {
        float viewCoordinateScale;
        float horizontalGap;
        float verticalGap;
        float inputImageViewRatio = (float) inputImageView.getWidth() / inputImageView.getHeight();
        float inputBitmapRatio = (float) inputImageView.getWidth() / inputImageView.getHeight();
        if (inputBitmapRatio <= inputImageViewRatio) { // Image content fills height
            viewCoordinateScale = (float) inputImageView.getHeight() / inputImageView.getHeight();
            horizontalGap =
                    (inputImageView.getWidth() - inputImageView.getWidth() * viewCoordinateScale) / 2;
            verticalGap = 0;
        } else { // Image content fills width
            viewCoordinateScale = (float) inputImageView.getWidth() / inputImageView.getWidth();
            horizontalGap = 0;
            verticalGap =
                    (inputImageView.getHeight() - inputImageView.getHeight() * viewCoordinateScale) / 2;
        }

        Rect boundingBox = response.getLabeledObject().getBoundingBox();
        RectF boxInViewCoordinate =
                new RectF(
                        boundingBox.left * viewCoordinateScale + horizontalGap,
                        boundingBox.top * viewCoordinateScale + verticalGap,
                        boundingBox.right * viewCoordinateScale + horizontalGap,
                        boundingBox.bottom * viewCoordinateScale + verticalGap);
        boolean initialSelected = (response.getRequest().getRequestIndex() == 0);
        StaticObjectDotView dotView = new StaticObjectDotView(this, initialSelected);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(dotViewSize, dotViewSize);
        PointF dotCenter =
                new PointF(
                        (boxInViewCoordinate.right + boxInViewCoordinate.left) / 2,
                        (boxInViewCoordinate.bottom + boxInViewCoordinate.top) / 2);
        layoutParams.setMargins(
                (int) (dotCenter.x - dotViewSize / 2f), (int) (dotCenter.y - dotViewSize / 2f), 0, 0);
        dotView.setLayoutParams(layoutParams);
        return dotView;
    }

    private void selectNewObject(int objectIndex) {
        StaticObjectDotView dotViewToDeselect =
                (StaticObjectDotView) dotViewContainer.getChildAt(currentSelectedObjectIndex);
        dotViewToDeselect.playAnimationWithSelectedState(false);

        currentSelectedObjectIndex = objectIndex;

        StaticObjectDotView selectedDotView =
                (StaticObjectDotView) dotViewContainer.getChildAt(currentSelectedObjectIndex);
        selectedDotView.playAnimationWithSelectedState(true);
    }



    public void showBottomPromptChip(String message) {
        bottomPromptChip.setVisibility(View.VISIBLE);
        bottomPromptChip.setText(message);
    }


}
