package com.n0xx1.livedetect.barcode;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.barcode.BarcodedProductAdapter.BarcodedProductViewHolder;
import com.n0xx1.livedetect.text2speech.Text2Speech;

import java.util.List;

public class BarcodedProductAdapter extends RecyclerView.Adapter<BarcodedProductViewHolder> {

        private Text2Speech tts;

        static class BarcodedProductViewHolder extends RecyclerView.ViewHolder {

            static BarcodedProductViewHolder create(ViewGroup parent) {
                View view =
                        LayoutInflater.from(parent.getContext()).inflate(R.layout.product_item, parent, false);
                return new BarcodedProductViewHolder(view);
            }

            private final ImageView imageView;
            private final TextView titleView;
            private final TextView subtitleView;
            private final int imageSize;

            private BarcodedProductViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.product_image);
                titleView = view.findViewById(R.id.product_title);
                subtitleView = view.findViewById(R.id.product_subtitle);
                imageSize = view.getResources().getDimensionPixelOffset(R.dimen.product_item_image_size);
            }

            void bindBarcodeEntity(BarcodedProduct barcodedProduct) {
                imageView.setImageDrawable(null);
                if (!TextUtils.isEmpty(barcodedProduct.imageUrl)) {
                    new ImageDownloadTask(imageView, imageSize).execute(barcodedProduct.imageUrl);
                } else {
                    imageView.setImageResource(R.drawable.logo_google_cloud);
                }
                titleView.setText(barcodedProduct.name);
                subtitleView.setText(barcodedProduct.price+"원");
            }
        }

        private final List<BarcodedProduct> barcodedProductList;

        public BarcodedProductAdapter(List<BarcodedProduct> barcodedProductList) {
            this.barcodedProductList = barcodedProductList;
        }

        @Override
        @NonNull
        public BarcodedProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view =
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.product_item, parent, false);
            BarcodedProductViewHolder holder= new BarcodedProductAdapter.BarcodedProductViewHolder(view);


            tts = new Text2Speech(parent.getContext(), (Activity) parent.getContext());

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tts.speech("상품명은 "+barcodedProductList.get(holder.getAdapterPosition()).name + "입니다."+"해당 가격은 "+barcodedProductList.get(holder.getAdapterPosition()).price + "원 입니다.");
                }
            });

//        return BarcodedProductViewHolder.create(parent);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull BarcodedProductViewHolder holder, int position) {
            holder.bindBarcodeEntity(barcodedProductList.get(position));
        }

        @Override
        public int getItemCount() {
            return barcodedProductList.size();
        }
    }


