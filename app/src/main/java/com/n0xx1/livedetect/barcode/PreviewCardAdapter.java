package com.n0xx1.livedetect.barcode;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.n0xx1.livedetect.R;

import java.util.List;

public class PreviewCardAdapter  extends RecyclerView.Adapter<PreviewCardAdapter.CardViewHolder> {

    /** Listens to user's interaction with the preview card item. */
    public interface CardItemListener {
        void onPreviewCardClicked(BarcodedProducts barcodedProducts);
    }

    private final List<BarcodedProducts> barcodedProductsList;
    private final CardItemListener cardItemListener;

    public PreviewCardAdapter(
            List<BarcodedProducts> barcodedProductsList, CardItemListener cardItemListener) {
        this.barcodedProductsList = barcodedProductsList;
        this.cardItemListener = cardItemListener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CardViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.products_preview_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        BarcodedProducts barcodedProducts = barcodedProductsList.get(position);
        holder.bindProducts(barcodedProducts.getEntityList());
        holder.itemView.setOnClickListener(v -> cardItemListener.onPreviewCardClicked(barcodedProducts));
    }

    @Override
    public int getItemCount() {
        return barcodedProductsList.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final TextView titleView;
        private final TextView subtitleView;
        private final int imageSize;

        private CardViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.card_image);
            titleView = itemView.findViewById(R.id.card_title);
            subtitleView = itemView.findViewById(R.id.card_subtitle);
            imageSize = itemView.getResources().getDimensionPixelOffset(R.dimen.preview_card_image_size);
        }

        private void bindProducts(List<Entity> products) {
            if (products.isEmpty()) {
                imageView.setVisibility(View.GONE);
                titleView.setText(R.string.static_image_card_no_result_title);
                subtitleView.setText(R.string.static_image_card_no_result_subtitle);
            } else {
                Entity topEntity = products.get(0);
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageDrawable(null);
                if (!TextUtils.isEmpty(topEntity.imageUrl)) {
                    new ImageDownloadTask(imageView, imageSize).execute(topEntity.imageUrl);
                } else {
                    imageView.setImageResource(R.drawable.logo_google_cloud);
                }
                titleView.setText(topEntity.title);
                subtitleView.setText(
                        itemView
                                .getResources()
                                .getString(R.string.static_image_preview_card_subtitle, products.size() - 1));
            }
        }
    }
}
