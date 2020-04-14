/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.n0xx1.livedetect.staticdetection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.n0xx1.livedetect.R;

import java.util.List;

/** Powers the bottom card carousel for displaying the preview of product search result. */
public class PreviewCardAdapter extends RecyclerView.Adapter<PreviewCardAdapter.CardViewHolder> {

  /** Listens to user's interaction with the preview card item. */
  public interface CardItemListener {
    void onPreviewCardClicked(StaticDetectResponse staticResponse);
  }

  private final List<StaticDetectResponse> staticDetectResponses;
  private final CardItemListener cardItemListener;

  public PreviewCardAdapter(
      List<StaticDetectResponse> staticDetectResponses, CardItemListener cardItemListener) {
    this.staticDetectResponses = staticDetectResponses;
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
    StaticDetectResponse staticResponse = staticDetectResponses.get(position);
    holder.bindLabels(staticResponse.getLabeledObject().getLabelList(), staticResponse.getRequest().getRequestIndex());
    holder.itemView.setOnClickListener(v -> cardItemListener.onPreviewCardClicked(staticResponse));
  }

  @Override
  public int getItemCount() {
    return staticDetectResponses.size();
  }

  class CardViewHolder extends RecyclerView.ViewHolder {

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

    private void bindLabels(List<Label> labels, int requestId) {
      if (labels.isEmpty()) {
        imageView.setVisibility(View.GONE);
        titleView.setText(R.string.static_image_card_no_result_title);
        subtitleView.setText(R.string.static_image_card_no_result_subtitle);
      } else {
        Label topLabel = labels.get(0);
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageDrawable(null);
//        if (!TextUtils.isEmpty(topProduct.imageUrl)) {
//          new ImageDownloadTask(imageView, imageSize).execute(topProduct.imageUrl);
//        } else {
//          imageView.setImageResource(R.drawable.logo_google_cloud);
//        }

        imageView.setImageBitmap(staticDetectResponses.get(requestId).getCroppedBitmap());

        titleView.setText(topLabel.label);
        subtitleView.setText(
            itemView
                .getResources()
                .getString(R.string.static_image_preview_card_subtitle, labels.size() - 1));
      }
    }
  }
}
