package com.marverenic.music.viewmodel;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;

import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.instances.Song;
import com.marverenic.music.player.PlayerController;

import java.util.List;

import timber.log.Timber;

public class QueueSongViewModel extends SongViewModel {

    private static final String TAG_PLAYLIST_DIALOG = "QueueSongViewModel.PlaylistDialog";

    private Context mContext;
    private FragmentManager mFragmentManager;
    private OnRemoveListener mRemoveListener;

    public QueueSongViewModel(Context context, FragmentManager fragmentManager, List<Song> songs,
                              OnRemoveListener removeListener) {
        super(context, fragmentManager, songs);
        mContext = context;
        mFragmentManager = fragmentManager;
        mRemoveListener = removeListener;
    }

    public interface OnRemoveListener {
        void onRemove();
    }

    @Override
    public View.OnClickListener onClickSong() {
        return v -> PlayerController.changeSong(getIndex());
    }

    public int getNowPlayingIndicatorVisibility() {
        if (PlayerController.getQueuePosition() == getIndex()) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
    }

    @Override
    public View.OnClickListener onClickMenu() {
        return v -> {
            PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
            String[] options = mContext.getResources().getStringArray(R.array.edit_queue_options);

            for (int i = 0; i < options.length;  i++) {
                menu.getMenu().add(Menu.NONE, i, i, options[i]);
            }

            menu.setOnMenuItemClickListener(onMenuItemClick(v));
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick(View view) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case 0: //Go to artist
                    mMusicStore.findArtistById(getReference().getArtistId()).subscribe(
                            artist -> {
                                mContext.startActivity(ArtistActivity.newIntent(mContext, artist));
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find artist");
                            });

                    return true;
                case 1: // Go to album
                    mMusicStore.findAlbumById(getReference().getAlbumId()).subscribe(
                            album -> {
                                mContext.startActivity(AlbumActivity.newIntent(mContext, album));
                            }, throwable -> {
                                Timber.e(throwable, "Failed to find album");
                            });

                    return true;
                case 2: // Add to playlist
                    new AppendPlaylistDialogFragment.Builder(mContext, mFragmentManager)
                            .setTitle(mContext.getResources().getString(
                                    R.string.header_add_song_name_to_playlist, getReference()))
                            .setSongs(getSongs())
                            .showSnackbarIn(R.id.imageArtwork)
                            .show(TAG_PLAYLIST_DIALOG);
                    return true;
                case 3: // Remove
                    int queuePosition = PlayerController.getQueuePosition();
                    int itemPosition = getIndex();

                    getSongs().remove(itemPosition);

                    PlayerController.editQueue(getSongs(),
                            (queuePosition > itemPosition)
                                    ? queuePosition - 1
                                    : queuePosition);

                    if (queuePosition == itemPosition) {
                        PlayerController.begin();
                    }

                    mRemoveListener.onRemove();
                    return true;
            }
            return false;
        };
    }
}
