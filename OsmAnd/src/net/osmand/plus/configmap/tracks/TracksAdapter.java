package net.osmand.plus.configmap.tracks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

class TracksAdapter extends RecyclerView.Adapter<ViewHolder> {

	public static final int TYPE_TRACK = 0;
	public static final int TYPE_NO_TRACKS = 1;
	public static final int TYPE_NO_VISIBLE_TRACKS = 2;
	public static final int TYPE_RECENTLY_VISIBLE_TRACKS = 3;

	private final OsmandApplication app;
	private final TrackTab trackTab;
	private final List<Object> items = new ArrayList<>();
	private final TracksVisibilityListener listener;

	private final boolean nightMode;

	public TracksAdapter(@NonNull OsmandApplication app, @NonNull TrackTab trackTab,
	                     @NonNull TracksVisibilityListener listener, boolean nightMode) {
		this.app = app;
		this.trackTab = trackTab;
		this.listener = listener;
		this.nightMode = nightMode;
		this.items.addAll(trackTab.items);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		switch (viewType) {
			case TYPE_TRACK:
				View view = inflater.inflate(R.layout.track_list_item, parent, false);
				return new TrackViewHolder(view, nightMode);
			case TYPE_NO_TRACKS:
				view = inflater.inflate(R.layout.tracks_empty_state, parent, false);
				return new EmptyTracksViewHolder(view, listener);
			case TYPE_NO_VISIBLE_TRACKS:
				view = inflater.inflate(R.layout.tracks_empty_state, parent, false);
				return new NoVisibleTracksViewHolder(view, listener);
			case TYPE_RECENTLY_VISIBLE_TRACKS:
				view = inflater.inflate(R.layout.list_header_switch_item, parent, false);
				return new RecentlyVisibleViewHolder(view, listener);
			default:
				throw new IllegalArgumentException("Unsupported view type " + viewType);
		}

	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof GPXInfo) {
			return TYPE_TRACK;
		} else if (object instanceof Integer) {
			int item = (Integer) object;
			if (TYPE_NO_TRACKS == item) {
				return TYPE_NO_TRACKS;
			} else if (TYPE_NO_VISIBLE_TRACKS == item) {
				return TYPE_NO_VISIBLE_TRACKS;
			} else if (TYPE_RECENTLY_VISIBLE_TRACKS == item) {
				return TYPE_RECENTLY_VISIBLE_TRACKS;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof TrackViewHolder) {
			GPXInfo gpxInfo = (GPXInfo) items.get(position);
			TrackViewHolder viewHolder = (TrackViewHolder) holder;
			boolean lastItem = position == getItemCount() - 1;
			String folderName = trackTab.type.shouldShowFolder() ? Algorithms.getFileWithoutDirs(trackTab.name) : null;
			viewHolder.bindView(listener, gpxInfo, folderName, lastItem);
		} else if (holder instanceof NoVisibleTracksViewHolder) {
			NoVisibleTracksViewHolder viewHolder = (NoVisibleTracksViewHolder) holder;
			viewHolder.bindView(app, nightMode);
		} else if (holder instanceof EmptyTracksViewHolder) {
			EmptyTracksViewHolder viewHolder = (EmptyTracksViewHolder) holder;
			viewHolder.bindView(app, nightMode);
		} else if (holder instanceof RecentlyVisibleViewHolder) {
			RecentlyVisibleViewHolder viewHolder = (RecentlyVisibleViewHolder) holder;
			viewHolder.bindView(nightMode);
		}
	}

	@Override
	public int getItemCount() {
		return trackTab.items.size();
	}

	public void onTrackItemSelected(@NonNull GPXInfo gpxInfo) {
		int index = items.indexOf(gpxInfo);
		if (index != -1) {
			notifyItemChanged(index);
		}
	}

	private static class RecentlyVisibleViewHolder extends ViewHolder {

		private final OsmandApplication app;
		private final TextView title;
		private final CheckBox checkbox;
		private final TracksVisibilityListener listener;

		public RecentlyVisibleViewHolder(@NonNull View view, @NonNull TracksVisibilityListener listener) {
			super(view);
			this.listener = listener;
			this.app = (OsmandApplication) view.getContext().getApplicationContext();

			title = view.findViewById(R.id.title);
			checkbox = view.findViewById(R.id.checkbox);
		}

		public void bindView(boolean nightMode) {
			title.setText(R.string.recently_visible);

			boolean selected = listener.isRecentlyTracksSelected();
			checkbox.setChecked(selected);
			int color = ColorUtilities.getSelectedProfileColor(app, nightMode);
			UiUtilities.setupCompoundButton(selected, color, checkbox);

			itemView.setOnClickListener(v -> listener.selectRecentlyVisibleTracks(!selected));
		}
	}

	public interface TracksVisibilityListener {

		boolean isTrackSelected(@NonNull GPXInfo gpxInfo);

		boolean isRecentlyTracksSelected();

		void onTrackItemSelected(@NonNull GPXInfo gpxInfo, boolean selected);

		void importTracks();

		void showAllTracks();

		void selectRecentlyVisibleTracks(boolean selected);
	}
}