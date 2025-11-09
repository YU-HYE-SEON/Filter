package com.example.filter.etc;

public class Face {

    /*private FrameLayout faceOverlay;
    private ImageView faceModel;
    private View stickerWrapper;
    private CheckBox checkBox;
    private ImageButton cancelBtn, checkBtn;
    private ImageView stickerImage, editFrame, rotateController, sizeController;
    String stickerId = null;
    private static final int WRAPPER_MIN_DP = 100;
    private FaceBoxOverlayView faceBox;
    private ConstraintLayout topArea;
    private FrameLayout photoPreviewContainer;
    private static final float FACE_STICKER_SCALE_BOOST = 1.3f;*/

    /*@Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_edit_my_sticker, container, false);

        return view;
    }*/

    /*@Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkBox = view.findViewById(R.id.checkBox);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        topArea = requireActivity().findViewById(R.id.topArea);
        photoPreviewContainer = requireActivity().findViewById(R.id.photoPreviewContainer);

        FrameLayout stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        stickerWrapper = inflater.inflate(R.layout.v_sticker_edit, faceOverlay, false);
        editFrame = stickerWrapper.findViewById(R.id.editFrame);
        stickerImage = stickerWrapper.findViewById(R.id.stickerImage);
        rotateController = stickerWrapper.findViewById(R.id.rotateController);
        sizeController = stickerWrapper.findViewById(R.id.sizeController);
        //stickerImage.setImageBitmap(stickerBmp);

        faceOverlay.post(() -> {
            int sizePx = Controller.dp(230, getResources());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
            stickerWrapper.setLayoutParams(lp);
            float cx = (faceOverlay.getWidth() - sizePx) / 2f;
            float cy = (faceOverlay.getHeight() - sizePx) / 2f;
            stickerWrapper.setX(cx);
            stickerWrapper.setY(cy);

            ViewParent parent = stickerWrapper.getParent();
            if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(stickerWrapper);

            faceOverlay.addView(stickerWrapper);
        });

        Controller.enableStickerControl(stickerWrapper, editFrame, rotateController, sizeController, faceModel, getResources());

        cancelBtn.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new MyStickersFragment())
                    .commit();

            //requireActivity().findViewById(R.id.faceContainer).setVisibility(View.GONE);
        });

        checkBtn.setOnClickListener(v -> {
            if (checkBox.isChecked() && photoPreviewContainer != null) {

                Controller.removeStickerFrame(stickerWrapper);

                FilterActivity activity = (FilterActivity) getActivity();
                if (activity == null) return;

                activity.getPhotoPreview().queueEvent(() -> {
                    Bitmap bmp = activity.getRenderer().getCurrentBitmap();
                    activity.runOnUiThread(() -> FaceDetect.detectFaces(bmp, faceBox, (faces, bitmap) -> {

                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .setCustomAnimations(R.anim.slide_up, 0)
                                .replace(R.id.bottomArea2, new StickersFragment())
                                .commit();

                        //requireActivity().findViewById(R.id.faceContainer).setVisibility(View.GONE);
                    }));
                });
            }
        });

        //String stickerId = args.getString("stickerId", "unknown");

        faceBox = new FaceBoxOverlayView(requireContext());
        faceOverlay.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        faceModel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int w = faceModel.getWidth();
                int h = faceModel.getHeight();
                if (w <= 0 || h <= 0) return;

                faceModel.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                faceModel.draw(c);
                FaceDetect.detectFaces(bmp, faceBox, (faces, bitmap) -> {
                    if (faces.isEmpty()) return;
                });
            }
        });
    }*/

    /*@Override
    public void onDestroyView() {
        super.onDestroyView();
        if (faceBox != null) {
            faceBox.clearBoxes();
            faceBox.setVisibility(View.GONE);
        }
    }*/
}