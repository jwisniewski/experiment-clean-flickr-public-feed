package com.jw.flickrfeed.app.screens.feed;

import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.jw.flickrfeed.R;
import com.jw.flickrfeed.domain.Photo;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

import static android.content.res.ColorStateList.valueOf;

/**
 * Adapts a list of {@link Photo} as the {@link RecyclerView}s data source.
 *
 * @author Jaroslaw Wisniewski, j.wisniewski@appsisle.com
 */
public class PhotoFeedAdapter extends RecyclerView.Adapter<PhotoFeedAdapter.ViewHolder> {

    public interface PhotoIntegrationListener {

        void onPhotoSelected(@NonNull Photo photo);

        void onPhotoDetailsRequested(@NonNull Photo photo);
    }

    @SuppressWarnings("Convert2MethodRef")
    static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.itemContent)
        ViewGroup itemContent;

        @BindView(R.id.photoDetailsLayout)
        ViewGroup photoDetailsLayout;

        @BindView(R.id.photoImageView)
        ImageView photoImageView;

        @BindView(R.id.authorTextView)
        TextView authorTextView;

        @BindView(R.id.tagsIndicatorImageView)
        ImageView tagsIndicatorImageView;

        @Nullable
        Photo boundPhoto;

        @NonNull
        static ViewHolder create(@NonNull ViewGroup parent) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                                                .inflate(R.layout.photo_feed_item, parent, false));
        }

        ViewHolder(@NonNull View view) {
            super(view);

            ButterKnife.bind(this, view);
        }

        void bind(@NonNull Photo newPhoto, @Nullable PhotoIntegrationListener listener) {
            boundPhoto = newPhoto;

            bindInitialState();
            loadPhoto(boundPhoto, (loadedPhoto, swatch) -> {
                if (loadedPhoto == boundPhoto) {
                    bindPhotoLoadedState(loadedPhoto, swatch);
                }
            });

            itemContent.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onPhotoSelected(boundPhoto);
                }
            });

            itemContent.setOnLongClickListener(view -> {
                if (listener != null) {
                    listener.onPhotoDetailsRequested(boundPhoto);
                    return true;
                } else {
                    return false;
                }
            });
        }

        private void bindInitialState() {
            photoDetailsLayout.setVisibility(View.GONE);
            authorTextView.setVisibility(View.GONE);
            tagsIndicatorImageView.setVisibility(View.GONE);
        }

        private void loadPhoto(@NonNull Photo photo, LoadedPhotoConsumer consumer) {
            Picasso.with(photoImageView.getContext())
                   .load(photo.thumbnailUrl())
                   .into(photoImageView, new Callback() {
                       @Override
                       public void onSuccess() {
                           Palette.from(((BitmapDrawable) photoImageView.getDrawable()).getBitmap())
                                  .generate(palette -> {
                                      final Palette.Swatch swatch = palette.getMutedSwatch();
                                      if (swatch != null) {
                                          consumer.consume(photo, swatch);
                                      }
                                  });
                       }

                       @Override
                       public void onError() {
                           // in production ready app we should definitely handle this
                       }
                   });
        }

        private void bindPhotoLoadedState(@NonNull Photo photo, @NonNull Palette.Swatch swatch) {
            photoDetailsLayout.setVisibility(View.VISIBLE);
            photoDetailsLayout.setBackgroundTintList(valueOf(swatch.getRgb()));

            authorTextView.setVisibility(View.VISIBLE);
            authorTextView.setTextColor(swatch.getBodyTextColor());
            authorTextView.setText(photo.author());

            if (!photo.tags().isEmpty()) {
                tagsIndicatorImageView.setVisibility(View.VISIBLE);
                tagsIndicatorImageView.setImageTintList(
                        valueOf(swatch.getBodyTextColor()));
            }
        }
    }

    @NonNull
    private final List<Photo> photos = new ArrayList<>();

    @Nullable
    private PhotoIntegrationListener photoIntegrationListener;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return ViewHolder.create(viewGroup);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        viewHolder.bind(photos.get(i), photoIntegrationListener);
    }

    public void updateItems(@NonNull List<Photo> newPhotos) {
        List<Photo> oldPhotos = new ArrayList<>(photos);
        photos.clear();
        photos.addAll(newPhotos);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PhotoFeedDiffCallback(oldPhotos, newPhotos));
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public void setPhotoInteractionListener(@Nullable PhotoIntegrationListener photoIntegrationListener) {
        this.photoIntegrationListener = photoIntegrationListener;
        notifyDataSetChanged();
    }

    interface LoadedPhotoConsumer {

        void consume(@NonNull Photo loadedPhoto, @NonNull Palette.Swatch swatch);
    }
}
