package net.osmand.plus.configmap.tracks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.myplaces.tracks.filters.SmartFolderHelper;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.SmartFolder;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TrackFolderAnalysis;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.track.helpers.GpxUiHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackTab implements TracksGroup {
	public static final String SMART_FOLDER_TAB_NAME_PREFIX = "SMART_FOLDER___";

	public final TrackTabType type;
	public final List<Object> items = new ArrayList<>();

	@Nullable
	public final File directory;

	@Nullable
	private final String name;
	private String typeName = null;

	private TracksSortMode sortMode = TracksSortMode.getDefaultSortMode();
	private TrackFolderAnalysis folderAnalysis;

	public TrackTab(@NonNull File directory) {
		this.directory = directory;
		this.name = null;
		this.type = TrackTabType.FOLDER;
	}

	public TrackTab(@NonNull TrackTabType type) {
		this.directory = null;
		this.name = null;
		this.type = type;
	}

	public TrackTab(@NonNull SmartFolder smartFolder) {
		this.directory = null;
		this.name = smartFolder.getFolderName();
		this.type = TrackTabType.SMART_FOLDER;
		typeName = SMART_FOLDER_TAB_NAME_PREFIX + name;
	}

	@NonNull
	public TracksSortMode getSortMode() {
		return sortMode;
	}

	public void setSortMode(@NonNull TracksSortMode sortMode) {
		this.sortMode = sortMode;
	}

	@NonNull
	public String getName(@NonNull Context context) {
		return getName(context, false);
	}

	@NonNull
	public String getName(@NonNull Context context, boolean includeParentDir) {
		if (type.titleId != -1) {
			return context.getString(type.titleId);
		}
		if (directory != null) {
			return GpxUiHelper.getFolderName(context, directory, includeParentDir);
		}
		if (name != null) {
			return name;
		}
		return "";
	}

	@NonNull
	public String getTypeName() {
		switch (type) {
			case FOLDER:
				return directory != null ? directory.getName() : "";
			case SMART_FOLDER:
				return typeName != null ? typeName : "";
			default:
				return type.name();
		}
	}

	@NonNull
	public List<TrackItem> getTrackItems() {
		List<TrackItem> trackItems = new ArrayList<>();
		for (Object object : items) {
			if (object instanceof TrackItem) {
				trackItems.add((TrackItem) object);
			}
		}
		return trackItems;
	}

	@NonNull
	public List<TrackFolder> getTrackFolders() {
		List<TrackFolder> trackFolders = new ArrayList<>();
		for (Object object : items) {
			if (object instanceof TrackFolder) {
				trackFolders.add((TrackFolder) object);
			}
		}
		return trackFolders;
	}

	@NonNull
	@Override
	public String toString() {
		return "TrackTab{name=" + getTypeName() + "}";
	}

	@NonNull
	public TrackFolderAnalysis getFolderAnalysis() {
		if (folderAnalysis == null) {
			folderAnalysis = new TrackFolderAnalysis(this);
		}
		return folderAnalysis;
	}

	public long lastModified(@NonNull Context context, @NonNull SmartFolderHelper smartFolderHelper) {
		if (directory != null) {
			return directory.lastModified();
		} else if (type == TrackTabType.SMART_FOLDER) {
			SmartFolder smartFolder = smartFolderHelper.getSmartFolder(getName(context));
			if (smartFolder != null) {
				return smartFolder.lastModified();
			}
		}
		return 0;
	}
}
